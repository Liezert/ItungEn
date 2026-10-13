package com.liezert.itungen.ui.home

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.liezert.glass.components.GlassCard
import com.liezert.glass.components.GlassSurface
import com.liezert.itungen.R
import com.liezert.itungen.domain.model.Goal
import com.liezert.itungen.domain.model.Transaction
import com.liezert.itungen.domain.model.formatRupiah
import com.liezert.itungen.domain.model.formatSigned
import com.liezert.itungen.domain.usecase.filterTransactions
import com.liezert.itungen.ui.components.GlassIconButton
import com.liezert.itungen.ui.components.Ico
import com.liezert.itungen.ui.components.IconCircle
import com.liezert.itungen.ui.components.PillChip
import com.liezert.itungen.ui.components.ProgressBar
import com.liezert.itungen.ui.components.TopBar
import com.liezert.itungen.ui.components.amountColor
import com.liezert.itungen.ui.components.bottomClearance
import com.liezert.itungen.ui.components.clockLabel
import com.liezert.itungen.ui.components.incoming
import com.liezert.itungen.ui.components.relativeDay
import com.liezert.itungen.ui.components.screenPad
import com.liezert.itungen.ui.components.style
import com.liezert.itungen.ui.theme.Ink
import com.liezert.itungen.ui.theme.Regular
import com.liezert.itungen.ui.theme.SemiBold
import com.liezert.itungen.ui.theme.appText

private const val RecentCount = 4

@Composable
fun HomeScreen(
    onHistory: () -> Unit,
    onSeeAll: () -> Unit,
    onOpenGoals: () -> Unit,
    viewModel: HomeViewModel = viewModel(factory = HomeViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    HomeContent(state, onHistory, onSeeAll, onOpenGoals)
}

@Composable
fun HomeContent(
    state: HomeUiState,
    onHistory: () -> Unit,
    onSeeAll: () -> Unit,
    onOpenGoals: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    // 0 = "Semua"; i > 0 = state.filters[i - 1]
    var selected by rememberSaveable { mutableIntStateOf(0) }
    val labels = listOf("Semua") + state.filters
    val recent = filterTransactions(state.transactions, "", state.filters.getOrNull(selected - 1)).take(RecentCount)

    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        TopBar(
            leading = { Spacer(Modifier.size(48.dp)) }, // hamburger ditunda sampai punya isi (DESIGN_SPEC §11.2)
            trailing = { GlassIconButton(R.drawable.ic_history, onHistory, "Semua transaksi") },
        )
        Spacer(Modifier.height(8.dp))
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            BalanceCard(state, Modifier.screenPad())
            if (state.weekSpent > 0) FyiCard(state.weekSpent, Modifier.screenPad())
            LazyRow(
                Modifier.fillMaxWidth().padding(vertical = 4.dp),
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                itemsIndexed(labels) { i, label ->
                    PillChip(
                        label = label,
                        selected = i == selected,
                        onClick = { selected = i },
                        selectedText = appText(16f, 24f, Regular, Ink.Black),
                        unselectedText = appText(16f, 24f, Regular, Ink.Gray),
                    )
                }
            }
            state.goal?.let { GoalPreviewCard(it, onOpenGoals, Modifier.screenPad()) }
            RecentCard(recent, state, onSeeAll, Modifier.screenPad())
        }
        Spacer(Modifier.height(bottomClearance()))
    }
}

@Composable
private fun BalanceCard(state: HomeUiState, modifier: Modifier = Modifier) {
    GlassCard(
        modifier.fillMaxWidth(),
        tint = Brush.linearGradient(listOf(Ink.Blue.copy(alpha = 0.30f), Ink.Purple.copy(alpha = 0.14f))),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
            Text("Total Saldo", style = appText(16f, 24f, Regular, Ink.Muted))
            Ico(R.drawable.ic_wallet)
        }
        // Saldo boleh negatif tapi ditandai jelas (DESIGN_SPEC §7.1).
        Text(
            formatRupiah(state.balance),
            Modifier.padding(top = 4.dp),
            style = appText(16f, 24f, color = if (state.balance < 0) Ink.Red else Ink.White),
        )
        if (state.monthNet != 0L) {
            val up = state.monthNet > 0
            val color = if (up) Ink.Green else Ink.Red
            Row(Modifier.padding(top = 8.dp), Arrangement.spacedBy(8.dp), Alignment.CenterVertically) {
                Ico(R.drawable.ic_trend_up, Modifier.graphicsLayer { scaleY = if (up) 1f else -1f })
                Text(
                    formatSigned(kotlin.math.abs(state.monthNet), incoming = up) + " bulan ini",
                    style = appText(16f, 24f, Regular, color),
                )
            }
        }
    }
}

