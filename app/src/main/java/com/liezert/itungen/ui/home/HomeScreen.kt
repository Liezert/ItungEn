package com.liezert.itungen.ui.home

import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.minimumInteractiveComponentSize
import com.liezert.itungen.ui.components.displayNote
import com.liezert.itungen.ui.design.itungImageFrame
import com.liezert.itungen.ui.components.formatIn
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.liezert.itungen.ui.components.displayName
import androidx.activity.compose.ReportDrawnWhen
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
import com.liezert.itungen.ItungEnApp
import com.liezert.itungen.R
import com.liezert.itungen.domain.model.Goal
import com.liezert.itungen.domain.model.Transaction
import com.liezert.itungen.domain.model.TransactionType
import com.liezert.itungen.domain.model.formatRupiah
import com.liezert.itungen.domain.model.AmountKind
import com.liezert.itungen.ui.components.AmountText
import com.liezert.itungen.ui.goals.GoalImage
import com.liezert.itungen.ui.components.HiddenAmount
import com.liezert.itungen.ui.components.amountKind
import com.liezert.itungen.domain.usecase.Budget
import com.liezert.itungen.domain.usecase.BudgetPeriod
import com.liezert.itungen.domain.usecase.BudgetStatus
import com.liezert.itungen.domain.usecase.LimitLevel
import com.liezert.itungen.domain.usecase.LimitWarning
import com.liezert.itungen.domain.usecase.QuickTemplate
import com.liezert.itungen.domain.usecase.Shortfall
import com.liezert.itungen.ui.components.AboveNavBar
import com.liezert.itungen.ui.components.ItungIconButton
import com.liezert.itungen.ui.components.Ico
import com.liezert.itungen.ui.components.IconCircle
import com.liezert.itungen.ui.components.ProgressBar
import com.liezert.itungen.ui.components.TopBar
import com.liezert.itungen.ui.components.UndoToast
import com.liezert.itungen.ui.components.categoryLabel
import com.liezert.itungen.ui.components.toastMessage
import com.liezert.itungen.ui.components.bottomClearance
import com.liezert.itungen.ui.components.clockLabel
import com.liezert.itungen.ui.components.relativeDay
import com.liezert.itungen.ui.components.screenPad
import com.liezert.itungen.ui.components.style
import com.liezert.itungen.ui.theme.Bold
import com.liezert.itungen.ui.theme.Ink
import com.liezert.itungen.ui.theme.Medium
import com.liezert.itungen.ui.theme.Regular
import com.liezert.itungen.ui.theme.SemiBold
import com.liezert.itungen.ui.theme.appText
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale
import androidx.compose.ui.text.style.TextOverflow
import com.liezert.itungen.ui.design.ItungCard
import com.liezert.itungen.ui.design.ItungButton
import com.liezert.itungen.ui.design.ItungTransactionRow


/** Nominal, atau [HiddenAmount] saat "Sembunyikan saldo" aktif: berlaku untuk semua nominal di Beranda. */
private fun money(amount: Long, hidden: Boolean) = if (hidden) HiddenAmount else formatRupiah(amount)

/** Nominal per periode anggaran: "Rp300.000/bulan" / "Rp300.000/month"; tanpa periode = nominal saja. */
@Composable
private fun perPeriod(amount: String, period: BudgetPeriod) = when (period) {
    BudgetPeriod.MONTHLY -> stringResource(R.string.per_month, amount)
    BudgetPeriod.WEEKLY -> stringResource(R.string.per_week, amount)
    BudgetPeriod.UNTIL_DATE -> amount
}

/**
 * Kenapa anggaran tidak realistis, menunjuk ke bagian yang membuatnya minus (dulu selalu menyalahkan tabungan,
 * padahal sering kali setoran target). Dipakai Beranda dan pratinjau di layar Anggaran.
 */
@Composable
fun unrealisticMessage(budget: Budget, hidden: Boolean = false): String = when (budget.shortfall) {
    Shortfall.GOALS -> stringResource(R.string.budget_short_goals, perPeriod(money(budget.goals, hidden), budget.period))
    Shortfall.SAVINGS -> stringResource(R.string.budget_short_savings, perPeriod(money(budget.maxSavings, hidden), budget.period))
    Shortfall.BILLS -> stringResource(R.string.budget_short_bills)
}

