package com.liezert.itungen.ui.budget

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
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDatePickerState
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
import com.liezert.glass.components.GlassCard
import com.liezert.glass.components.LiquidBottomTab
import com.liezert.glass.components.LiquidBottomTabs
import com.liezert.glass.components.LiquidButton
import com.liezert.glass.components.LocalGlassBackdrop
import com.liezert.itungen.ItungEnApp
import com.liezert.itungen.R
import com.liezert.itungen.domain.model.Category
import com.liezert.itungen.domain.model.CategoryType
import com.liezert.itungen.domain.model.GoalStatus
import com.liezert.itungen.domain.model.formatRupiah
import com.liezert.itungen.domain.usecase.BudgetPeriod
import com.liezert.itungen.domain.usecase.BudgetPlan
import com.liezert.itungen.domain.usecase.computeBudget
import com.liezert.itungen.domain.usecase.cycleOf
import com.liezert.itungen.ui.components.AmountField
import com.liezert.itungen.ui.insight.CategoryLimitSheet
import com.liezert.itungen.ui.components.ChoiceSheet
import com.liezert.itungen.ui.components.GlassIconButton
import com.liezert.itungen.ui.components.Ico
import com.liezert.itungen.ui.components.LiquidDatePickerDialog
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

private val Id = Locale.forLanguageTag("id-ID")
private val LongDate = DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", Id)
private const val DayMillis = 86_400_000L

private val Periods = listOf(
    BudgetPeriod.MONTHLY to "Bulanan",
    BudgetPeriod.WEEKLY to "Mingguan",
    BudgetPeriod.UNTIL_DATE to "Tak tentu",
)

/** "per bulan" / "per minggu" / "sampai kiriman berikutnya". */
private fun BudgetPeriod.per() = when (this) {
    BudgetPeriod.MONTHLY -> "per bulan"
    BudgetPeriod.WEEKLY -> "per minggu"
    BudgetPeriod.UNTIL_DATE -> "sampai kiriman berikutnya"
}

