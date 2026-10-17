package com.liezert.itungen.ui.insight

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.liezert.glass.components.GlassCard
import com.liezert.glass.components.LiquidButton
import com.liezert.glass.components.LiquidSheet
import com.liezert.itungen.domain.model.Category
import com.liezert.itungen.domain.model.formatRupiah
import com.liezert.itungen.domain.usecase.heatLevel
import com.liezert.itungen.ui.components.AmountField
import com.liezert.itungen.ui.theme.Bold
import com.liezert.itungen.ui.theme.Ink
import com.liezert.itungen.ui.theme.Medium
import com.liezert.itungen.ui.theme.Regular
import com.liezert.itungen.ui.theme.SemiBold
import com.liezert.itungen.ui.theme.appText
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import com.liezert.itungen.domain.usecase.DayTone
import com.liezert.itungen.domain.usecase.canMarkNoSpend
import com.liezert.itungen.domain.usecase.dayTone
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

private val Id = Locale.forLanguageTag("id-ID")
private val DayFormat = DateTimeFormatter.ofPattern("EEEE, d MMMM", Id)
private val Weekdays = DayOfWeek.entries.map { it.getDisplayName(TextStyle.SHORT, Id) }

/** Pekat warna netral per tingkat 1–4 (tanpa anggaran). */
private val HeatAlpha = listOf(0f, 0.18f, 0.32f, 0.5f, 0.72f)

/**
 * Kalender pengeluaran sebulan (heatmap tanpa library). Dengan anggaran, warna dibanding jatah harian: abu-abu ≤ separuh,
 * oranye ≤ jatah, merah lewat jatah (dulu dibanding hari terbesar bulan itu, jadi satu belanja kecil pun merah penuh).
 * Tanpa anggaran: abu-abu makin pekat = makin besar. Hijau = ditandai tidak jajan. Di atasnya streak hari tercatat
 * berturut-turut (hanya di bulan ini). Ketuk satu hari untuk melihat totalnya.
 */
@Composable
fun SpendingCalendar(
    state: InsightUiState,
    onSelect: (LocalDate?) -> Unit,
    onToggleNoSpend: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    val month = state.month
    val max = state.daily.values.maxOrNull() ?: 0
    val noSpendThisMonth = state.noSpendDays.count { it.year == month.year && it.month == month.month && (state.daily[it] ?: 0) == 0L }
    GlassCard(modifier.fillMaxWidth(), padding = 17.dp, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
            Text("Kalender", style = appText(15f, 22.5f, SemiBold, Ink.white(0.9f)))
            // Streak adalah keadaan sekarang: di bulan lalu terkesan milik bulan itu.
            if (month == YearMonth.from(state.today)) {
                Text(
                    if (state.streak > 0) "Tercatat ${state.streak} hari berturut-turut" else "Belum ada streak",
                    style = appText(13f, 18f, SemiBold, if (state.streak > 0) Ink.Green else Ink.Gray),
                )
            }
        }
        Text(
            "$noSpendThisMonth hari tanpa jajan bulan ini. " +
                (state.dailyAllowance?.let { "Merah = lewat jatah harian ${formatRupiah(it)}. " } ?: "") +
                "Ketuk hari untuk detail.",
            style = appText(12f, 16f, Regular, Ink.Gray),
        )
        Row(Modifier.fillMaxWidth()) {
            Weekdays.forEach {
                Text(it, Modifier.weight(1f), style = appText(11f, 14f, Medium, Ink.Gray), textAlign = TextAlign.Center)
            }
        }
        // Senin = kolom pertama (§5.2 minggu mulai Senin).
        val leading = month.atDay(1).dayOfWeek.value - 1
        val cells = List(leading) { null } + (1..month.lengthOfMonth()).map { month.atDay(it) }
        cells.chunked(7).forEach { week ->
            Row(Modifier.fillMaxWidth()) {
                week.forEach { day ->
                    if (day == null) Spacer(Modifier.weight(1f)) else {
                        DayCell(day, state, max, Modifier.weight(1f)) { onSelect(if (state.selectedDay == day) null else day) }
                    }
                }
                repeat(7 - week.size) { Spacer(Modifier.weight(1f)) }
            }
        }
        state.selectedDay?.let { DayDetail(it, state, onToggleNoSpend) }
    }
}