@Composable
fun HomeScreen(
    onHistory: () -> Unit,
    onSeeAll: () -> Unit,
    onOpenGoals: () -> Unit,
    onOpenBudget: () -> Unit,
    /** Peringatan batas kategori diketuk: buka Insight. */
    onOpenInsight: () -> Unit = {},
    /** Ketuk riwayat pemasukan/pengeluaran: buka layar ubah. */
    onEdit: (Long) -> Unit = {},
    viewModel: HomeViewModel = viewModel(factory = HomeViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val undo by viewModel.undo.entries.collectAsStateWithLifecycle()
    // Waktu "siap dipakai" untuk pengukuran start dingin (docs/PERFORMANCE.md): saat data Beranda pertama tampil.
    ReportDrawnWhen { !state.loading }
    val container = (LocalContext.current.applicationContext as ItungEnApp).container
    HomeContent(
        state, onHistory, onSeeAll,
        onOpenGoals = onOpenGoals,
        onOpenBudget = onOpenBudget,
        onOpenInsight = onOpenInsight,
        onQuickAdd = viewModel::quickAdd,
        balanceHidden = container.hideBalance,
        onToggleBalance = { container.setBalanceHidden(!container.hideBalance) },
        onEdit = { onEdit(it.id) },
    )
    // Di atas FAB "+" (56 dp + jarak 16 dp) agar tidak menutupinya.
    UndoToast(undo.toastMessage(), viewModel.undo::undoLatest, bottomOffset = AboveNavBar + 72.dp)
}

@Composable
fun HomeContent(
    state: HomeUiState,
    onHistory: () -> Unit,
    onSeeAll: () -> Unit,
    modifier: Modifier = Modifier,
    onOpenGoals: () -> Unit = {},
    onOpenBudget: () -> Unit = {},
    onOpenInsight: () -> Unit = {},
    onQuickAdd: (QuickTemplate) -> Unit = {},
    balanceHidden: Boolean = false,
    onToggleBalance: () -> Unit = {},
    onEdit: (Transaction) -> Unit = {},
) {
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        TopBar(
            leading = { Spacer(Modifier.size(48.dp)) }, // hamburger ditunda sampai punya isi (DESIGN_SPEC §11.2)
            trailing = { ItungIconButton(R.drawable.ic_history, onHistory, stringResource(R.string.all_transactions)) },
        )
        Spacer(Modifier.height(8.dp))
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            // Saldo = jawaban pertama saat app dibuka ("uangku berapa?"), lalu jatah hari ini.
            BalanceCard(state, balanceHidden, onToggleBalance, Modifier.screenPad())
            if (!state.loading) {
                val budget = state.budget
                if (budget == null) BudgetSetupCard(onOpenBudget, Modifier.screenPad())
                else {
                    // Setoran target yang membuat anggaran minus diubah di Goals, bukan di layar Anggaran.
                    val goalsAtFault = budget.unrealistic && budget.shortfall == Shortfall.GOALS
                    BudgetCard(budget, balanceHidden, if (goalsAtFault) onOpenGoals else onOpenBudget, Modifier.screenPad())
                }
            }
            if (state.limitWarnings.isNotEmpty()) LimitWarningCard(state.limitWarnings, balanceHidden, onOpenInsight, Modifier.screenPad())
            if (state.templates.isNotEmpty()) QuickAddRow(state.templates, balanceHidden, onQuickAdd)
            state.goal?.let { GoalPreviewCard(it, balanceHidden, onOpenGoals, Modifier.screenPad()) }
            RecentCard(state.recent, state, balanceHidden, onSeeAll, onEdit, onOpenGoals, Modifier.screenPad())
        }
        Spacer(Modifier.height(bottomClearance()))
    }
}

@Composable
private fun BudgetStatus.label() = stringResource(
    when (this) {
        BudgetStatus.HEMAT -> R.string.budget_status_frugal
        BudgetStatus.ON_TRACK -> R.string.budget_status_on_track
        BudgetStatus.MELEBIHI -> R.string.budget_status_over
    }
)

@Composable
private fun BudgetStatus.color() = when (this) {
    BudgetStatus.HEMAT -> Ink.Green
    BudgetStatus.ON_TRACK -> Ink.Blue
    BudgetStatus.MELEBIHI -> Ink.Red
}

