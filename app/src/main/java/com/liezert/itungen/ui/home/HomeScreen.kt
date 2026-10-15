package com.liezert.itungen.ui.home

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.liezert.glass.components.GlassCard
import com.liezert.glass.components.LiquidButton
import com.liezert.itungen.ItungEnApp
import com.liezert.itungen.R
import com.liezert.itungen.domain.model.Goal
import com.liezert.itungen.domain.model.Transaction
import com.liezert.itungen.domain.model.TransactionType
import com.liezert.itungen.domain.model.formatRupiah
import com.liezert.itungen.domain.model.formatRupiahShort
import com.liezert.itungen.domain.model.formatSigned
import com.liezert.itungen.domain.usecase.Budget
import com.liezert.itungen.domain.usecase.BudgetPeriod
import com.liezert.itungen.domain.usecase.BudgetStatus
import com.liezert.itungen.domain.usecase.QuickTemplate
import com.liezert.itungen.ui.components.AboveNavBar
import com.liezert.itungen.ui.components.GlassIconButton
import com.liezert.itungen.ui.components.Ico
import com.liezert.itungen.ui.components.IconCircle
import com.liezert.itungen.ui.components.ProgressBar
import com.liezert.itungen.ui.components.TopBar
import com.liezert.itungen.ui.components.UndoToast
import com.liezert.itungen.ui.components.amountColor
import com.liezert.itungen.ui.components.categoryLabel
import com.liezert.itungen.ui.components.toastMessage
import com.liezert.itungen.ui.components.bottomClearance
import com.liezert.itungen.ui.components.clockLabel
import com.liezert.itungen.ui.components.incoming
import com.liezert.itungen.ui.components.relativeDay
import com.liezert.itungen.ui.components.screenPad
import com.liezert.itungen.ui.components.style
import com.liezert.itungen.ui.theme.Bold
import com.liezert.itungen.ui.theme.Ink
import com.liezert.itungen.ui.theme.Medium
import com.liezert.itungen.ui.theme.Regular
import com.liezert.itungen.ui.theme.SemiBold
import com.liezert.itungen.ui.theme.appText
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

private const val RecentCount = 4

/** Pengganti nominal saat saldo disembunyikan. */
const val HiddenAmount = "Rp ••••••"

@Composable
fun HomeScreen(
    onHistory: () -> Unit,
    onSeeAll: () -> Unit,
    onOpenGoals: () -> Unit,
    onOpenBudget: () -> Unit,
    viewModel: HomeViewModel = viewModel(factory = HomeViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val undo by viewModel.undo.entries.collectAsStateWithLifecycle()
    val container = (LocalContext.current.applicationContext as ItungEnApp).container
    HomeContent(
        state, onHistory, onSeeAll, onOpenGoals, onOpenBudget,
        onQuickAdd = viewModel::quickAdd,
        balanceHidden = container.hideBalance,
        onToggleBalance = { container.setBalanceHidden(!container.hideBalance) },
    )
    // Di atas FAB "+" (56 dp + jarak 16 dp) agar tidak menutupinya.
    UndoToast(undo.toastMessage(), viewModel.undo::undoLatest, bottomOffset = AboveNavBar + 72.dp)
}

@Composable
fun HomeContent(
    state: HomeUiState,
    onHistory: () -> Unit,
    onSeeAll: () -> Unit,
    onOpenGoals: () -> Unit = {},
    onOpenBudget: () -> Unit = {},
    onQuickAdd: (QuickTemplate) -> Unit = {},
    modifier: Modifier = Modifier,
    balanceHidden: Boolean = false,
    onToggleBalance: () -> Unit = {},
) {
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        TopBar(
            leading = { Spacer(Modifier.size(48.dp)) }, // hamburger ditunda sampai punya isi (DESIGN_SPEC §11.2)
            trailing = { GlassIconButton(R.drawable.ic_history, onHistory, "Semua transaksi") },
        )
        Spacer(Modifier.height(8.dp))
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            if (!state.loading) {
                val budget = state.budget
                if (budget == null) BudgetSetupCard(onOpenBudget, Modifier.screenPad())
                else BudgetCard(budget, state.today, onOpenBudget, Modifier.screenPad())
            }
            BalanceCard(state, balanceHidden, onToggleBalance, Modifier.screenPad())
            if (state.templates.isNotEmpty()) QuickAddRow(state.templates, onQuickAdd)
            state.goal?.let { GoalPreviewCard(it, onOpenGoals, Modifier.screenPad()) }
            RecentCard(state.transactions.take(RecentCount), state, onSeeAll, Modifier.screenPad())
        }
        Spacer(Modifier.height(bottomClearance()))
    }
}

