package com.liezert.itungen.domain.model

import java.util.Locale
import kotlin.math.absoluteValue

/** Batas atas wajar per transaksi (< 1 triliun), jauh dari batas `Long` (§7.1). */
const val MAX_AMOUNT = 999_999_999_999L

/** Batas panjang input; firestore.rules memakai batas yang lebih longgar (500 / 100) untuk data lama. */
const val MAX_NOTE = 100
const val MAX_TITLE = 60

private val Indonesia = Locale.forLanguageTag("id-ID")

/** "Rp" + spasi tak-putus: nominal tidak pernah patah baris menjadi "Rp⏎30.000". */
const val RupiahPrefix = "Rp\u00A0"

/** 1500000 → "Rp 1.500.000", -50000 → "-Rp 50.000". */
fun formatRupiah(amount: Long): String =
    (if (amount < 0) "-" else "") + RupiahPrefix + "%,d".format(Indonesia, amount.absoluteValue)

/** "+Rp 500.000" / "-Rp 18.000": tanda ditentukan arah uang, bukan tanda nominal. */
fun formatSigned(amount: Long, incoming: Boolean): String = (if (incoming) "+" else "-") + formatRupiah(amount)

/** Ringkas untuk ruang sempit: 1250000 → "Rp 1.25M", 3000000 → "Rp 3M", 250000 → "Rp 250.000". */
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