// ponytail: info minggu ini saja; status HEMAT/ON_TRACK/MELEBIHI + "Yuk berhemat" menunggu RecommendationEngine.
@Composable
private fun FyiCard(weekSpent: Long, modifier: Modifier = Modifier) {
    GlassSurface(
        modifier.fillMaxWidth(),
        tint = SolidColor(Ink.Orange.copy(alpha = 0.12f)),
        contentPadding = PaddingValues(17.dp),
    ) {
        Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(16.dp)) {
            IconCircle(R.drawable.ic_warn_trend_down, 40.dp, Ink.Orange.copy(alpha = 0.2f))
            Text(
                buildAnnotatedString {
                    withStyle(SpanStyle(color = Ink.Orange, fontWeight = SemiBold)) { append("FYI!") }
                    withStyle(SpanStyle(color = Ink.White, fontWeight = Regular)) {
                        append(" Kamu sudah mengeluarkan ${formatRupiah(weekSpent)} minggu ini.")
                    }
                },
                Modifier.padding(end = 6.dp),
                style = appText(16f, 26f, SemiBold, Ink.Orange),
            )
        }
    }
}

@Composable
private fun GoalPreviewCard(goal: Goal, onClick: () -> Unit, modifier: Modifier = Modifier) {
    GlassCard(
        modifier.fillMaxWidth(),
        tint = Brush.verticalGradient(listOf(Ink.Accent.copy(alpha = 0.16f), Color.Transparent)),
        onClick = onClick,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
            Text("Target Tabungan", style = appText(16f, 24f))
            Ico(R.drawable.ic_target)
        }
        Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(16.dp), Alignment.CenterVertically) {
            IconCircle(R.drawable.ic_goal_shoe, 48.dp, Ink.white(0.05f), border = Ink.white(0.14f))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                    Text(goal.name, style = appText(16f, 24f, SemiBold))
                    Text("${goal.percent}%", style = appText(16f, 24f, Regular, Ink.Blue))
                }
                Box(Modifier.fillMaxWidth().height(12.dp).padding(top = 4.dp)) {
                    ProgressBar(goal.progress, Brush.horizontalGradient(listOf(Ink.Blue, Ink.Cyan)), 8.dp, Ink.white(0.1f))
                }
                Text(
                    "${formatRupiah(goal.saved)} dari ${formatRupiah(goal.target)}",
                    Modifier.padding(top = 4.dp),
                    style = appText(16f, 24f, Regular, Ink.Gray),
                )
            }
        }
    }
}

@Composable
private fun RecentCard(
    items: List<Transaction>,
    state: HomeUiState,
    onSeeAll: () -> Unit,
    modifier: Modifier = Modifier,
) {
    GlassCard(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
            Text("Riwayat Terakhir", style = appText(16f, 24f))
            Text(
                "Lihat Semua",
                Modifier.clickable(role = Role.Button, onClick = onSeeAll),
                style = appText(16f, 24f, Regular, Ink.Blue),
            )
        }
        if (items.isEmpty() && !state.loading) {
            Text("Belum ada transaksi. Tekan + untuk mencatat.", style = appText(16f, 24f, Regular, Ink.Gray))
        }
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) { items.forEach { RecentRow(it, state) } }
    }
}

@Composable
private fun RecentRow(item: Transaction, state: HomeUiState) {
    val style = item.style()
    val day = relativeDay(item.date, state.today)
    // Jam hanya untuk hari ini / kemarin, seperti Figma.
    val clock = if (item.date >= state.today.minusDays(1)) item.clockLabel(state.zone) else null
    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
        Row(
            Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconCircle(style.rowIcon, 40.dp, Ink.white(0.05f), border = Ink.white(0.14f))
            Column(Modifier.weight(1f)) {
                Text(item.note ?: item.category?.name ?: "Transfer", style = appText(16f, 24f), maxLines = 1)
                Text(
                    if (clock != null) "$day, $clock" else day,
                    style = appText(16f, 24f, Regular, Ink.Gray),
                    maxLines = 1,
                )
            }
        }
        Text(
            formatSigned(item.amount, item.incoming),
            Modifier.padding(start = 12.dp),
            style = appText(16f, 24f, Regular, item.amountColor),
            maxLines = 1,
        )
    }
}
