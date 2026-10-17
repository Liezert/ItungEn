package com.liezert.itungen.ui.budget

import com.liezert.itungen.ui.components.SegmentLabel
import com.liezert.itungen.ui.components.formatIn
import androidx.annotation.StringRes
import androidx.compose.ui.res.stringResource
import com.liezert.itungen.ui.components.dateLocale
import com.liezert.itungen.ui.components.displayName
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.liezert.itungen.ItungEnApp
import com.liezert.itungen.R
import com.liezert.itungen.domain.model.Category
import com.liezert.itungen.domain.model.CategoryType
import com.liezert.itungen.domain.model.GoalStatus
import com.liezert.itungen.domain.model.formatRupiah
import com.liezert.itungen.domain.usecase.BudgetPeriod
import com.liezert.itungen.domain.usecase.BudgetPlan
import com.liezert.itungen.domain.usecase.computeBudget
import com.liezert.itungen.domain.usecase.convertPerPeriod
import com.liezert.itungen.domain.usecase.periodDays
import com.liezert.itungen.ui.home.unrealisticMessage
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import com.liezert.itungen.domain.usecase.cycleOf
import com.liezert.itungen.ui.components.AmountField
import com.liezert.itungen.ui.insight.CategoryLimitSheet
import com.liezert.itungen.ui.components.ChoiceSheet
import com.liezert.itungen.ui.components.ItungIconButton
import com.liezert.itungen.ui.components.Ico
import com.liezert.itungen.ui.components.ItungAlertDialog
import com.liezert.itungen.ui.components.ItungDatePickerDialog
import androidx.activity.compose.BackHandler
import com.liezert.itungen.ui.components.rememberAppDatePickerState
import com.liezert.itungen.ui.components.TopBar
import com.liezert.itungen.ui.components.bottomClearance
import com.liezert.itungen.ui.components.screenPad
import com.liezert.itungen.ui.theme.Bold
import com.liezert.itungen.ui.theme.Ink
import com.liezert.itungen.ui.theme.Medium
import com.liezert.itungen.ui.theme.Regular
import com.liezert.itungen.ui.theme.SemiBold
import com.liezert.itungen.ui.theme.appText
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import com.liezert.itungen.ui.design.ItungTabs
import com.liezert.itungen.ui.design.ItungTab
import com.liezert.itungen.ui.design.ItungCard
import com.liezert.itungen.ui.design.ItungButton

private const val DayMillis = 86_400_000L

private val Periods = listOf(
    BudgetPeriod.MONTHLY to R.string.period_monthly,
    BudgetPeriod.WEEKLY to R.string.period_weekly,
    BudgetPeriod.UNTIL_DATE to R.string.period_until,
)

/** Teks per periode: (bulanan, mingguan, tak tentu). */
@Composable
private fun BudgetPeriod.pick(@StringRes monthly: Int, @StringRes weekly: Int, @StringRes until: Int, vararg args: Any) =
    stringResource(
        when (this) {
            BudgetPeriod.MONTHLY -> monthly
            BudgetPeriod.WEEKLY -> weekly
            BudgetPeriod.UNTIL_DATE -> until
        },
        *args,
    )

@Composable
fun dayName(day: Int): String = DayOfWeek.of(day.coerceIn(1, 7)).getDisplayName(TextStyle.FULL, dateLocale())