private val ShortDate = DateTimeFormatter.ofPattern("d MMM", Locale.forLanguageTag("id-ID"))

private fun BudgetStatus.label() = when (this) {
    BudgetStatus.HEMAT -> "Hemat"
    BudgetStatus.ON_TRACK -> "Sesuai rencana"
    BudgetStatus.MELEBIHI -> "Melebihi"
}

@Composable
private fun BudgetStatus.color() = when (this) {
    BudgetStatus.HEMAT -> Ink.Green
    BudgetStatus.ON_TRACK -> Ink.Blue
    BudgetStatus.MELEBIHI -> Ink.Red
}

/** Jawaban "boleh belanja berapa hari ini?": sisa jatah, jatah minggu ini, dan sampai kapan uang cukup (§5). */
@Composable
private fun BudgetCard(budget: Budget, today: LocalDate, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val statusColor = budget.status.color()
    GlassCard(
        modifier.fillMaxWidth(),
        tint = Brush.verticalGradient(listOf(statusColor.copy(alpha = 0.18f), Color.Transparent)),
        onClick = onClick,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
            Text("Jatah hari ini", style = appText(16f, 24f, Regular, Ink.Muted))
            if (!budget.unrealistic) {
                Text(
                    budget.status.label(),
                    Modifier.clip(CircleShape).background(statusColor.copy(alpha = 0.18f)).padding(horizontal = 10.dp, vertical = 3.dp),
                    style = appText(12f, 16f, SemiBold, statusColor),
                )
            }
        }
        if (budget.unrealistic) {
            Text("Anggaran belum realistis", style = appText(20f, 28f, Bold, Ink.Red))
            Text(
                "Pemasukan tidak cukup untuk tagihan, tabungan, dan setoran target. " +
                    "Tabungan maksimal yang masih mungkin: ${formatRupiah(budget.maxSavings)}/bulan. Ketuk untuk mengubah.",
                style = appText(14f, 20f, Regular, Ink.Gray),
            )
            return@GlassCard
        }

        if (budget.overdue) {
            Text(
                "Tanggal uang masuk sudah lewat. Ketuk untuk mengisi tanggal kiriman berikutnya.",
                style = appText(13f, 18f, Medium, Ink.Orange),
            )
        }
        val over = budget.todayRemaining < 0
        Text(
            if (over) "Lewat ${formatRupiah(-budget.todayRemaining)}" else formatRupiah(budget.todayRemaining),
            style = appText(32f, 40f, Bold, if (over) Ink.Red else Ink.White, tracking = -0.5f),
        )
        Text(
            if (over) "dari jatah ${formatRupiah(budget.todayAllowance)} hari ini"
            else "tersisa dari jatah ${formatRupiah(budget.todayAllowance)} hari ini",
            style = appText(14f, 20f, Regular, Ink.Gray),
        )
        val fraction = if (budget.todayAllowance > 0) budget.todayRemaining.toFloat() / budget.todayAllowance else 0f
        ProgressBar(fraction, SolidColor(statusColor), 8.dp, Ink.white(0.1f), Modifier.padding(top = 4.dp))

        Row(Modifier.fillMaxWidth().padding(top = 6.dp), Arrangement.spacedBy(12.dp)) {
            // Siklus mingguan = minggunya sendiri; selain itu tampilkan jatah minggu kalender ini.
            val (label, remaining) =
                if (budget.period == BudgetPeriod.WEEKLY) "Sisa periode ini" to budget.cycleRemaining
                else "Sisa minggu ini" to budget.weekRemaining
            BudgetStat(
                label,
                if (remaining < 0) "Lewat ${formatRupiah(-remaining)}" else formatRupiah(remaining),
                Modifier.weight(1f),
                if (remaining < 0) Ink.Red else Ink.White,
            )
            BudgetStat(
                "Uang masuk berikutnya",
                if (budget.overdue) "Belum diatur" else "${budget.cycleEnd.plusDays(1).format(ShortDate)} · ${budget.daysLeft} hari",
                Modifier.weight(1f),
            )
        }
        val runsOut = budget.runsOutOn
        Text(
            when {
                runsOut != null -> "Dengan pola belanja sekarang, uang habis sekitar ${runsOut.format(ShortDate)} — " +
                    "${ChronoUnit.DAYS.between(runsOut, budget.cycleEnd) + 1} hari sebelum uang masuk lagi."
                budget.cycleEnd.toEpochDay() - budget.cycleStart.toEpochDay() + 1 - budget.daysLeft < 3 ->
                    "Perkiraan sampai kapan uangmu cukup muncul setelah 3 hari pencatatan."
                else -> "Dengan pola belanja sekarang, uangmu cukup sampai uang masuk lagi."
            },
            style = appText(13f, 18f, Regular, if (runsOut == null) Ink.Gray else Ink.Orange),
        )
    }
}