fun dayName(day: Int): String = DayOfWeek.of(day.coerceIn(1, 7)).getDisplayName(TextStyle.FULL, Id)

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
    var limitFor by remember { mutableStateOf<Category?>(null) }
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
        startedOn = if (period != BudgetPeriod.UNTIL_DATE) null
        else current?.takeIf { it.period == BudgetPeriod.UNTIL_DATE && it.nextIncomeOn == nextDate }?.startedOn ?: today,
        nextIncomeOn = nextDate.takeIf { period == BudgetPeriod.UNTIL_DATE },
    )
    val goalsMonthly = goals.filter { it.status == GoalStatus.ACTIVE }.sumOf { it.plannedMonthly }
    // Hanya transaksi siklus rencana yang sedang diisi, sama seperti Beranda.
    val cycleStart = draft.cycleOf(today).start
    val transactions by remember(cycleStart) { container.transactionRepository.observeBetween(cycleStart, today) }
        .collectAsStateWithLifecycle(emptyList())
    val preview = computeBudget(draft, transactions, goalsMonthly, today)

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).imePadding()) {
        TopBar(
            leading = { GlassIconButton(R.drawable.ic_back, onBack, "Kembali") },
            trailing = { Spacer(Modifier.size(48.dp)) },
        )
        Column(Modifier.screenPad(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("Anggaran", style = appText(20f, 28f, Bold, tracking = -0.5f))
            Text(
                "Diisi sekali. ItungEn membagi sisa uangmu per hari sampai uang masuk berikutnya.",
                style = appText(14f, 20f, Regular, Ink.Gray),
            )
            Text("Uang masuk kapan?", Modifier.padding(start = 4.dp, top = 4.dp), style = appText(13f, 18f, Medium, Ink.Gray))
            LiquidBottomTabs(
                selectedTabIndex = { Periods.indexOfFirst { it.first == period } },
                onTabSelected = { period = Periods[it].first },
                backdrop = LocalGlassBackdrop.current,
                tabsCount = Periods.size,
                modifier = Modifier.fillMaxWidth(),
                accentColor = Ink.Blue,
                height = 52.dp,
            ) {
                Periods.forEach { (value, label) ->
                    LiquidBottomTab({ period = value }, selected = value == period) {
                        Text(label, style = appText(14f, 20f, SemiBold, if (value == period) Ink.White else Ink.Muted))
                    }
                }
            }
            DateRow(
                title = when (period) {
                    BudgetPeriod.MONTHLY -> "Tanggal gajian / kiriman"
                    BudgetPeriod.WEEKLY -> "Hari uang saku"
                    BudgetPeriod.UNTIL_DATE -> "Kiriman berikutnya"
                },
                value = when (period) {
                    BudgetPeriod.MONTHLY -> "Tanggal $monthDay setiap bulan"
                    BudgetPeriod.WEEKLY -> "Setiap ${dayName(weekDay)}"
                    BudgetPeriod.UNTIL_DATE -> nextDate.format(LongDate)
                },
                onClick = { picking = true },
            )
            AmountField(
                income,
                when (period) {
                    BudgetPeriod.MONTHLY -> "Uang masuk per bulan (gaji, uang saku)"
                    BudgetPeriod.WEEKLY -> "Uang saku per minggu"
                    BudgetPeriod.UNTIL_DATE -> "Uang yang ada sampai kiriman berikutnya"
                },
            )
            AmountField(bills, "Tagihan tetap ${period.per()} (kos, listrik, cicilan)")
            Text(
                "Catat pembayaran tagihan dengan kategori Tagihan agar tidak mengurangi jatah harian.",
                Modifier.padding(horizontal = 4.dp),
                style = appText(12f, 16f, Regular, Ink.Gray),
            )
            AmountField(savings, "Target tabungan ${period.per()}")

            GlassCard(Modifier.fillMaxWidth(), padding = 17.dp, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Jatah hari ini", style = appText(13f, 18f, Medium, Ink.Gray))
                Text(
                    when {
                        incomeValue == 0L -> "—"
                        preview.unrealistic -> "Belum realistis"
                        else -> formatRupiah(preview.todayAllowance)
                    },
                    style = appText(22f, 28f, Bold, if (incomeValue > 0 && preview.unrealistic) Ink.Red else Ink.White),
                )
                Text(
                    if (incomeValue > 0 && preview.unrealistic) {
                        "Tagihan + tabungan + setoran target melebihi uang masuk. Kurangi target tabungan."
                    } else {
                        "Sama dengan Beranda: sudah memperhitungkan catatan siklus ini" +
                            if (goalsMonthly > 0) " dan setoran target ${formatRupiah(goalsMonthly)}/bulan." else "."
                    },
                    style = appText(13f, 18f, Regular, Ink.Gray),
                )
            }

            LiquidButton(
                onClick = {
                    container.saveBudgetPlan(draft)
                    onBack()
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                tint = Ink.Blue.copy(alpha = if (incomeValue > 0) 1f else 0.5f),
                enabled = incomeValue > 0,
            ) { Text("Simpan anggaran", style = appText(16f, 22f, Bold, Color.White)) }

            // Batas per kategori tersimpan langsung (tidak menunggu "Simpan anggaran").
            Text("Batas per kategori (opsional)", Modifier.padding(start = 4.dp, top = 12.dp), style = appText(15f, 22f, SemiBold))
            Text(
                "Diingatkan di Beranda dan Insight saat pengeluaran kategori mencapai 80% dan 100% batas bulan ini.",
                Modifier.padding(horizontal = 4.dp),
                style = appText(12f, 16f, Regular, Ink.Gray),
            )
            expenseCategories.forEach { category ->
                val limit = limits[category.uuid]
                DateRow(
                    title = category.name,
                    value = if (limit != null) "${formatRupiah(limit)} / bulan" else "Tanpa batas",
                    onClick = { limitFor = category },
                )
            }
        }
        Spacer(Modifier.height(bottomClearance()))
    }

    CategoryLimitSheet(limitFor, limitFor?.let { limits[it.uuid] }, onSave = { limit ->
        limitFor?.let { container.settings.setCategoryLimit(it.uuid, limit) }
        limitFor = null
    }) { limitFor = null }

    when (period) {
        BudgetPeriod.MONTHLY -> ChoiceSheet("Tanggal gajian / kiriman", (1..28).map { it to it.toString() }, 7, monthDay, picking, {
            monthDay = it
            picking = false
        }) { picking = false }
        BudgetPeriod.WEEKLY -> ChoiceSheet("Hari uang saku", (1..7).map { it to dayName(it).take(3) }, 7, weekDay, picking, {
            weekDay = it
            picking = false
        }) { picking = false }
        BudgetPeriod.UNTIL_DATE -> {
            val pickerState = rememberDatePickerState(
                initialSelectedDateMillis = nextIncome * DayMillis,
                selectableDates = object : SelectableDates {
                    override fun isSelectableDate(utcTimeMillis: Long) = utcTimeMillis / DayMillis > today.toEpochDay()
                },
            )
            LiquidDatePickerDialog(
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
    GlassCard(Modifier.fillMaxWidth(), padding = 13.dp, onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(Modifier.weight(1f)) {
                Text(title, style = appText(13f, 18f, Medium, Ink.Gray))
                Text(value, style = appText(16f, 22f, SemiBold))
            }
            Ico(R.drawable.ic_chevron_right)
        }
    }
}
