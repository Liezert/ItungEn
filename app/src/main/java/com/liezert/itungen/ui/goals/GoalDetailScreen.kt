package com.liezert.itungen.ui.goals

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.Image
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.liezert.itungen.R
import com.liezert.itungen.domain.model.AmountKind
import com.liezert.itungen.domain.model.Goal
import com.liezert.itungen.domain.model.GoalDeposit
import com.liezert.itungen.domain.model.formatRupiah
import com.liezert.itungen.ui.components.AmountText
import com.liezert.itungen.ui.components.Ico
import com.liezert.itungen.ui.components.ItungIconButton
import com.liezert.itungen.ui.components.TopBar
import com.liezert.itungen.ui.components.bottomClearance
import com.liezert.itungen.ui.components.screenPad
import com.liezert.itungen.ui.design.ItungButton
import com.liezert.itungen.ui.design.ItungCard
import com.liezert.itungen.ui.design.ItungDivider
import com.liezert.itungen.ui.design.ItungImageCard
import com.liezert.itungen.ui.design.ItungSheet
import com.liezert.itungen.ui.theme.Bold
import com.liezert.itungen.ui.theme.Ink
import com.liezert.itungen.ui.theme.Medium
import com.liezert.itungen.ui.theme.Regular
import com.liezert.itungen.ui.theme.SemiBold
import com.liezert.itungen.ui.theme.appText
import com.liezert.itungen.ui.theme.formatId

/** Detail goal (dibuka dari kartu di tab Goals): Tambah Dana, ganti gambar, riwayat uang masuk, dan Hapus target. */
@Composable
fun GoalDetailScreen(
    goalId: Long,
    onBack: () -> Unit,
    viewModel: GoalsViewModel = viewModel(factory = GoalsViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val goal = state.goals.firstOrNull { it.id == goalId }
    val deposits by remember(goalId) { viewModel.deposits(goalId) }.collectAsStateWithLifecycle(emptyList())
    // Goal sudah tidak ada (baru dihapus di sini, atau di HP lain lewat sinkron): kembali ke daftar, sekali saja.
    var left by remember { mutableStateOf(false) }
    LaunchedEffect(state.loading, goal == null) {
        if (!state.loading && goal == null && !left) {
            left = true
            onBack()
        }
    }
    if (goal == null) return

    var fundsOpen by remember { mutableStateOf(false) }
    var opened by remember { mutableIntStateOf(0) }
    // Aksi berhasil (Tambah Dana) → tutup sheet.
    LaunchedEffect(state.form.doneCount) { fundsOpen = false }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }

    GoalDetailContent(
        goal = goal,
        deposits = deposits,
        onBack = onBack,
        onAddFunds = {
            viewModel.clearErrors()
            fundsOpen = true
            opened++
        },
        onDelete = { confirmDelete = true },
        onPickImage = { viewModel.setImage(goal, it) },
        onRemoveImage = { viewModel.removeImage(goal) },
    )
    ItungSheet(fundsOpen, onDismiss = { fundsOpen = false }) {
        key(opened) {
            AddFundsSheet(goal, state, onAddFunds = { viewModel.addFunds(goal.id, it) }, onEdited = viewModel::clearErrors)
        }
    }
    DeleteGoalDialog(
        goal,
        visible = confirmDelete,
        onConfirm = {
            confirmDelete = false
            viewModel.deleteGoal(goal.id)
        },
        onDismiss = { confirmDelete = false },
    )
}

