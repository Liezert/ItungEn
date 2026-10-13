package com.liezert.itungen.ui.goals

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.liezert.glass.components.GlassCard
import com.liezert.glass.components.GlassSurface
import com.liezert.glass.components.LiquidButton
import com.liezert.glass.components.LiquidSheet
import com.liezert.itungen.R
import com.liezert.itungen.domain.model.Goal
import com.liezert.itungen.domain.model.formatRupiah
import com.liezert.itungen.ui.components.GlassIconButton
import com.liezert.itungen.ui.components.Ico
import com.liezert.itungen.ui.components.IconCircle
import com.liezert.itungen.ui.components.ProgressBar
import com.liezert.itungen.ui.components.TopBar
import com.liezert.itungen.ui.components.bottomClearance
import com.liezert.itungen.ui.components.screenPad
import com.liezert.itungen.ui.theme.Bold
import com.liezert.itungen.ui.theme.Ink
import com.liezert.itungen.ui.theme.ItungEnTheme
import com.liezert.itungen.ui.theme.Medium
import com.liezert.itungen.ui.theme.Regular
import com.liezert.itungen.ui.theme.SemiBold
import com.liezert.itungen.ui.theme.appText

private val GoalBrush = Brush.horizontalGradient(listOf(Ink.Accent, Ink.AccentLight))
private val Dash = Color(0xFF414754)

private sealed interface Sheet {
    data object Create : Sheet
    data class Funds(val goalId: Long) : Sheet
}

@Composable
fun GoalsScreen(
    onHistory: () -> Unit,
    viewModel: GoalsViewModel = viewModel(factory = GoalsViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    // Isi sheet terakhir tetap digambar selama animasi tutup; [opened] membuat form baru (kosong) tiap dibuka.
    var sheet by remember { mutableStateOf<Sheet>(Sheet.Create) }
    var sheetOpen by remember { mutableStateOf(false) }
    var opened by remember { mutableIntStateOf(0) }
    // Aksi berhasil → tutup sheet.
    LaunchedEffect(state.form.doneCount) { sheetOpen = false }
    val open = { s: Sheet ->
        viewModel.clearErrors()
        sheet = s
        sheetOpen = true
        opened++
    }

    GoalsContent(
        onHistory = onHistory,
        goals = state.goals,
        loading = state.loading,
        onAddGoal = { open(Sheet.Create) },
        onAddFunds = { open(Sheet.Funds(it.id)) },
    )

    LiquidSheet(sheetOpen, onDismiss = { sheetOpen = false }) {
        key(opened) {
            when (val s = sheet) {
                Sheet.Create -> CreateGoalSheet(state, onCreate = viewModel::createGoal)
                is Sheet.Funds -> state.goals.firstOrNull { it.id == s.goalId }?.let { goal ->
                    AddFundsSheet(
                        goal = goal,
                        state = state,
                        onAddFunds = { viewModel.addFunds(goal.id, it) },
                        onCancelGoal = { viewModel.cancelGoal(goal.id) },
                    )
                }
            }
        }
    }
}

/**
 * Tab Goals. Target pertama menjadi kartu utama, sisanya "GOAL LAINNYA".
 * Tombol aksi hanya muncul bila handler-nya diberikan (mis. di pratinjau tanpa aksi).
 */
@Composable
fun GoalsContent(
    onHistory: () -> Unit,
    modifier: Modifier = Modifier,
    goals: List<Goal> = emptyList(),
    loading: Boolean = false,
    onAddGoal: (() -> Unit)? = null,
    onAddFunds: ((Goal) -> Unit)? = null,
) {
    val hero = goals.firstOrNull()
    val others = goals.drop(1)
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        TopBar(
            leading = { Spacer(Modifier.size(48.dp)) }, // hamburger ditunda sampai punya isi (DESIGN_SPEC §11.2)
            trailing = { GlassIconButton(R.drawable.ic_history, onHistory, "Semua transaksi") },
        )
        Column(Modifier.screenPad(), verticalArrangement = Arrangement.spacedBy(24.dp)) {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Text("Target Tabungan", style = appText(20f, 25f, Bold))
                if (onAddGoal != null) {
                    LiquidButton(
                        onClick = onAddGoal,
                        modifier = Modifier.height(36.dp),
                        surfaceColor = Ink.Accent.copy(alpha = 0.2f),
                        contentPadding = 13.dp,
                    ) {
                        Ico(R.drawable.ic_plus_small)
                        Text("Tambah Target", style = appText(14f, 20f, SemiBold, Ink.AccentLight))
                    }
                }
            }
            if (hero != null) HeroGoalCard(hero, onAddFunds?.let { { it(hero) } })
            if (hero != null) {
                Text(
                    "GOAL LAINNYA",
                    Modifier.padding(top = 16.dp),
                    style = appText(12f, 18f, Bold, Ink.Subtle, tracking = 1.2f),
                )
            }
            if (loading) Unit else if (others.isEmpty()) EmptyGoals(onAddGoal, hasHero = hero != null) else others.forEach { goal ->
                ImageGoalCard(goal, onAddFunds = onAddFunds?.let { { it(goal) } })
            }
        }
        Spacer(Modifier.height(bottomClearance()))
    }
}