/**
 * Setup sekali untuk Jatah hari ini (DESIGN_SPEC §5): kapan uang masuk (bulanan, mingguan, atau tak tentu),
 * berapa, tagihan tetap, target tabungan. Pratinjau memakai mesin yang sama dengan Beranda, termasuk setoran target,
 * jadi angkanya selalu sama.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetScreen(onBack: () -> Unit) {
    val container = (LocalContext.current.applicationContext as ItungEnApp).container
    val current by container.budgetPlan.collectAsStateWithLifecycle()
    val goals by remember { container.goalRepository.observeGoals() }.collectAsStateWithLifecycle(emptyList())
    val expenseCategories by remember { container.transactionRepository.observeCategories(CategoryType.EXPENSE) }
        .collectAsStateWithLifecycle(emptyList())
    val limits by container.settings.categoryLimits.collectAsStateWithLifecycle()
    // Uuid (bukan Category) agar sheet batas tetap terbuka setelah rotasi.
    var limitForUuid by rememberSaveable { mutableStateOf<String?>(null) }
    val today = LocalDate.now(container.clock)

    val income = rememberTextFieldState(current?.income?.takeIf { it > 0 }?.toString().orEmpty())
    val bills = rememberTextFieldState(current?.fixedBills?.takeIf { it > 0 }?.toString().orEmpty())
    val savings = rememberTextFieldState(current?.savingsTarget?.takeIf { it > 0 }?.toString().orEmpty())
    var period by rememberSaveable { mutableStateOf(current?.period ?: BudgetPeriod.MONTHLY) }
    var monthDay by rememberSaveable {
        mutableIntStateOf(current?.takeIf { it.period == BudgetPeriod.MONTHLY }?.cycleStartDay ?: 25)
    }
    var weekDay by rememberSaveable {
        mutableIntStateOf(current?.takeIf { it.period == BudgetPeriod.WEEKLY }?.cycleStartDay ?: 1)
    }
    var nextIncome by rememberSaveable {
        mutableLongStateOf((current?.nextIncomeOn?.takeIf { it > today } ?: today.plusDays(7)).toEpochDay())
    }
    var picking by rememberSaveable { mutableStateOf(false) }
    /** Nominal baru saja dikonversi karena periode diganti: beri tahu agar diperiksa. */
    var converted by rememberSaveable { mutableStateOf(false) }

    val incomeValue = income.text.toString().toLongOrNull() ?: 0
    val billsValue = bills.text.toString().toLongOrNull() ?: 0
    val savingsValue = savings.text.toString().toLongOrNull() ?: 0
    val nextDate = LocalDate.ofEpochDay(nextIncome)
    val draft = BudgetPlan(
        income = incomeValue,
        cycleStartDay = if (period == BudgetPeriod.WEEKLY) weekDay else monthDay,
        fixedBills = billsValue,
        savingsTarget = savingsValue,
        period = period,
        // Kiriman tak tentu: siklus mulai hari ini, kecuali tanggal kiriman yang sama sedang diubah nominalnya.
        // Bulanan/mingguan: hari rencana mulai dipakai, agar siklus pertama yang dimulai di tengah jalan diprorata
        // (computeBudget). Rencana lama tanpa tanggal mulai tetap dihitung penuh.
        startedOn = if (period == BudgetPeriod.UNTIL_DATE) {
            current?.takeIf { it.period == BudgetPeriod.UNTIL_DATE && it.nextIncomeOn == nextDate }?.startedOn ?: today
        } else current?.takeIf { it.period == period }.let { same -> if (same == null) today else same.startedOn },
        nextIncomeOn = nextDate.takeIf { period == BudgetPeriod.UNTIL_DATE },
    )
    val goalsMonthly = goals.filter { it.status == GoalStatus.ACTIVE }.sumOf { it.plannedMonthly }
    // Hanya transaksi siklus rencana yang sedang diisi, sama seperti Beranda.
    val cycleStart = draft.cycleOf(today).start
    val transactions by remember(cycleStart) { container.transactionRepository.observeBetween(cycleStart, today) }
        .collectAsStateWithLifecycle(emptyList())
    val preview = computeBudget(draft, transactions, goalsMonthly, today)

    // Ganti periode: nominal ikut dikonversi (dulu 3.000.000/bulan langsung jadi 3.000.000/minggu).
    fun switchPeriod(next: BudgetPeriod) {
        if (next == period) return
        val from = periodDays(period, today, nextDate)
        val to = periodDays(next, today, nextDate)
        listOf(income, bills, savings).forEach { field ->
            field.text.toString().toLongOrNull()?.let { field.setTextAndPlaceCursorAtEnd(convertPerPeriod(it, from, to).toString()) }
        }
        converted = incomeValue > 0 || billsValue > 0 || savingsValue > 0
        period = next
    }

    // Kembali tanpa menekan Simpan: tanya dulu bila rencana berubah (dulu perubahan dibuang diam-diam).
    val dirty = if (current == null) incomeValue > 0 || billsValue > 0 || savingsValue > 0 else draft != current
    var confirmBack by rememberSaveable { mutableStateOf(false) }
    val back = { if (dirty) confirmBack = true else onBack() }
    BackHandler(enabled = dirty) { confirmBack = true }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).imePadding()) {
        TopBar(
            leading = { ItungIconButton(R.drawable.ic_back, back, stringResource(R.string.action_back)) },
            trailing = { Spacer(Modifier.size(48.dp)) },
        )
        Column(Modifier.screenPad(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(stringResource(R.string.budget_title), style = appText(20f, 28f, Bold, tracking = -0.5f))
            Text(
                stringResource(R.string.budget_intro),
                style = appText(14f, 20f, Regular, Ink.Gray),
            )
            Text(stringResource(R.string.income_when), Modifier.padding(start = 4.dp, top = 4.dp), style = appText(13f, 18f, Medium, Ink.Gray))
            ItungTabs(
                selectedTabIndex = { Periods.indexOfFirst { it.first == period } },
                onTabSelected = { switchPeriod(Periods[it].first) },
                tabsCount = Periods.size,
                modifier = Modifier.fillMaxWidth(),
                accentColor = Ink.Blue,
                height = 52.dp,
            ) {
                Periods.forEach { (value, label) ->
                    ItungTab({ switchPeriod(value) }, selected = value == period) {
                        SegmentLabel(stringResource(label), selected = value == period)
                    }
                }
            }
            if (converted) {
                Text(
                    period.pick(R.string.converted_monthly, R.string.converted_weekly, R.string.converted_until),
                    Modifier.padding(horizontal = 4.dp),
                    style = appText(12f, 16f, Medium, Ink.Orange),
                )
            }
            DateRow(
                title = period.pick(R.string.payday_title, R.string.allowance_day_title, R.string.next_transfer_title),
                value = when (period) {
                    BudgetPeriod.MONTHLY -> stringResource(R.string.payday_value, monthDay)
                    BudgetPeriod.WEEKLY -> stringResource(R.string.allowance_day_value, dayName(weekDay))
                    BudgetPeriod.UNTIL_DATE -> nextDate.formatIn("EEEE, d MMMM yyyy")
                },
                onClick = { picking = true },
            )
            AmountField(
                income,
                period.pick(R.string.income_monthly, R.string.income_weekly, R.string.income_until),
            )
            AmountField(bills, period.pick(R.string.bills_monthly, R.string.bills_weekly, R.string.bills_until))
            Text(
                stringResource(R.string.bills_hint),
                Modifier.padding(horizontal = 4.dp),
                style = appText(12f, 16f, Regular, Ink.Gray),
            )
            AmountField(savings, period.pick(R.string.savings_monthly, R.string.savings_weekly, R.string.savings_until))

            ItungCard(Modifier.fillMaxWidth(), padding = 17.dp, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(stringResource(R.string.today_allowance), style = appText(13f, 18f, Medium, Ink.Gray))
                Text(
                    when {
                        incomeValue == 0L -> "—"
                        preview.unrealistic -> stringResource(R.string.not_realistic)
                        else -> formatRupiah(preview.todayAllowance)
                    },
                    style = appText(22f, 28f, Bold, if (incomeValue > 0 && preview.unrealistic) Ink.Red else Ink.White),
                )
                Text(
                    if (incomeValue > 0 && preview.unrealistic) {
                        unrealisticMessage(preview)
                    } else {
                        listOfNotNull(
                            if (goalsMonthly > 0) stringResource(R.string.preview_same_with_goals, formatRupiah(goalsMonthly))
                            else stringResource(R.string.preview_same),
                            // Rencana baru di tengah siklus: uangnya diprorata sampai uang masuk berikutnya.
                            stringResource(R.string.preview_first_cycle).takeIf { preview.cycleStart > cycleStart },
                        ).joinToString(" ")
                    },
                    style = appText(13f, 18f, Regular, Ink.Gray),
                )
            }

            ItungButton(
                onClick = {
                    container.saveBudgetPlan(draft)
                    onBack()
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                tint = Ink.Blue.copy(alpha = if (incomeValue > 0) 1f else 0.5f),
                enabled = incomeValue > 0,
            ) { Text(stringResource(R.string.save_budget), style = appText(16f, 22f, Bold, Color.White)) }

            // Batas per kategori tersimpan langsung (tidak menunggu "Simpan anggaran").
            Text(stringResource(R.string.category_limits_title), Modifier.padding(start = 4.dp, top = 12.dp), style = appText(15f, 22f, SemiBold))
            Text(
                stringResource(R.string.category_limits_body),
                Modifier.padding(horizontal = 4.dp),
                style = appText(12f, 16f, Regular, Ink.Gray),
            )
            expenseCategories.forEach { category ->
                val limit = limits[category.uuid]
                DateRow(
                    title = category.displayName(),
                    value = if (limit != null) stringResource(R.string.per_month, formatRupiah(limit)) else stringResource(R.string.no_limit),
                    onClick = { limitForUuid = category.uuid },
                )
            }
        }
        Spacer(Modifier.height(bottomClearance()))
    }

    ItungAlertDialog(
        visible = confirmBack,
        title = stringResource(R.string.save_budget_title),
        text = stringResource(R.string.save_budget_body),
        confirmLabel = stringResource(R.string.action_save),
        onConfirm = {
            confirmBack = false
            if (incomeValue > 0) container.saveBudgetPlan(draft)
            onBack()
        },
        onDismiss = {
            confirmBack = false
            onBack()
        },
        dismissLabel = stringResource(R.string.discard_action),
        confirmColor = Ink.Blue,
    )

    val limitFor = limitForUuid?.let { uuid -> expenseCategories.firstOrNull { it.uuid == uuid } }
    CategoryLimitSheet(limitFor, limitFor?.let { limits[it.uuid] }, onSave = { limit ->
        limitFor?.let { container.settings.setCategoryLimit(it.uuid, limit) }
        limitForUuid = null
    }) { limitForUuid = null }

    when (period) {
        BudgetPeriod.MONTHLY -> ChoiceSheet(stringResource(R.string.payday_title), (1..28).map { it to it.toString() }, 7, monthDay, picking, {
            monthDay = it
            picking = false
        }) { picking = false }
        BudgetPeriod.WEEKLY -> ChoiceSheet(stringResource(R.string.allowance_day_title), (1..7).map { it to dayName(it).take(3) }, 7, weekDay, picking, {
            weekDay = it
            picking = false
        }) { picking = false }
        BudgetPeriod.UNTIL_DATE -> {
            val pickerState = rememberAppDatePickerState(nextDate) { it > today }
            ItungDatePickerDialog(
                visible = picking,
                state = pickerState,
                onConfirm = {
                    pickerState.selectedDateMillis?.let { nextIncome = it / DayMillis }
                    picking = false
                },
                onDismiss = { picking = false },
            )
        }
    }
}

@Composable
private fun DateRow(title: String, value: String, onClick: () -> Unit) {
    ItungCard(Modifier.fillMaxWidth(), padding = 13.dp, onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(Modifier.weight(1f)) {
                Text(title, style = appText(13f, 18f, Medium, Ink.Gray))
                Text(value, style = appText(16f, 22f, SemiBold))
            }
            Ico(R.drawable.ic_chevron_right)
        }
    }
}