@Composable
private fun BudgetStat(label: String, value: String, modifier: Modifier = Modifier, color: Color = Ink.White) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label, style = appText(12f, 16f, Medium, Ink.Gray), maxLines = 1)
        Text(value, style = appText(15f, 20f, SemiBold, color), maxLines = 1)
    }
}

@Composable
private fun BudgetSetupCard(onSetup: () -> Unit, modifier: Modifier = Modifier) {
    GlassCard(
        modifier.fillMaxWidth(),
        tint = Brush.verticalGradient(listOf(Ink.Blue.copy(alpha = 0.18f), Color.Transparent)),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text("Boleh belanja berapa hari ini?", style = appText(18f, 24f, Bold))
        Text(
            "Isi berapa uang yang masuk dan kapan (gaji, uang saku, atau kiriman) sekali saja. " +
                "ItungEn menghitung jatah harianmu, dan memberi tahu kalau mulai boros.",
            style = appText(14f, 20f, Regular, Ink.Gray),
        )
        LiquidButton(onClick = onSetup, modifier = Modifier.fillMaxWidth().padding(top = 4.dp), tint = Ink.Blue) {
            Text("Atur anggaran", style = appText(15f, 20f, SemiBold, Color.White))
        }
    }
}

@Composable
private fun BalanceCard(state: HomeUiState, hidden: Boolean, onToggle: () -> Unit, modifier: Modifier = Modifier) {
    GlassCard(
        modifier.fillMaxWidth(),
        tint = Brush.linearGradient(listOf(Ink.Blue.copy(alpha = 0.22f), Ink.Purple.copy(alpha = 0.10f))),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
            Text("Total Saldo", style = appText(16f, 24f, Regular, Ink.Muted))
            // Ikon mata: saldo disamarkan saat HP dilihat orang lain.
            Ico(
                if (hidden) R.drawable.ic_eye_off else R.drawable.ic_eye,
                Modifier.clip(CircleShape).clickable(role = Role.Button, onClick = onToggle).padding(4.dp),
                if (hidden) "Tampilkan saldo" else "Sembunyikan saldo",
            )
        }
        // Saldo boleh negatif tapi ditandai jelas (DESIGN_SPEC §7.1).
        Text(
            if (hidden) HiddenAmount else formatRupiah(state.balance),
            style = appText(24f, 32f, Bold, if (state.balance < 0 && !hidden) Ink.Red else Ink.White),
        )
        Row(Modifier.fillMaxWidth().padding(top = 4.dp), Arrangement.spacedBy(12.dp)) {
            BudgetStat("Masuk bulan ini", if (hidden) HiddenAmount else "+" + formatRupiah(state.monthIncome), Modifier.weight(1f))
            BudgetStat("Keluar bulan ini", if (hidden) HiddenAmount else "-" + formatRupiah(state.monthExpense), Modifier.weight(1f))
        }
    }
}

/** Transaksi yang sering diulang, dicatat hari ini dengan sekali ketuk (bisa diurungkan). */
@Composable
private fun QuickAddRow(templates: List<QuickTemplate>, onQuickAdd: (QuickTemplate) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Catat cepat", Modifier.screenPad(), style = appText(14f, 20f, SemiBold, Ink.Muted))
        LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(templates) { template ->
                val income = template.type == TransactionType.INCOME
                LiquidButton(onClick = { onQuickAdd(template) }, modifier = Modifier.height(44.dp)) {
                    Text(template.label, style = appText(14f, 20f, Medium), maxLines = 1)
                    Text(
                        (if (income) "+" else "-") + formatRupiahShort(template.amount).removePrefix("Rp "),
                        style = appText(14f, 20f, SemiBold, if (income) Ink.Green else Ink.Red),
                    )
                }
            }
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
                Text(item.note ?: item.categoryLabel, style = appText(16f, 24f), maxLines = 1)
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
