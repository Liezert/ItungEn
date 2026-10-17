package com.liezert.itungen.ui.insight

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.liezert.itungen.domain.model.CategoryType
import com.liezert.itungen.domain.model.formatRupiah
import com.liezert.itungen.domain.model.AmountKind
import com.liezert.itungen.ui.components.AmountText
import com.liezert.itungen.domain.model.Category
import com.liezert.itungen.domain.usecase.CategorySpend
import com.liezert.itungen.domain.usecase.LimitLevel
import com.liezert.itungen.domain.usecase.limitLevel
import com.liezert.itungen.domain.usecase.roundedPercents
import com.liezert.itungen.ui.components.DonutChart
import com.liezert.itungen.ui.components.DonutSlice
import com.liezert.itungen.ui.components.IconCircle
import com.liezert.itungen.ui.components.TopBar
import com.liezert.itungen.ui.components.bottomClearance
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
import java.util.Locale
import com.liezert.itungen.ui.design.ItungTabs
import com.liezert.itungen.ui.design.ItungTab
import com.liezert.itungen.ui.design.ItungSurface
import com.liezert.itungen.ui.design.ItungCard
import com.liezert.itungen.ui.design.ItungButton

private val MonthFormat = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.forLanguageTag("id-ID"))
private val MonthOnly = DateTimeFormatter.ofPattern("MMMM", Locale.forLanguageTag("id-ID"))

@Composable
fun InsightScreen(
    viewModel: InsightViewModel = viewModel(factory = InsightViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    InsightContent(
        state, viewModel::selectType, viewModel::previousMonth, viewModel::nextMonth,
        onSelectDay = viewModel::selectDay,
        onToggleNoSpend = viewModel::toggleNoSpend,
        onSetLimit = viewModel::setLimit,
    )
}

private val Kinds = listOf(CategoryType.EXPENSE to "Pengeluaran", CategoryType.INCOME to "Pemasukan")

/** Kata dan warna yang berbeda untuk pengeluaran (merah, "-") dan pemasukan (hijau, "+"). */
private val InsightUiState.income get() = type == CategoryType.INCOME
private val InsightUiState.noun get() = if (income) "pemasukan" else "pengeluaran"

@Composable
fun InsightContent(
    state: InsightUiState,
    onType: (CategoryType) -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
    onSelectDay: (LocalDate?) -> Unit = {},
    onToggleNoSpend: (LocalDate) -> Unit = {},
    /** uuid kategori, batas (null = hapus). */
    onSetLimit: (String, Long?) -> Unit = { _, _ -> },
) {
    val total = state.total
    var limitFor by remember { mutableStateOf<Category?>(null) }
    val lastMonth = state.month.minusMonths(1).format(MonthOnly)
    val accent = if (state.income) Ink.Income else Ink.Expense

    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        TopBar(
            leading = { Spacer(Modifier.size(48.dp)) }, // hamburger ditunda sampai punya isi (DESIGN_SPEC §11.2)
            trailing = { Spacer(Modifier.size(48.dp)) },
        )
        Spacer(Modifier.height(8.dp))
        Row(Modifier.screenPad().fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text("Insight", style = appText(20f, 25f, Bold, Ink.White, tracking = -0.5f))
                Text(
                    "${state.noun.replaceFirstChar { it.uppercase() }} ${state.month.format(MonthFormat)}",
                    style = appText(13f, 19.5f, Medium, Ink.Gray),
                )
            }
            MonthButton("‹", "Bulan sebelumnya", state.canGoBack, onPrevious)
            Spacer(Modifier.width(8.dp))
            MonthButton("›", "Bulan berikutnya", state.canGoForward, onNext)
        }
        Spacer(Modifier.height(16.dp))
        // Segmented control kaca, sama dengan toggle di Tambah Transaksi.
        ItungTabs(
            selectedTabIndex = { Kinds.indexOfFirst { it.first == state.type } },
            onTabSelected = { onType(Kinds[it].first) },
            tabsCount = Kinds.size,
            modifier = Modifier.screenPad().fillMaxWidth(),
            accentColor = accent,
            height = 52.dp,
        ) {
            Kinds.forEach { (type, label) ->
                ItungTab({ onType(type) }, selected = type == state.type) {
                    Text(label, style = appText(14f, 20f, SemiBold, if (type == state.type) Ink.White else Ink.Muted))
                }
            }
        }
        Spacer(Modifier.height(20.dp))
        SummaryCard(state, Modifier.screenPad())
        if (!state.income) {
            Spacer(Modifier.height(16.dp))
            SpendingCalendar(state, onSelectDay, onToggleNoSpend, Modifier.screenPad())
        }
        Spacer(Modifier.height(22.dp))
        Column(Modifier.screenPad(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("Detail Kategori", style = appText(15f, 22.5f, SemiBold, Ink.white(0.9f)))
            if (state.spending.isEmpty() && !state.loading) {
                Text("Belum ada ${state.noun} di bulan ini.", style = appText(14f, 20f, Regular, Ink.Gray))
            }
            if (!state.income && state.spending.isNotEmpty()) {
                Text("Ketuk kategori untuk mengatur batas bulanan.", style = appText(12f, 16f, Regular, Ink.Gray))
            }
            state.spending.forEach {
                // Batas hanya untuk pengeluaran.
                val limit = if (state.income) null else state.limits[it.category.uuid]
                BreakdownItem(
                    it, it.total.toFloat() / total, state.previous[it.category.id] ?: 0, lastMonth, state.income, limit,
                    onClick = if (state.income) null else ({ limitFor = it.category }),
                )
            }
        }
        Spacer(Modifier.height(bottomClearance()))
    }

    CategoryLimitSheet(limitFor, limitFor?.let { state.limits[it.uuid] }, onSave = { limit ->
        limitFor?.let { onSetLimit(it.uuid, limit) }
        limitFor = null
    }) { limitFor = null }
}