/** Jawaban "boleh belanja berapa hari ini?": sisa jatah, jatah minggu ini, dan sampai kapan uang cukup (§5). */
@Composable
private fun BudgetCard(budget: Budget, hidden: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val statusColor = budget.status.color()
    ItungCard(
        modifier.fillMaxWidth(),
        tint = Brush.verticalGradient(listOf(statusColor.copy(alpha = 0.18f), Color.Transparent)),
        onClick = onClick,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
            Text(stringResource(R.string.today_allowance), Modifier.weight(1f).padding(end = 8.dp), style = appText(16f, 24f, Regular, Ink.Muted))
            if (!budget.unrealistic) {
                Text(
                    budget.status.label(),
                    Modifier.clip(CircleShape).background(statusColor.copy(alpha = 0.18f)).padding(horizontal = 10.dp, vertical = 3.dp),
                    style = appText(12f, 16f, SemiBold, statusColor),
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (budget.unrealistic) {
            Text(stringResource(R.string.budget_unrealistic), style = appText(20f, 28f, Bold, Ink.Red))
            Text(stringResource(R.string.tap_to_change, unrealisticMessage(budget, hidden)), style = appText(14f, 20f, Regular, Ink.Gray))
            // Jatah harian tidak bisa dihitung, tetapi belanja hari ini tetap terlihat.
            Text(stringResource(R.string.spent_today, money(-budget.todayRemaining, hidden)), style = appText(14f, 20f, Medium, Ink.White))
            return@ItungCard
        }

        if (budget.overdue) {
            Text(
                stringResource(R.string.budget_overdue),
                style = appText(13f, 18f, Medium, Ink.Orange),
            )
        }
        val over = budget.todayRemaining < 0
        Text(
            if (over) stringResource(R.string.over_by, money(-budget.todayRemaining, hidden)) else money(budget.todayRemaining, hidden),
            style = appText(32f, 40f, Bold, if (over) Ink.Red else Ink.White, tracking = -0.5f),
        )
        Text(
            stringResource(if (over) R.string.of_today_allowance else R.string.left_of_today_allowance, money(budget.todayAllowance, hidden)),
            style = appText(14f, 20f, Regular, Ink.Gray),
        )
        val fraction = if (budget.todayAllowance > 0) budget.todayRemaining.toFloat() / budget.todayAllowance else 0f
        ProgressBar(fraction, SolidColor(statusColor), 8.dp, Ink.white(0.1f), Modifier.padding(top = 4.dp))

        Row(Modifier.fillMaxWidth().padding(top = 6.dp), Arrangement.spacedBy(12.dp)) {
            // Siklus mingguan = minggunya sendiri; selain itu tampilkan jatah minggu kalender ini.
            val (label, remaining) =
                if (budget.period == BudgetPeriod.WEEKLY) stringResource(R.string.left_this_period) to budget.cycleRemaining
                else stringResource(R.string.left_this_week) to budget.weekRemaining
            BudgetStat(
                label,
                if (remaining < 0) stringResource(R.string.over_by, money(-remaining, hidden)) else money(remaining, hidden),
                Modifier.weight(1f),
                if (remaining < 0) Ink.Red else Ink.White,
            )
            BudgetStat(
                stringResource(R.string.next_income),
                if (budget.overdue) stringResource(R.string.not_set)
                else stringResource(R.string.date_and_days, budget.cycleEnd.plusDays(1).formatIn("d MMM"), pluralStringResource(R.plurals.days, budget.daysLeft, budget.daysLeft)),
                Modifier.weight(1f),
            )
        }
        val runsOut = budget.runsOutOn
        Text(
            when {
                // Sudah habis: "habis sekitar <hari ini>" terdengar seperti perkiraan, padahal sudah terjadi.
                budget.cycleRemaining <= 0 -> pluralStringResource(R.plurals.money_ran_out, budget.daysLeft, budget.daysLeft)
                runsOut != null -> {
                    val days = (ChronoUnit.DAYS.between(runsOut, budget.cycleEnd) + 1).toInt()
                    pluralStringResource(R.plurals.money_runs_out_on, days, runsOut.formatIn("d MMM"), days)
                }
                budget.cycleEnd.toEpochDay() - budget.cycleStart.toEpochDay() + 1 - budget.daysLeft < 3 ->
                    stringResource(R.string.forecast_after_3_days)
                else -> stringResource(R.string.money_lasts)
            },
            style = appText(13f, 18f, Regular, if (runsOut == null && budget.cycleRemaining > 0) Ink.Gray else Ink.Orange),
        )
    }
}

@Composable
private fun BudgetStat(label: String, value: String, modifier: Modifier = Modifier, color: Color = Ink.White) {
    // Dua baris: dua kolom sempit + huruf besar (130–150%) dulu memotong "25 Okt · 8 hari" menjadi "25 Okt · 8 …".
    Column(modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label, style = appText(12f, 16f, Medium, Ink.Gray), maxLines = 2, overflow = TextOverflow.Ellipsis)
        // Satu kata (nominal) tidak boleh pecah di tengah: satu baris yang mengecil; selain itu membungkus di spasi.
        val oneWord = ' ' !in value
        val style = appText(15f, 20f, SemiBold, color)
        Text(
            value,
            style = style,
            maxLines = if (oneWord) 1 else 2,
            overflow = TextOverflow.Ellipsis,
            autoSize = if (oneWord) TextAutoSize.StepBased(minFontSize = 8.sp, maxFontSize = style.fontSize) else null,
        )
    }
}

/** Seperti [BudgetStat], untuk nominal pemasukan/pengeluaran (hijau/merah, tanpa +/-). */
@Composable
private fun AmountStat(label: String, amount: Long, kind: AmountKind, hidden: Boolean, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label, style = appText(12f, 16f, Medium, Ink.Gray), maxLines = 2, overflow = TextOverflow.Ellipsis)
        AmountText(amount, kind, appText(15f, 20f, SemiBold), hidden = hidden)
    }
}

