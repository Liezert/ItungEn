package com.liezert.itungen.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.Color
import com.liezert.itungen.R
import com.liezert.itungen.domain.model.Category
import com.liezert.itungen.domain.model.CategoryType
import com.liezert.itungen.domain.model.Transaction
import com.liezert.itungen.domain.model.TransactionType
import com.liezert.itungen.ui.theme.Ink

/**
 * Tampilan kategori. Warna dan ikon dipetakan dari kunci ikon di DB (CategoryEntity.icon), bukan dari kolom warna.
 *
 * @property rowIcon ikon di lingkaran 40 dp (Beranda, Semua Transaksi)
 * @property bigIcon ikon di lingkaran 48 dp (Insight)
 * @property chipIcon ikon kecil di chip Tambah Transaksi
 */
class CategoryStyle(
    val color: Color,
    @param:DrawableRes val rowIcon: Int,
    @param:DrawableRes val bigIcon: Int,
    @param:DrawableRes val chipIcon: Int,
)

// ponytail: Figma baru menggambar ikon Makanan/Transportasi/Hiburan/Lainnya; kategori lain memakai ikon generik
// dengan warna sendiri sampai ikonnya digambar.
private val Generic = { color: Color -> CategoryStyle(color, R.drawable.ic_cat_other, R.drawable.ic_cat_other, R.drawable.ic_add_other) }

private val ExpenseStyles = mapOf(
    "food" to CategoryStyle(Ink.Orange, R.drawable.ic_home_tx_food, R.drawable.ic_cat_food, R.drawable.ic_add_food),
    "transport" to CategoryStyle(Ink.Green, R.drawable.ic_home_tx_car, R.drawable.ic_cat_transport, R.drawable.ic_add_car),
    "entertainment" to CategoryStyle(Ink.Blue, R.drawable.ic_home_tx_music, R.drawable.ic_cat_entertainment, R.drawable.ic_add_film),
    "shopping" to CategoryStyle(Ink.Purple, R.drawable.ic_tx_bag, R.drawable.ic_tx_bag, R.drawable.ic_add_other),
    "bill" to Generic(Ink.Yellow),
    "health" to Generic(Ink.Pink),
    "education" to Generic(Ink.Cyan),
)
private val ExpenseOther = Generic(Ink.Gray)

private val IncomeStyles = mapOf(
    "salary" to CategoryStyle(Ink.Green, R.drawable.ic_tx_salary, R.drawable.ic_tx_salary, R.drawable.ic_inc_gaji),
    "gift" to CategoryStyle(Ink.Green, R.drawable.ic_home_tx_transfer, R.drawable.ic_home_tx_transfer, R.drawable.ic_inc_transfer),
    "bonus" to CategoryStyle(Ink.Green, R.drawable.ic_inc_bonus, R.drawable.ic_inc_bonus, R.drawable.ic_inc_bonus),
)
private val IncomeOther = CategoryStyle(Ink.Gray, R.drawable.ic_inc_other, R.drawable.ic_inc_other, R.drawable.ic_inc_other)

/** Transfer (ke/dari goal) tidak punya kategori. */
private val TransferStyle = CategoryStyle(Ink.Gray, R.drawable.ic_home_tx_transfer, R.drawable.ic_home_tx_transfer, R.drawable.ic_home_tx_transfer)

fun Category.style(): CategoryStyle =
    if (type == CategoryType.INCOME) IncomeStyles[icon] ?: IncomeOther else ExpenseStyles[icon] ?: ExpenseOther

fun Transaction.style(): CategoryStyle = category?.style() ?: TransferStyle

/** Nama kategori; "Transfer" hanya untuk transfer goal, bukan untuk pemasukan/pengeluaran yang kategorinya hilang. */
val Transaction.categoryLabel: String
    get() = category?.name ?: if (type == TransactionType.INCOME || type == TransactionType.EXPENSE) "Lainnya" else "Transfer"

val Transaction.incoming: Boolean get() = type == TransactionType.INCOME || type == TransactionType.TRANSFER_IN

/** Hijau untuk pemasukan, merah untuk pengeluaran, abu-abu untuk transfer (bukan pemasukan/pengeluaran, §3.2). */
val Transaction.amountColor: Color
    get() = when (type) {
        TransactionType.INCOME -> Ink.Green
        TransactionType.EXPENSE -> Ink.Red
        else -> Ink.Gray
    }