@Composable
private fun MonthButton(symbol: String, description: String, enabled: Boolean, onClick: () -> Unit) {
    ItungButton(
        onClick = onClick,
        modifier = Modifier.size(40.dp).semantics { contentDescription = description },
        enabled = enabled,
        contentPadding = 0.dp,
    ) { Text(symbol, style = appText(22f, 26f, SemiBold, if (enabled) Ink.White else Ink.Gray.copy(alpha = 0.5f))) }
}

/**
 * "Naik Rp 12.000 (35%) dibanding September". Pengeluaran naik = merah (buruk); pemasukan naik = hijau (baik).
 */
@Composable
private fun ComparisonText(
    current: Long,
    previous: Long,
    monthName: String,
    size: Float,
    income: Boolean,
    /** Ringkas untuk kartu kategori yang sempit: "▲ Rp 12.000 vs September". */
    compact: Boolean = false,
) {
    if (previous == 0L && current == 0L) return
    val diff = current - previous
    // Naik = baik untuk pemasukan, buruk untuk pengeluaran.
    val up = if (income) Ink.Income else Ink.Expense
    val down = if (income) Ink.Expense else Ink.Income
    // Persen dari pembanding yang sangat kecil (mis. Rp 1) tidak bermakna; tampilkan nominalnya saja.
    // Selisih kecil dibulatkan ke 0% terbaca "turun (0%)"; tampilkan "<1%".
    val pct = (kotlin.math.abs(diff) * 100 / maxOf(previous, 1)).takeIf { it <= 999 }?.let { if (it == 0L) " (<1%)" else " ($it%)" }.orEmpty()
    val (text, color) = when {
        previous == 0L -> (if (compact) "Baru bulan ini" else "Belum ada ${if (income) "pemasukan" else "pengeluaran"} di $monthName") to Ink.Gray
        diff == 0L -> "Sama dengan $monthName" to Ink.Gray
        compact -> (if (diff > 0) "▲ " else "▼ ") + "${formatRupiah(kotlin.math.abs(diff))} vs $monthName" to (if (diff > 0) up else down)
        diff > 0 -> "▲ Naik ${formatRupiah(diff)}$pct dibanding $monthName" to up
        else -> "▼ Turun ${formatRupiah(-diff)}$pct dibanding $monthName" to down
    }
    Text(text, style = appText(size, size + 5f, Medium, color))
}

