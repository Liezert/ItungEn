package com.liezert.itungen.domain.model

import java.util.Locale
import kotlin.math.absoluteValue

/** Batas atas wajar per transaksi (< 1 triliun), jauh dari batas `Long` (§7.1). */
const val MAX_AMOUNT = 999_999_999_999L

private val Indonesia = Locale.forLanguageTag("id-ID")

/** 1500000 → "Rp 1.500.000", -50000 → "-Rp 50.000". */
fun formatRupiah(amount: Long): String =
    (if (amount < 0) "-" else "") + "Rp " + "%,d".format(Indonesia, amount.absoluteValue)