@Composable
private fun PrimaryButton(label: String, @DrawableRes icon: Int, onClick: () -> Unit) {
    LiquidButton(onClick = onClick, modifier = Modifier.fillMaxWidth(), tint = Ink.Accent) {
        Ico(icon)
        Text(label, style = appText(14f, 20f, Bold, Ink.White))
    }
}

@Composable
private fun AmountLine(goal: Goal, modifier: Modifier = Modifier) {
    Text(
        buildAnnotatedString {
            withStyle(SpanStyle(fontWeight = Medium, color = Ink.Muted)) { append("${formatRupiah(goal.saved)} ") }
            withStyle(SpanStyle(fontWeight = Regular, color = Ink.Subtle)) { append("dari") }
            withStyle(SpanStyle(fontWeight = Medium, color = Ink.Muted)) { append(" ${formatRupiah(goal.target)}") }
        },
        modifier.fillMaxWidth(),
        style = appText(14f, 20f, Medium, Ink.Muted),
        textAlign = TextAlign.End,
    )
}

@Composable
private fun HeroGoalCard(goal: Goal, onAddFunds: (() -> Unit)?) {
    GlassCard(
        Modifier.fillMaxWidth(),
        tint = Brush.verticalGradient(listOf(Ink.Accent.copy(alpha = 0.18f), Color.Transparent)),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(16.dp), Alignment.CenterVertically) {
            IconCircle(R.drawable.ic_goal_shoe_large, 56.dp, Ink.Accent.copy(alpha = 0.2f))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(goal.name, style = appText(18f, 22.5f, Bold))
                Box(
                    Modifier
                        .clip(CircleShape)
                        .background(Ink.Accent.copy(alpha = 0.2f))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) { Text("${goal.percent}% Tercapai", style = appText(12f, 16f, SemiBold, Ink.AccentLight)) }
            }
        }
        Column(Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            ProgressBar(goal.progress, GoalBrush, 10.dp, Ink.Track)
            AmountLine(goal)
            planLine(goal)?.let { Text(it, Modifier.fillMaxWidth(), style = appText(12f, 16f, Regular, Ink.Subtle), textAlign = TextAlign.End) }
        }
        if (onAddFunds != null) {
            Box(Modifier.padding(top = 4.dp)) { PrimaryButton("Tambah Dana", R.drawable.ic_plus_fund, onAddFunds) }
        }
    }
}