@Composable
private fun SummaryCard(state: InsightUiState, modifier: Modifier = Modifier) {
    val spending = state.spending
    val total = state.total
    val accent = if (state.income) Ink.Income else Ink.Expense
    val kind = if (state.income) AmountKind.INCOME else AmountKind.EXPENSE
    ItungCard(
        modifier.fillMaxWidth(),
        tint = Brush.verticalGradient(listOf(accent.copy(alpha = if (total > 0) 0.14f else 0f), Color.Transparent)),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("TOTAL ${state.noun.uppercase()}", style = appText(14f, 20f, SemiBold, Ink.Gray, tracking = 0.7f))
            // Nol tetap warna teks biasa: belum ada pemasukan/pengeluaran untuk diwarnai.
            AmountText(total, if (total > 0) kind else AmountKind.NEUTRAL, appText(28f, 42f, Bold, Ink.White, tracking = -0.7f))
            ComparisonText(total, state.previousTotal, state.month.minusMonths(1).format(MonthOnly), 13f, state.income)
        }
        // Diagram lingkaran per kategori: jumlah kategori di tengah, legenda (warna, nama, persen) di sampingnya.
        Row(
            Modifier.fillMaxWidth().padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            DonutChart(
                slices = spending.map { DonutSlice(it.category.style().color, it.total.toFloat()) },
                track = Ink.white(0.1f),
                modifier = Modifier.size(132.dp),
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(spending.size.toString(), style = appText(22f, 26f, Bold))
                    Text("kategori", style = appText(11f, 14f, Medium, Ink.Gray))
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (spending.isEmpty()) {
                    Text("Belum ada ${state.noun}.", style = appText(13f, 18f, Regular, Ink.Gray))
                }
                // ponytail: 5 teratas + "Lainnya" agar legenda tetap muat di samping diagram.
                val top = spending.take(5)
                val rest = spending.drop(5).sumOf { it.total }
                val percents = roundedPercents(top.map { it.total } + listOfNotNull(rest.takeIf { it > 0 }))
                top.forEachIndexed { i, it -> LegendRow(it.category.style().color, it.category.name, percents[i]) }
                if (rest > 0) LegendRow(Ink.Gray, "Lainnya", percents.last())
            }
        }
    }
}

@Composable
private fun LegendRow(color: Color, label: String, percent: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(10.dp).clip(CircleShape).background(color))
        Text(label, Modifier.weight(1f), style = appText(13f, 18f, Medium, Ink.Muted), maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text("$percent%", style = appText(13f, 18f, SemiBold))
    }
}

@Composable
private fun BreakdownItem(
    item: CategorySpend,
    share: Float,
    previous: Long,
    lastMonth: String,
    income: Boolean,
    /** Batas bulanan kategori ini; null = tanpa batas. */
    limit: Long?,
    onClick: (() -> Unit)?,
) {
    val style = item.category.style()
    val level = limit?.let { limitLevel(item.total, it) }
    val limitColor = when (level) {
        LimitLevel.OVER -> Ink.Red
        LimitLevel.NEAR -> Ink.Orange
        else -> style.color
    }
    ItungSurface(Modifier.fillMaxWidth(), onClick = onClick) {
        Row(
            Modifier.fillMaxWidth().padding(start = 17.dp, end = 17.dp, top = 17.dp, bottom = 29.dp),
            Arrangement.SpaceBetween,
            Alignment.CenterVertically,
        ) {
            Row(
                Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconCircle(style.bigIcon, 48.dp, style.color.copy(alpha = 0.15f))
                Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                    Text(item.category.name, style = appText(16f, 24f, SemiBold), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("${item.count} transaksi", style = appText(13f, 19.5f, Regular, Ink.Gray))
                    ComparisonText(item.total, previous, lastMonth, 12f, income, compact = true)
                    if (limit != null) {
                        Text(
                            when (level) {
                                LimitLevel.OVER -> "Lewat batas ${formatRupiah(limit)}"
                                else -> "${item.total * 100 / limit}% dari batas ${formatRupiah(limit)}"
                            },
                            style = appText(12f, 16f, SemiBold, if (level == LimitLevel.OK) Ink.Gray else limitColor),
                        )
                    }
                }
            }
            AmountText(
                item.total,
                if (income) AmountKind.INCOME else AmountKind.EXPENSE,
                appText(16f, 24f, Bold),
                Modifier.padding(start = 12.dp),
            )
        }
        // Garis progres tipis di tepi bawah kartu: terhadap batas bila ada, selain itu porsi dari total bulan ini.
        val progress = if (limit != null) (item.total.toFloat() / limit).coerceIn(0f, 1f) else share
        Box(Modifier.align(Alignment.BottomStart).fillMaxWidth().height(4.dp).background(limitColor.copy(alpha = 0.2f))) {
            Box(Modifier.fillMaxWidth(progress).fillMaxHeight().background(limitColor))
        }
    }
}