/** Kategori yang mendekati (≥ 80%) atau melewati batas bulanannya; ketuk untuk melihat di Insight. */
@Composable
private fun LimitWarningCard(warnings: List<LimitWarning>, hidden: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val over = warnings.any { it.level == LimitLevel.OVER }
    val color = if (over) Ink.Red else Ink.Orange
    ItungCard(
        modifier.fillMaxWidth(),
        tint = Brush.verticalGradient(listOf(color.copy(alpha = 0.16f), Color.Transparent)),
        onClick = onClick,
        padding = 17.dp,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(stringResource(R.string.category_limits_month), style = appText(14f, 20f, SemiBold, color))
        warnings.take(3).forEach { w ->
            val name = w.spend.category.displayName()
            Text(
                if (w.level == LimitLevel.OVER) stringResource(R.string.limit_over, name, money(w.spend.total, hidden), money(w.limit, hidden))
                else stringResource(R.string.limit_near, name, (w.spend.total * 100 / w.limit).toInt(), money(w.limit, hidden)),
                style = appText(13f, 18f, Regular, Ink.White),
            )
        }
    }
}

@Composable
private fun BudgetSetupCard(onSetup: () -> Unit, modifier: Modifier = Modifier) {
    ItungCard(
        modifier.fillMaxWidth(),
        tint = Brush.verticalGradient(listOf(Ink.Blue.copy(alpha = 0.18f), Color.Transparent)),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(stringResource(R.string.budget_setup_title), style = appText(18f, 24f, Bold))
        Text(
            stringResource(R.string.budget_setup_body),
            style = appText(14f, 20f, Regular, Ink.Gray),
        )
        ItungButton(onClick = onSetup, modifier = Modifier.fillMaxWidth().padding(top = 4.dp), tint = Ink.Blue) {
            Text(stringResource(R.string.budget_setup_action), style = appText(15f, 20f, SemiBold, Color.White))
        }
    }
}

@Composable
private fun BalanceCard(state: HomeUiState, hidden: Boolean, onToggle: () -> Unit, modifier: Modifier = Modifier) {
    ItungCard(
        modifier.fillMaxWidth(),
        tint = Brush.linearGradient(listOf(Ink.Blue.copy(alpha = 0.22f), Ink.Purple.copy(alpha = 0.10f))),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
            Text(stringResource(R.string.total_balance), style = appText(16f, 24f, Regular, Ink.Muted))
            // Ikon mata: saldo disamarkan saat HP dilihat orang lain.
            Ico(
                if (hidden) R.drawable.ic_eye_off else R.drawable.ic_eye,
                Modifier.clip(CircleShape).clickable(role = Role.Button, onClick = onToggle).minimumInteractiveComponentSize().padding(4.dp),
                stringResource(if (hidden) R.string.balance_show else R.string.balance_hide),
            )
        }
        // Saldo = nominal netral: warna teks biasa (hijau/merah hanya untuk pemasukan/pengeluaran). Saldo negatif tetap
        // jelas lewat tanda minus ("-Rp50.000", DESIGN_SPEC §7.1), bukan warna.
        Text(
            if (hidden) HiddenAmount else formatRupiah(state.balance),
            style = appText(30f, 38f, Bold, Ink.White),
        )
        Row(Modifier.fillMaxWidth().padding(top = 4.dp), Arrangement.spacedBy(12.dp)) {
            AmountStat(stringResource(R.string.income_this_month), state.monthIncome, AmountKind.INCOME, hidden, Modifier.weight(1f))
            AmountStat(stringResource(R.string.expense_this_month), state.monthExpense, AmountKind.EXPENSE, hidden, Modifier.weight(1f))
        }
    }
}

