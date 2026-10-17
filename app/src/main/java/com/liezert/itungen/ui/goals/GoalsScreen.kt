package com.liezert.itungen.ui.goals

import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.Image
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.liezert.itungen.R
import com.liezert.itungen.domain.model.Goal
import com.liezert.itungen.domain.model.formatRupiah
import com.liezert.itungen.ui.components.Ico
import com.liezert.itungen.ui.components.TopBar
import com.liezert.itungen.ui.components.bottomClearance
import com.liezert.itungen.ui.components.screenPad
import com.liezert.itungen.ui.design.ItungButton
import com.liezert.itungen.ui.design.ItungCard
import com.liezert.itungen.ui.design.ItungSheet
import com.liezert.itungen.ui.design.itungImageFrame
import com.liezert.itungen.ui.theme.Bold
import com.liezert.itungen.ui.theme.Ink
import com.liezert.itungen.ui.theme.ItungEnTheme
import com.liezert.itungen.ui.theme.Medium
import com.liezert.itungen.ui.theme.Regular
import com.liezert.itungen.ui.theme.SemiBold
import com.liezert.itungen.ui.theme.appText

private val Dash = Color(0xFF414754)

/** Tab Goals: daftar kartu ringkas; ketuk kartu → detail goal ([onOpenGoal]). "Tambah Target" membuka Buat Target. */
@Composable
fun GoalsScreen(
    onOpenGoal: (Long) -> Unit,
    viewModel: GoalsViewModel = viewModel(factory = GoalsViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    // Saveable: sheet (dan isiannya) tetap terbuka setelah rotasi atau proses dibuat ulang.
    var sheetOpen by rememberSaveable { mutableStateOf(false) }
    // [opened] membuat form baru (kosong) tiap dibuka.
    var opened by rememberSaveable { mutableIntStateOf(0) }
    // Target berhasil dibuat → tutup sheet.
    // Hanya saat jumlahnya naik: efek ini juga berjalan ulang setelah rotasi / proses dibuat ulang, dan dulu ikut
    // menutup sheet yang sedang diisi.
    var seenDone by rememberSaveable { mutableIntStateOf(state.form.doneCount) }
    LaunchedEffect(state.form.doneCount) {
        if (state.form.doneCount > seenDone) sheetOpen = false
        seenDone = state.form.doneCount
    }

    GoalsContent(
        goals = state.goals,
        loading = state.loading,
        onAddGoal = {
            viewModel.clearErrors()
            sheetOpen = true
            opened++
        },
        onOpenGoal = { onOpenGoal(it.id) },
    )

    // Tutup sheet Buat Target tanpa menyimpan: gambar drafnya dihapus (setelah dibuat, drafnya sudah kosong).
    LaunchedEffect(sheetOpen) { if (!sheetOpen) viewModel.discardDraftImage() }

    ItungSheet(sheetOpen, onDismiss = { sheetOpen = false }) {
        key(opened) {
            CreateGoalSheet(
                state,
                onCreate = viewModel::createGoal,
                onEdited = viewModel::clearErrors,
                onPickImage = viewModel::pickDraftImage,
                onRemoveImage = { viewModel.discardDraftImage() },
            )
        }
    }
}

/** Isi tab Goals tanpa ViewModel (dipakai layar, pratinjau, dan tes). Aksi null = tidak bisa diketuk. */
@Composable
fun GoalsContent(
    modifier: Modifier = Modifier,
    goals: List<Goal> = emptyList(),
    loading: Boolean = false,
    onAddGoal: (() -> Unit)? = null,
    onOpenGoal: ((Goal) -> Unit)? = null,
) {
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        TopBar(
            leading = { Spacer(Modifier.size(48.dp)) }, // hamburger ditunda sampai punya isi (DESIGN_SPEC §11.2)
            trailing = { Spacer(Modifier.size(48.dp)) },
        )
        Column(Modifier.screenPad(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            // Judul berbobot: FlowRow memutuskan pindah baris dari kata terpanjangnya, jadi biasanya judul dan tombol
            // sebaris (judul membungkus di spasi); bila satu kata pun tidak muat (huruf besar), tombol turun ke baris
            // berikutnya. Dulu judulnya yang dipecah di tengah kata ("Tabung / an").
            FlowRow(
                Modifier.fillMaxWidth().padding(bottom = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                itemVerticalAlignment = Alignment.CenterVertically,
            ) {
                Text(stringResource(R.string.savings_goals), Modifier.weight(1f).padding(end = 8.dp), style = appText(20f, 25f, Bold))
                if (onAddGoal != null) {
                    ItungButton(
                        onClick = onAddGoal,
                        modifier = Modifier.heightIn(min = 36.dp),
                        surfaceColor = Ink.Accent.copy(alpha = 0.2f),
                        contentPadding = 13.dp,
                    ) {
                        Image(painterResource(R.drawable.ic_plus_small), null, Modifier.size(16.dp), colorFilter = ColorFilter.tint(Ink.AccentLight))
                        Text(stringResource(R.string.add_goal), style = appText(14f, 20f, SemiBold, Ink.AccentLight))
                    }
                }
            }
            if (loading) Unit else if (goals.isEmpty()) EmptyGoals() else goals.forEach { goal ->
                GoalCard(goal, onOpenGoal?.let { open -> { open(goal) } })
            }
        }
        Spacer(Modifier.height(bottomClearance()))
    }
}

/** Kartu ringkas: thumbnail 4:3 berbingkai tema, nama, status, bar, dan nominal. Seluruh kartu diketuk → detail. */
@Composable
private fun GoalCard(goal: Goal, onClick: (() -> Unit)?) {
    ItungCard(Modifier.fillMaxWidth(), padding = 14.dp, tint = goalTint(goal), onClick = onClick) {
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
            GoalImage(goal.imagePath, null, Modifier.width(104.dp).aspectRatio(4f / 3f).itungImageFrame()) {
                Ico(R.drawable.ic_goal_shoe)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(goal.name, style = appText(16f, 22f, Bold), maxLines = 2, overflow = TextOverflow.Ellipsis)
                GoalStatusChip(goal)
                GoalProgressBar(goal, 8.dp)
                Text(
                    stringResource(R.string.amount_of_amount, formatRupiah(goal.saved), formatRupiah(goal.target)),
                    style = appText(12f, 16f, Medium, Ink.Muted),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/** Keadaan kosong tanpa tombol sendiri: "Tambah Target" di judul sudah cukup (tidak ada dua tombol yang sama). */
@Composable
private fun EmptyGoals() {
    ItungCard(
        Modifier.fillMaxWidth(),
        padding = 33.dp,
        dashedBorder = Dash.copy(alpha = 0.4f),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.padding(bottom = 8.dp)) {
            Box(Modifier.size(64.dp).clip(CircleShape).background(Ink.EmptyIconBg), contentAlignment = Alignment.Center) {
                Ico(R.drawable.ic_flag)
            }
        }
        Text(
            stringResource(R.string.goals_empty),
            Modifier.padding(horizontal = 37.dp),
            style = appText(14f, 20f, Regular, Ink.Muted),
            textAlign = TextAlign.Center,
        )
    }
}

// Data contoh hanya untuk pratinjau di Android Studio.
private val PreviewGoals = listOf(
    Goal("Sepatu Lari", saved = 452_000, target = 800_000, plannedMonthly = 174_000, targetDate = java.time.LocalDate.of(2026, 12, 4)),
    Goal("Liburan Bali", saved = 2_100_000, target = 2_000_000),
)

@Preview(widthDp = 390, heightDp = 800, showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun GoalsPreview() = ItungEnTheme {
    GoalsContent(goals = PreviewGoals, onAddGoal = {}, onOpenGoal = {})
}

@Preview(widthDp = 390, heightDp = 600, showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun GoalsEmptyPreview() = ItungEnTheme { GoalsContent() }
