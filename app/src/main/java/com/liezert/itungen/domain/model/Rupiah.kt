package com.liezert.itungen.domain.model

import java.util.Locale
import kotlin.math.absoluteValue

/** Batas atas wajar per transaksi (< 1 triliun), jauh dari batas `Long` (§7.1). */
const val MAX_AMOUNT = 999_999_999_999L

/** Batas panjang input; firestore.rules memakai batas yang lebih longgar (500 / 100) untuk data lama. */
const val MAX_NOTE = 100
const val MAX_TITLE = 60

private val Indonesia = Locale.forLanguageTag("id-ID")

/** Ejaan PUEBI: "Rp50.000" tanpa spasi, jadi nominal juga tidak bisa patah baris menjadi "Rp⏎30.000". */
const val RupiahPrefix = "Rp"

/** 1500000 → "Rp1.500.000", -50000 → "-Rp50.000" (nominal netral seperti saldo boleh minus). */
fun formatRupiah(amount: Long): String =
    (if (amount < 0) "-" else "") + RupiahPrefix + "%,d".format(Indonesia, amount.absoluteValue)

/** Jenis nominal yang ditampilkan: pemasukan (hijau), pengeluaran (merah), atau netral (saldo, target, transfer). */
enum class AmountKind { INCOME, EXPENSE, NEUTRAL }

/**
 * Teks nominal pemasukan/pengeluaran: tanpa tanda +/- (arahnya ditunjukkan warna, ikon, dan deskripsi TalkBack).
 * [short] = ringkas untuk ruang sempit ("Rp1.25M").
 */
fun amountText(amount: Long, short: Boolean = false): String =
    if (short) formatRupiahShort(amount.absoluteValue) else formatRupiah(amount.absoluteValue)

/** Ringkas untuk ruang sempit: 1250000 → "Rp1.25M", 3000000 → "Rp3M", 250000 → "Rp250.000". */
fun formatRupiahShort(amount: Long): String {
    val abs = amount.absoluteValue
    val (divisor, suffix) = when {
        abs >= 1_000_000_000L -> 1_000_000_000.0 to "B"
        abs >= 1_000_000L -> 1_000_000.0 to "M"
        else -> return formatRupiah(amount)
    }
    val value = "%.2f".format(Locale.US, abs / divisor).trimEnd('0').trimEnd('.')
    return (if (amount < 0) "-" else "") + RupiahPrefix + value + suffix
}

private val Plain = Regex("""^(\d{1,3}(?:\.\d{3})+|\d+)(,\d+)?$""")
private val WithUnit = Regex("""^\d+([.,]\d+)?$""")
private val Units = listOf("ribu" to 1_000L, "rb" to 1_000L, "k" to 1_000L, "juta" to 1_000_000L, "jt" to 1_000_000L)

/**
 * Nominal ketikan bebas (mis. di notifikasi catat cepat): "50000", "50.000", "50rb", "50 ribu", "50k", "1,5jt",
 * "Rp 50.000". Tanpa satuan, titik = pemisah ribuan dan koma = desimal; dengan satuan, koma atau titik = desimal.
 * Hasil harus rupiah bulat 1..[MAX_AMOUNT]; selain itu null (tidak ditebak).
 */
fun parseRupiah(input: String): Long? {
    var text = input.lowercase().filterNot { it.isWhitespace() || it == '\u00A0' }.removePrefix("rp")
    val multiplier = Units.firstOrNull { (unit, _) -> text.endsWith(unit) }?.let { (unit, factor) ->
        text = text.removeSuffix(unit)
        factor
    } ?: 1L
    val value = if (multiplier == 1L) {
        val match = Plain.matchEntire(text) ?: return null
        java.math.BigDecimal(match.groupValues[1].replace(".", "") + match.groupValues[2].replace(',', '.'))
    } else {
        if (!WithUnit.matches(text)) return null
        java.math.BigDecimal(text.replace(',', '.')).multiply(java.math.BigDecimal.valueOf(multiplier))
    }.stripTrailingZeros()
    if (value.scale() > 0 || value > java.math.BigDecimal.valueOf(MAX_AMOUNT)) return null
    return value.toLong().takeIf { it >= 1 }
}