/** Transaksi yang sering diulang, dicatat hari ini dengan sekali ketuk (bisa diurungkan). */
@Composable
private fun QuickAddRow(templates: List<QuickTemplate>, hidden: Boolean, onQuickAdd: (QuickTemplate) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(stringResource(R.string.quick_add), Modifier.screenPad(), style = appText(14f, 20f, SemiBold, Ink.Muted))
        LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(templates) { template ->
                val income = template.type == TransactionType.INCOME
                ItungButton(onClick = { onQuickAdd(template) }, modifier = Modifier.heightIn(min = 44.dp)) {
                    Text(template.note ?: template.category.displayName(), style = appText(14f, 20f, Medium), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    AmountText(
                        template.amount,
                        if (income) AmountKind.INCOME else AmountKind.EXPENSE,
                        appText(14f, 20f, SemiBold),
                        Modifier.padding(start = 6.dp),
                        hidden = hidden,
                        short = true,
                    )
                }
            }
        }
    }
}

@Composable
private fun GoalPreviewCard(goal: Goal, hidden: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    ItungCard(
        modifier.fillMaxWidth(),
        tint = Brush.verticalGradient(listOf(Ink.Accent.copy(alpha = 0.16f), Color.Transparent)),
        onClick = onClick,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
            Text(stringResource(R.string.savings_goals), style = appText(16f, 24f))
            Ico(R.drawable.ic_target)
        }
        Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(16.dp), Alignment.CenterVertically) {
            // Persegi panjang berbingkai tema, sama seperti kartu di Goals (bukan lingkaran seperti foto profil).
            GoalImage(goal.imagePath, null, Modifier.size(64.dp, 48.dp).itungImageFrame()) { Ico(R.drawable.ic_goal_shoe) }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                    Text(goal.name, style = appText(16f, 24f, SemiBold))
                    Text(stringResource(R.string.percent_value, goal.percent), style = appText(16f, 24f, Regular, Ink.Blue))
                }
                Box(Modifier.fillMaxWidth().height(12.dp).padding(top = 4.dp)) {
                    ProgressBar(goal.progress, Brush.horizontalGradient(listOf(Ink.Blue, Ink.Cyan)), 8.dp, Ink.white(0.1f))
                }
                Text(
                    stringResource(R.string.amount_of_amount, money(goal.saved, hidden), money(goal.target, hidden)),
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
    hidden: Boolean,
    onSeeAll: () -> Unit,
    onEdit: (Transaction) -> Unit,
    onOpenGoals: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ItungCard(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
            Text(stringResource(R.string.recent_history), Modifier.weight(1f), style = appText(16f, 24f))
            Text(
                stringResource(R.string.see_all),
                Modifier.clip(CircleShape).clickable(role = Role.Button, onClick = onSeeAll).minimumInteractiveComponentSize().padding(horizontal = 8.dp),
                style = appText(16f, 24f, Regular, Ink.Blue),
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (items.isEmpty() && !state.loading) {
            Text(stringResource(R.string.recent_empty), style = appText(16f, 24f, Regular, Ink.Gray))
        }
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) { items.forEach { RecentRow(it, state, hidden, onEdit, onOpenGoals) } }
    }
}

@Composable
private fun RecentRow(item: Transaction, state: HomeUiState, hidden: Boolean, onEdit: (Transaction) -> Unit, onOpenGoals: () -> Unit) {
    val style = item.style()
    val day = relativeDay(item.date, state.today)
    // Jam hanya untuk hari ini / kemarin, seperti Figma.
    val clock = if (item.date >= state.today.minusDays(1)) item.clockLabel(state.zone) else null
    // Transfer goal tidak bisa diubah dari sini (mengikuti goal-nya): ketukan membuka Goals.
    val editable = item.type == TransactionType.INCOME || item.type == TransactionType.EXPENSE
    ItungTransactionRow(
        icon = { IconCircle(style.rowIcon, 40.dp, Ink.white(0.05f), border = Ink.white(0.14f)) },
        title = { Text(item.displayNote() ?: item.categoryLabel(), style = appText(16f, 24f), maxLines = 2, overflow = TextOverflow.Ellipsis) },
        subtitle = {
            Text(
                if (clock != null) stringResource(R.string.day_and_time, day, clock) else day,
                style = appText(16f, 24f, Regular, Ink.Gray),
                maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
        },
        amount = { AmountText(item.amount, item.amountKind, appText(16f, 24f, Regular, Ink.Gray), hidden = hidden, arrow = true) },
        modifier = if (editable) Modifier.clickable(onClickLabel = stringResource(R.string.action_edit), role = Role.Button) { onEdit(item) }
        else Modifier.clickable(onClickLabel = stringResource(R.string.open_goals), role = Role.Button, onClick = onOpenGoals),
    )
}
