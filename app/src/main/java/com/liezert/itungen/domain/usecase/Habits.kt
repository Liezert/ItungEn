package com.liezert.itungen.domain.usecase

import java.time.LocalDate

/** Posisi pengeluaran satu kategori terhadap batas bulanannya. */
enum class LimitLevel { OK, NEAR, OVER }

/** Peringatan muncul di 80% (hampir) dan 100% (lewat). */
fun limitLevel(spent: Long, limit: Long): LimitLevel = when {
    limit <= 0 -> LimitLevel.OK
    spent >= limit -> LimitLevel.OVER
    spent * 100 >= limit * 80 -> LimitLevel.NEAR
    else -> LimitLevel.OK
}

data class LimitWarning(val spend: CategorySpend, val limit: Long, val level: LimitLevel)

/**
 * Kategori yang sudah ≥ 80% dari batasnya bulan ini, yang lewat batas dulu lalu yang paling dekat batas.
 * [limits] = uuid kategori → batas.
 */
fun limitWarnings(spending: List<CategorySpend>, limits: Map<String, Long>): List<LimitWarning> =
    spending.mapNotNull { spend ->
        val limit = limits[spend.category.uuid] ?: return@mapNotNull null
        LimitWarning(spend, limit, limitLevel(spend.total, limit)).takeIf { it.level != LimitLevel.OK }
    }.sortedWith(compareByDescending<LimitWarning> { it.level }.thenByDescending { it.spend.total.toDouble() / it.limit })

/**
 * Hari tercatat berturut-turut sampai hari ini: hari yang punya transaksi atau ditandai tidak jajan ([recorded]).
 * Hari ini yang belum dicatat tidak memutus streak (harinya belum selesai), jadi hitungan mulai dari kemarin.
 */
fun recordStreak(recorded: Set<LocalDate>, today: LocalDate): Int {
    var day = if (today in recorded) today else today.minusDays(1)
    var streak = 0
    while (day in recorded) {
        streak++
        day = day.minusDays(1)
    }
    return streak
}

/**
 * Tingkat warna heatmap 0–4: 0 = tidak ada pengeluaran, 1–4 = seperempat-an dari pengeluaran harian terbesar
 * bulan itu ([max]).
 */
fun heatLevel(amount: Long, max: Long): Int = when {
    amount <= 0 || max <= 0 -> 0
    else -> ((amount * 4 + max - 1) / max).toInt().coerceIn(1, 4)
}
