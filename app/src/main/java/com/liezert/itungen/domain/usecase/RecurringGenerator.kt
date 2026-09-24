package com.liezert.itungen.domain.usecase

import java.time.LocalDate
import java.time.YearMonth

/**
 * Tanggal jatuh tempo aturan bulanan yang belum dicatat, urut naik, sampai [today] (inklusif).
 *
 * - Mulai setelah [lastGenerated], atau dari [startDate] bila belum pernah dicatat, jadi semua bulan
 *   yang terlewat ikut tercatat (catch-up).
 * - [dayOfMonth] yang tidak ada di bulan itu (mis. 31 di April) jatuh ke hari terakhir bulan (§3.2).
 * - Berhenti di [endDate] bila ada.
 */
fun recurringDueDates(
    dayOfMonth: Int,
    startDate: LocalDate,
    endDate: LocalDate?,
    lastGenerated: LocalDate?,
    today: LocalDate,
): List<LocalDate> {
    val until = if (endDate != null && endDate < today) endDate else today
    val dates = mutableListOf<LocalDate>()
    var month = YearMonth.from(lastGenerated ?: startDate)
    while (true) {
        val date = month.atDay(minOf(dayOfMonth, month.lengthOfMonth()))
        if (date > until) break
        if (date >= startDate && (lastGenerated == null || date > lastGenerated)) dates += date
        month = month.plusMonths(1)
    }
    return dates
}