@Composable
private fun EmptyGoals(onAddGoal: (() -> Unit)?, hasHero: Boolean) {
    GlassCard(
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
            if (hasHero) "Belum ada target lain, yuk mulai menabung..." else "Belum ada target, yuk mulai menabung...",
            Modifier.padding(horizontal = 37.dp),
            style = appText(14f, 20f, Regular, Ink.Muted),
            textAlign = TextAlign.Center,
        )
        if (onAddGoal != null) {
            Box(Modifier.padding(top = 8.dp)) {
                LiquidButton(onClick = onAddGoal, modifier = Modifier.height(42.dp), contentPadding = 21.dp) {
                    Text("+ Buat Target Baru", style = appText(14f, 20f, SemiBold))
                }
            }
        }
    }
}

/** Kartu target dengan foto di atas; tanpa foto, area gambar berupa latar gelap dengan ikon target. */
@Composable
private fun ImageGoalCard(goal: Goal, image: Painter? = null, onAddFunds: (() -> Unit)?) {
    GlassSurface(Modifier.fillMaxWidth(), contentPadding = PaddingValues(1.dp)) {
        Column {
            Box(Modifier.fillMaxWidth().height(192.dp).background(Ink.EmptyIconBg), contentAlignment = Alignment.Center) {
                if (image != null) {
                    Image(image, goal.name, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                } else {
                    Ico(R.drawable.ic_goal_shoe_large)
                }
                Box(
                    Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 12.dp, end = 12.dp)
                        .clip(CircleShape)
                        .background(Ink.AccentLight.copy(alpha = 0.2f))
                        .border(1.dp, Ink.AccentLight.copy(alpha = 0.2f), CircleShape)
                        .padding(horizontal = 13.dp, vertical = 5.dp)
                ) { Text("${goal.percent}% Tercapai", style = appText(12f, 16f, Bold, Ink.AccentLight)) }
            }
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(goal.name, style = appText(20f, 25f, Bold))
                        Text("Target: ${formatRupiah(goal.target)}", style = appText(12f, 16f, Regular, Ink.Muted))
                    }
                    Text(formatRupiah(goal.saved), style = appText(18f, 27f, Bold, Ink.AccentLight))
                }
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ProgressBar(goal.progress, GoalBrush, 10.dp, Ink.Track)
                    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                        val small = appText(10f, 15f, Medium, Ink.Subtle, tracking = 0.5f)
                        Text("MULAI", style = small)
                        Text("SISA RP ${formatRupiah(goal.remaining).removePrefix("Rp ")}", style = small)
                    }
                }
                if (onAddFunds != null) {
                    Box(Modifier.padding(top = 8.dp)) { PrimaryButton("Tambah Dana", R.drawable.ic_plus_fund, onAddFunds) }
                }
            }
        }
    }
}

/** "Rencana Rp 300.000/bulan · target 31 Des 2026", atau null bila keduanya kosong. */
private fun planLine(goal: Goal): String? = listOfNotNull(
    goal.plannedMonthly.takeIf { it > 0 }?.let { "Rencana ${formatRupiah(it)}/bulan" },
    goal.targetDate?.let { "target ${it.format(TargetDate)}" },
).joinToString(" · ").ifEmpty { null }

private val TargetDate = java.time.format.DateTimeFormatter.ofPattern("d MMM yyyy", java.util.Locale.forLanguageTag("id-ID"))

// Data contoh hanya untuk pratinjau di Android Studio.
private val PreviewGoals = listOf(
    Goal("Sepatu Lari", saved = 452_000, target = 800_000, plannedMonthly = 174_000, targetDate = java.time.LocalDate.of(2026, 12, 4)),
    Goal("Liburan Bali", saved = 600_000, target = 2_000_000),
)

@Preview(widthDp = 390, heightDp = 1100, showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun GoalsPreview() = ItungEnTheme {
    GoalsContent(onHistory = {}, goals = PreviewGoals, onAddGoal = {}, onAddFunds = {})
}

@Preview(widthDp = 390, heightDp = 600, showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun GoalsEmptyPreview() = ItungEnTheme { GoalsContent(onHistory = {}) }
