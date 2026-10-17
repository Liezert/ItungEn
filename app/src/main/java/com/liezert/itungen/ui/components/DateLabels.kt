package com.liezert.itungen.ui.components

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.liezert.itungen.R
import com.liezert.itungen.domain.model.Transaction
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

private val Clock24 = DateTimeFormatter.ofPattern("HH:mm")

/**
 * Locale nama hari/bulan = bahasa teks yang sedang dipakai app (resource date_locale), bukan bahasa sistem: dengan
 * "Ikuti sistem" pada HP berbahasa yang tidak didukung, teks jatuh ke Indonesia dan tanggalnya ikut Indonesia.
 */
fun Context.dateLocale(): Locale = Locale.forLanguageTag(getString(R.string.date_locale))

@Composable
fun dateLocale(): Locale = Locale.forLanguageTag(stringResource(R.string.date_locale))

/** Format tanggal dengan [pattern] dalam bahasa app, mis. "d MMM yyyy" → "12 Sep 2026" / "12 Sept 2026". */
@Composable
fun LocalDate.formatIn(pattern: String): String = format(DateTimeFormatter.ofPattern(pattern, dateLocale()))

/** "Hari ini" / "Kemarin" / "3 hari lalu" / "12 Sep 2026". */
@Composable
fun relativeDay(date: LocalDate, today: LocalDate): String {
    val days = ChronoUnit.DAYS.between(date, today).toInt()
    return when {
        days == 0 -> stringResource(R.string.day_today)
        days == 1 -> stringResource(R.string.day_yesterday)
        days in 2..6 -> pluralStringResource(R.plurals.days_ago, days, days)
        else -> date.formatIn("d MMM yyyy")
    }
}

/** Judul grup di Semua Transaksi: "HARI INI" / "KEMARIN" / "12 SEP 2026". */
@Composable
fun groupLabel(date: LocalDate, today: LocalDate): String = when (ChronoUnit.DAYS.between(date, today)) {
    0L -> stringResource(R.string.day_today)
    1L -> stringResource(R.string.day_yesterday)
    else -> date.formatIn("d MMM yyyy")
}.uppercase(dateLocale())

/**
 * Jam pencatatan "12:30", hanya bila dicatat di hari yang sama dengan tanggal transaksi.
 * Transaksi yang tanggalnya dimundurkan (atau dibuat otomatis) tidak punya jam yang bermakna.
 */
fun Transaction.clockLabel(zone: ZoneId): String? {
    val at = createdAt.atZone(zone)
    return if (at.toLocalDate() == date) at.format(Clock24) else null
}
