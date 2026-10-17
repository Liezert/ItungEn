package com.liezert.itungen.ui.components

import android.content.Context
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.liezert.itungen.R
import com.liezert.itungen.domain.model.AmountKind
import com.liezert.itungen.domain.model.RupiahPrefix
import com.liezert.itungen.domain.model.amountText
import com.liezert.itungen.domain.model.formatRupiah
import com.liezert.itungen.domain.model.formatRupiahShort
import com.liezert.itungen.ui.theme.Ink

/** Pengganti nominal saat saldo disembunyikan. */
const val HiddenAmount = RupiahPrefix + "••••••"

/** Warna nominal: token pemasukan/pengeluaran; nominal netral (saldo, target, transfer) memakai warna teks biasa. */
fun AmountKind.color(neutral: Color): Color = when (this) {
    AmountKind.INCOME -> Ink.Income
    AmountKind.EXPENSE -> Ink.Expense
    AmountKind.NEUTRAL -> neutral
}

/**
 * Satu-satunya cara menampilkan nominal pemasukan/pengeluaran: tanpa tanda +/-, hijau/merah dari token tema, dan
 * dibacakan TalkBack beserta jenisnya ("Pengeluaran Rp50.000"). [arrow] = panah kecil (naik/turun) di daftar
 * transaksi agar jenisnya tidak hanya dibedakan warna. [hidden] = "Sembunyikan saldo".
 */
@Composable
fun AmountText(
    amount: Long,
    kind: AmountKind,
    style: TextStyle,
    modifier: Modifier = Modifier,
    hidden: Boolean = false,
    short: Boolean = false,
    arrow: Boolean = false,
) {
    val color = kind.color(style.color)
    val text = when {
        hidden -> HiddenAmount
        kind != AmountKind.NEUTRAL -> amountText(amount, short)
        short -> formatRupiahShort(amount)
        else -> formatRupiah(amount)
    }
    val description = if (hidden) {
        when (kind) {
            AmountKind.INCOME -> stringResource(R.string.amount_income_hidden)
            AmountKind.EXPENSE -> stringResource(R.string.amount_expense_hidden)
            AmountKind.NEUTRAL -> stringResource(R.string.amount_hidden)
        }
    } else {
        amountDescription(amount, kind)
    }
    Row(modifier.clearAndSetSemantics { contentDescription = description }, verticalAlignment = Alignment.CenterVertically) {
        if (arrow && kind != AmountKind.NEUTRAL) {
            Image(
                painterResource(if (kind == AmountKind.INCOME) R.drawable.ic_amount_up else R.drawable.ic_amount_down),
                contentDescription = null,
                modifier = Modifier.padding(end = 2.dp).size(14.dp),
                colorFilter = ColorFilter.tint(color),
            )
        }
        Text(text, style = style.copy(color = color), maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** Untuk TalkBack: "Pengeluaran Rp50.000" / "Expense Rp50.000"; nominal netral apa adanya (saldo minus bertanda). */
@Composable
fun amountDescription(amount: Long, kind: AmountKind): String = when (kind) {
    AmountKind.INCOME -> stringResource(R.string.amount_income, amountText(amount))
    AmountKind.EXPENSE -> stringResource(R.string.amount_expense, amountText(amount))
    AmountKind.NEUTRAL -> formatRupiah(amount)
}

/** [amountDescription] di luar komposisi (pesan, notifikasi). */
fun Context.amountDescription(amount: Long, kind: AmountKind): String = when (kind) {
    AmountKind.INCOME -> getString(R.string.amount_income, amountText(amount))
    AmountKind.EXPENSE -> getString(R.string.amount_expense, amountText(amount))
    AmountKind.NEUTRAL -> formatRupiah(amount)
}