/** Isi detail goal tanpa ViewModel (dipakai layar dan tes). Aksi null = tombolnya tidak ditampilkan. */
@Composable
fun GoalDetailContent(
    goal: Goal,
    deposits: List<GoalDeposit>,
    onBack: () -> Unit,
    onAddFunds: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
    onPickImage: (Uri) -> Unit = {},
    onRemoveImage: () -> Unit = {},
) {
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri -> uri?.let(onPickImage) }
    val pick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        TopBar(
            leading = { ItungIconButton(R.drawable.ic_back, onBack, "Kembali") },
            trailing = { Spacer(Modifier.size(48.dp)) },
        )
        Column(Modifier.screenPad(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(goal.name, Modifier.semantics { heading() }, style = appText(24f, 30f, Bold, tracking = -0.5f))
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                ItungImageCard(
                    Modifier.fillMaxWidth(),
                    imageHeight = maxWidth * 9f / 16f,
                    image = {
                        GoalImage(goal.imagePath, "Gambar ${goal.name}", Modifier.fillMaxSize()) { Ico(R.drawable.ic_goal_shoe_large) }
                    },
                ) {
                    GoalStatusChip(goal)
                    GoalProgressBar(goal, 10.dp)
                    Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(12.dp)) {
                        Stat("Terkumpul", goal.saved, Modifier.weight(1f))
                        Stat("Target", goal.target, Modifier.weight(1f))
                    }
                    detailLine(goal)?.let { Text(it, style = appText(13f, 18f, Regular, Ink.Muted)) }
                    Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(8.dp)) {
                        SmallButton(if (goal.imagePath == null) "Tambah gambar" else "Ganti gambar", Ink.AccentLight, pick)
                        if (goal.imagePath != null) SmallButton("Hapus gambar", Ink.Red, onRemoveImage)
                    }
                    if (onAddFunds != null) {
                        ItungButton(onClick = onAddFunds, modifier = Modifier.fillMaxWidth().height(52.dp), tint = Ink.Accent) {
                            // Putih eksplisit: Ico mewarnai ulang ikon untuk latar terang, padahal tombol ini selalu berwarna aksen.
                            Image(painterResource(R.drawable.ic_plus_fund), null, Modifier.size(20.dp), colorFilter = ColorFilter.tint(Color.White))
                            Text("Tambah Dana", style = appText(16f, 24f, Bold, Color.White))
                        }
                    }
                }
            }

            Text(
                "RIWAYAT UANG MASUK",
                Modifier.padding(top = 8.dp).semantics { heading() },
                style = appText(12f, 18f, Bold, Ink.Subtle, tracking = 1.2f),
            )
            DepositHistory(deposits)

            if (onDelete != null) {
                ItungButton(onClick = onDelete, modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 8.dp)) {
                    Text("Hapus target", style = appText(14f, 20f, SemiBold, Ink.Red))
                }
            }
        }
        Spacer(Modifier.height(bottomClearance()))
    }
}

@Composable
private fun Stat(label: String, amount: Long, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label, style = appText(12f, 16f, Medium, Ink.Subtle), maxLines = 1, overflow = TextOverflow.Ellipsis)
        AmountText(amount, AmountKind.NEUTRAL, appText(18f, 24f, Bold))
    }
}

@Composable
private fun SmallButton(label: String, color: Color, onClick: () -> Unit) {
    ItungButton(onClick = onClick, modifier = Modifier.height(40.dp), contentPadding = 14.dp) {
        Text(label, style = appText(13f, 18f, SemiBold, color), maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** Uang masuk ke target (setiap Tambah Dana), terbaru dulu; keadaan kosong menjelaskan isinya nanti. */
@Composable
private fun DepositHistory(deposits: List<GoalDeposit>) {
    ItungCard(Modifier.fillMaxWidth(), padding = 17.dp, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (deposits.isEmpty()) {
            Text(
                "Belum ada uang masuk. Setiap Tambah Dana tercatat di sini.",
                style = appText(14f, 20f, Regular, Ink.Muted),
            )
        }
        deposits.forEachIndexed { index, deposit ->
            if (index > 0) ItungDivider(color = Ink.white(0.08f))
            Row(
                Modifier.fillMaxWidth().semantics(mergeDescendants = true) {},
                Arrangement.spacedBy(12.dp),
                Alignment.CenterVertically,
            ) {
                Text(
                    deposit.date.formatId(),
                    Modifier.weight(1f),
                    style = appText(14f, 20f, Regular, Ink.Muted),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                AmountText(deposit.amount, AmountKind.NEUTRAL, appText(15f, 20f, SemiBold))
            }
        }
    }
}

/** "Sisa Rp… · rencana Rp…/bulan · target 31 Des 2026"; null bila tidak ada yang perlu disebut. */
private fun detailLine(goal: Goal): String? = listOfNotNull(
    goal.remaining.takeIf { it > 0 }?.let { "Sisa ${formatRupiah(it)}" },
    goal.plannedMonthly.takeIf { it > 0 && !goal.achieved }?.let { "rencana ${formatRupiah(it)}/bulan" },
    goal.targetDate?.let { "target ${it.format(TargetDate)}" },
).joinToString(" · ").ifEmpty { null }?.replaceFirstChar { it.uppercase() }

private val TargetDate = java.time.format.DateTimeFormatter.ofPattern("d MMM yyyy", java.util.Locale.forLanguageTag("id-ID"))