@Composable
private fun DayCell(day: LocalDate, state: InsightUiState, max: Long, modifier: Modifier, onClick: () -> Unit) {
    val amount = state.daily[day] ?: 0
    val future = day > state.today
    val noSpend = amount == 0L && day in state.noSpendDays
    val fill = if (noSpend) Ink.Green.copy(alpha = 0.35f) else when (dayTone(amount, state.dailyAllowance)) {
        DayTone.NONE -> Ink.white(0.05f)
        DayTone.LOW -> Ink.white(0.18f)
        DayTone.NEAR -> Ink.Orange.copy(alpha = 0.45f)
        DayTone.OVER -> Ink.Red.copy(alpha = 0.72f)
        DayTone.NEUTRAL -> Ink.white(HeatAlpha[heatLevel(amount, max)])
    }
    val selected = day == state.selectedDay
    val description = day.format(DayFormat) + ", " + when {
        future -> "belum terjadi"
        amount > 0 -> formatRupiah(amount)
        noSpend -> "tidak jajan"
        else -> "tidak ada pengeluaran"
    }
    Box(
        modifier
            .aspectRatio(1f)
            .padding(2.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(fill)
            .then(
                when {
                    selected -> Modifier.border(1.5.dp, Ink.White, RoundedCornerShape(10.dp))
                    day == state.today -> Modifier.border(1.dp, Ink.Blue, RoundedCornerShape(10.dp))
                    else -> Modifier
                }
            )
            .clickable(enabled = !future, role = Role.Button, onClick = onClick)
            .semantics {
                contentDescription = description
                this.selected = selected
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            day.dayOfMonth.toString(),
            style = appText(13f, 16f, if (selected) Bold else Medium, if (future) Ink.Gray.copy(alpha = 0.5f) else Ink.White),
        )
    }
}

@Composable
private fun DayDetail(day: LocalDate, state: InsightUiState, onToggleNoSpend: (LocalDate) -> Unit) {
    val amount = state.daily[day] ?: 0
    val marked = day in state.noSpendDays
    Row(Modifier.fillMaxWidth().padding(top = 4.dp), Arrangement.spacedBy(12.dp), Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(day.format(DayFormat), style = appText(14f, 20f, SemiBold))
            Text(
                when {
                    amount > 0 -> "Pengeluaran ${formatRupiah(amount)}"
                    marked -> "Ditandai tidak jajan"
                    else -> "Tidak ada pengeluaran"
                },
                style = appText(13f, 18f, Regular, if (amount > 0) Ink.Red else if (marked) Ink.Green else Ink.Gray),
            )
        }
        // Hanya hari tanpa pengeluaran yang bisa ditandai, dan hanya hari ini/kemarin (streak tidak bisa digelembungkan
        // dengan menandai hari lama). Tanda yang sudah ada tetap bisa dibatalkan.
        if (amount == 0L && (marked || canMarkNoSpend(day, state.today))) {
            LiquidButton(onClick = { onToggleNoSpend(day) }, modifier = Modifier.height(40.dp), contentPadding = 14.dp) {
                Text(if (marked) "Batalkan tanda" else "Tandai tidak jajan", style = appText(13f, 18f, SemiBold, Ink.Green))
            }
        }
    }
}

/**
 * Sheet batas bulanan satu kategori. [category] null = tertutup (kategori terakhir tetap tampil selama animasi keluar).
 * Simpan dengan nominal kosong/0 = hapus batas.
 */
@Composable
fun CategoryLimitSheet(category: Category?, current: Long?, onSave: (Long?) -> Unit, onDismiss: () -> Unit) {
    var shown by remember { mutableStateOf(category) }
    if (category != null) shown = category
    val target = shown ?: return
    LiquidSheet(visible = category != null, onDismiss = onDismiss) {
        key(target.id) {
            val amount = rememberTextFieldState(current?.toString().orEmpty())
            Column(Modifier.padding(start = 20.dp, end = 20.dp, bottom = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Batas ${target.name} per bulan", style = appText(18f, 24f, Bold))
                Text(
                    "Peringatan muncul di Beranda dan Insight saat pengeluaran kategori ini mencapai 80% dan 100% batas.",
                    style = appText(13f, 18f, Regular, Ink.Gray),
                )
                AmountField(amount, "Batas per bulan")
                Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(12.dp)) {
                    if (current != null) {
                        LiquidButton(onClick = { onSave(null) }, modifier = Modifier.weight(1f)) {
                            Text("Hapus batas", style = appText(15f, 20f, SemiBold, Ink.Red))
                        }
                    }
                    LiquidButton(
                        onClick = { onSave(amount.text.toString().toLongOrNull()) },
                        modifier = Modifier.weight(1f),
                        tint = Ink.Blue,
                    ) { Text("Simpan", style = appText(15f, 20f, SemiBold, Color.White)) }
                }
            }
        }
    }
}
