package com.liezert.itungen.ui.components

import com.liezert.itungen.domain.model.Transaction
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

private val Indonesia = Locale.forLanguageTag("id-ID")
private val ShortDate = DateTimeFormatter.ofPattern("d MMM yyyy", Indonesia)
private val Clock24 = DateTimeFormatter.ofPattern("HH:mm")

/** "Hari ini" / "Kemarin" / "3 Hari lalu" / "12 Sep 2026". */
fun relativeDay(date: LocalDate, today: LocalDate): String {
    val days = ChronoUnit.DAYS.between(date, today).toInt()
    return when {
        days == 0 -> "Hari ini"
        days == 1 -> "Kemarin"
        days in 2..6 -> "$days Hari lalu"
        else -> date.format(ShortDate)
    }
}

/** Judul grup di Semua Transaksi: "HARI INI" / "KEMARIN" / "12 SEP 2026". */
fun groupLabel(date: LocalDate, today: LocalDate): String = when (ChronoUnit.DAYS.between(date, today)) {
    0L -> "HARI INI"
    1L -> "KEMARIN"
    else -> date.format(ShortDate).uppercase(Indonesia)
}

/**
 * Jam pencatatan "12:30", hanya bila dicatat di hari yang sama dengan tanggal transaksi.
 * Transaksi yang tanggalnya dimundurkan (atau dibuat otomatis) tidak punya jam yang bermakna.
 */
fun Transaction.clockLabel(zone: ZoneId): String? {
    val at = createdAt.atZone(zone)
    return if (at.toLocalDate() == date) at.format(Clock24) else null
}
