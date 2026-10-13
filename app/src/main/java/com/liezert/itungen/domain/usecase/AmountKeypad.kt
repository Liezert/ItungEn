package com.liezert.itungen.domain.usecase

import com.liezert.itungen.domain.model.MAX_AMOUNT
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Ekspresi dari keypad nominal, mis. "15000+2000×3": angka dan simbol operator tanpa spasi.
 * Semua fungsi murni (string masuk, string keluar) agar mudah diuji dan disimpan di rememberSaveable.
 */
const val OPERATORS = "+-×÷"

private val MaxDigits = MAX_AMOUNT.toString().length

/** Tambah "0".."9" atau "000". Tanpa nol di depan angka dan maksimal 12 digit per angka (§7.1). */
fun keypadDigit(expr: String, digits: String): String {
    val current = expr.takeLastWhile(Char::isDigit)
    if (current.isEmpty() && digits.startsWith('0')) return expr
    if (current.length + digits.length > MaxDigits) return expr
    return expr + digits
}

/** Operator setelah operator menggantikannya; operator di awal diabaikan. */
fun keypadOperator(expr: String, op: Char): String = when {
    expr.isEmpty() -> expr
    expr.last() in OPERATORS -> expr.dropLast(1) + op
    else -> expr + op
}

fun hasOperator(expr: String) = expr.any { it in OPERATORS }

/**
 * Hasil ekspresi dibulatkan ke rupiah (× ÷ sebelum + −). Operator di akhir diabaikan.
 * null bila kosong atau dibagi nol; hasil di luar rentang Long dijepit (validasi menolak sebagai terlalu besar / ≤ 0).
 */
fun evaluateKeypad(expr: String): Long? {
    val body = expr.trimEnd { it in OPERATORS }
    if (body.isEmpty()) return null
    val numbers = body.split(*OPERATORS.toCharArray()).map(::BigDecimal)
    val ops = body.filter { it in OPERATORS }

    // Lintasan 1: × dan ÷ dilipat ke suku sebelumnya.
    val terms = mutableListOf(numbers[0])
    val addOps = StringBuilder()
    ops.forEachIndexed { i, op ->
        val n = numbers[i + 1]
        when (op) {
            '×' -> terms[terms.lastIndex] = terms.last() * n
            '÷' -> {
                if (n.signum() == 0) return null
                terms[terms.lastIndex] = terms.last().divide(n, 10, RoundingMode.HALF_UP)
            }
            else -> {
                terms += n
                addOps.append(op)
            }
        }
    }
    // Lintasan 2: + dan −.
    var result = terms[0]
    addOps.forEachIndexed { i, op -> result = if (op == '+') result + terms[i + 1] else result - terms[i + 1] }
    return result.setScale(0, RoundingMode.HALF_UP)
        .max(BigDecimal.valueOf(Long.MIN_VALUE))
        .min(BigDecimal.valueOf(Long.MAX_VALUE))
        .toLong()
}

/** "15000+2000" → "15.000 + 2.000". */
fun formatKeypad(expr: String): String = buildString {
    var number = StringBuilder()
    fun flush() {
        append(number.reversed().chunked(3).joinToString(".").reversed())
        number = StringBuilder()
    }
    expr.forEach { c ->
        if (c.isDigit()) number.append(c) else {
            flush()
            append(' ').append(c).append(' ')
        }
    }
    flush()
}
