package com.liezert.itungen.ui.components

import com.liezert.itungen.data.local.defaultCategoryUuid
import com.liezert.itungen.data.local.DefaultCategories
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.Color
import com.liezert.itungen.R
import com.liezert.itungen.domain.model.Category
import com.liezert.itungen.domain.model.CategoryType
import com.liezert.itungen.domain.model.Transaction
import com.liezert.itungen.domain.model.TransactionType
import com.liezert.itungen.domain.model.AmountKind
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

// Ikon Makanan/Transportasi/Hiburan/Lainnya dari Figma; Tagihan/Kesehatan/Pendidikan dari Material Icons.
private val ExpenseStyles = mapOf(
    "food" to CategoryStyle(Ink.Orange, R.drawable.ic_home_tx_food, R.drawable.ic_cat_food, R.drawable.ic_add_food),
    "transport" to CategoryStyle(Ink.Green, R.drawable.ic_home_tx_car, R.drawable.ic_cat_transport, R.drawable.ic_add_car),
    "entertainment" to CategoryStyle(Ink.Blue, R.drawable.ic_home_tx_music, R.drawable.ic_cat_entertainment, R.drawable.ic_add_film),
    "shopping" to CategoryStyle(Ink.Purple, R.drawable.ic_tx_bag, R.drawable.ic_tx_bag, R.drawable.ic_tx_bag),
    "bill" to CategoryStyle(Ink.Yellow, R.drawable.ic_cat_bill, R.drawable.ic_cat_bill, R.drawable.ic_add_bill),
    "health" to CategoryStyle(Ink.Pink, R.drawable.ic_cat_health, R.drawable.ic_cat_health, R.drawable.ic_add_health),
    "education" to CategoryStyle(Ink.Cyan, R.drawable.ic_cat_education, R.drawable.ic_cat_education, R.drawable.ic_add_education),
)
private val ExpenseOther = CategoryStyle(Ink.Gray, R.drawable.ic_cat_other, R.drawable.ic_cat_other, R.drawable.ic_add_other)

private val IncomeStyles = mapOf(
    "salary" to CategoryStyle(Ink.Green, R.drawable.ic_tx_salary, R.drawable.ic_tx_salary, R.drawable.ic_inc_gaji),
    "gift" to CategoryStyle(Ink.Green, R.drawable.ic_home_tx_transfer, R.drawable.ic_home_tx_transfer, R.drawable.ic_inc_transfer),
    "bonus" to CategoryStyle(Ink.Green, R.drawable.ic_inc_bonus, R.drawable.ic_inc_bonus, R.drawable.ic_inc_bonus),
)
private val IncomeOther = CategoryStyle(Ink.Gray, R.drawable.ic_inc_other, R.drawable.ic_inc_other, R.drawable.ic_inc_other)

/** Transfer (ke/dari goal) tidak punya kategori. Uang masuk kembali dari goal = panah masuk hijau. */
private val TransferStyle = CategoryStyle(Ink.Gray, R.drawable.ic_home_tx_transfer, R.drawable.ic_home_tx_transfer, R.drawable.ic_home_tx_transfer)

/** Setoran ke goal: uang keluar dari saldo utama, jadi ikon target (dulu panah masuk hijau untuk "-Rp 450.000"). */
private val TransferOutStyle = CategoryStyle(Ink.Gray, R.drawable.ic_target, R.drawable.ic_target, R.drawable.ic_target)

fun Category.style(): CategoryStyle =
    if (type == CategoryType.INCOME) IncomeStyles[icon] ?: IncomeOther else ExpenseStyles[icon] ?: ExpenseOther

fun Transaction.style(): CategoryStyle =
    category?.style() ?: if (type == TransactionType.TRANSFER_OUT) TransferOutStyle else TransferStyle

private val DefaultNames = mapOf(
    (CategoryType.EXPENSE to "food") to R.string.cat_food,
    (CategoryType.EXPENSE to "transport") to R.string.cat_transport,
    (CategoryType.EXPENSE to "shopping") to R.string.cat_shopping,
    (CategoryType.EXPENSE to "bill") to R.string.cat_bill,
    (CategoryType.EXPENSE to "entertainment") to R.string.cat_entertainment,
    (CategoryType.EXPENSE to "health") to R.string.cat_health,
    (CategoryType.EXPENSE to "education") to R.string.cat_education,
    (CategoryType.EXPENSE to "other") to R.string.cat_other,
    (CategoryType.INCOME to "salary") to R.string.cat_salary,
    (CategoryType.INCOME to "gift") to R.string.cat_gift,
    (CategoryType.INCOME to "bonus") to R.string.cat_bonus,
    (CategoryType.INCOME to "other") to R.string.cat_other,
)

/**
 * Kategori bawaan yang namanya belum diubah → nama dalam bahasa app. Data di database tidak berubah (tetap nama
 * Indonesia); kategori buatan pengguna atau yang sudah diganti namanya tampil apa adanya.
 */
private fun Category.defaultNameRes(): Int? {
    if (uuid != defaultCategoryUuid(icon, type)) return null
    if (DefaultCategories.none { (seed, seedIcon, seedType) -> seed == name && seedIcon == icon && seedType == type }) return null
    return DefaultNames[type to icon]
}

@Composable
fun Category.displayName(): String = defaultNameRes()?.let { stringResource(it) } ?: name

/** Nama tersimpan (mis. kunci filter "Makanan") → nama tampilan; nama bawaan diterjemahkan, lainnya apa adanya. */
@Composable
fun categoryNameLabel(storedName: String): String =
    DefaultCategories.firstOrNull { it.first == storedName }?.let { (_, icon, type) -> DefaultNames[type to icon] }
        ?.let { stringResource(it) } ?: storedName

fun Category.displayName(context: Context): String = defaultNameRes()?.let(context::getString) ?: name

private val TransferBack = Regex("Dari (.+) \\((?:dibatalkan|dihapus)\\)")

/**
 * Catatan untuk ditampilkan. Catatan transfer goal dibuat app dalam bahasa Indonesia ("Ke Sepatu lari", "Dari … (dihapus)")
 * dan tersimpan begitu (ikut sinkron & cadangan), jadi diterjemahkan saat tampil; catatan pengguna apa adanya.
 */
@Composable
fun Transaction.displayNote(): String? = transferNoteRes()?.let { (res, name) -> stringResource(res, name) } ?: note

fun Transaction.displayNote(context: Context): String? = transferNoteRes()?.let { (res, name) -> context.getString(res, name) } ?: note

private fun Transaction.transferNoteRes(): Pair<Int, String>? {
    val text = note ?: return null
    return when {
        type == TransactionType.TRANSFER_OUT && text.startsWith("Ke ") -> R.string.transfer_to to text.removePrefix("Ke ")
        type == TransactionType.TRANSFER_IN -> TransferBack.matchEntire(text)?.let { R.string.transfer_back_from to it.groupValues[1] }
        else -> null
    }
}

/** Nama kategori; "Transfer" hanya untuk transfer goal, bukan untuk pemasukan/pengeluaran yang kategorinya hilang. */
@Composable
fun Transaction.categoryLabel(): String = category?.displayName()
    ?: stringResource(if (type == TransactionType.INCOME || type == TransactionType.EXPENSE) R.string.cat_other else R.string.category_transfer)

/** Pemasukan / pengeluaran; transfer goal netral (bukan pemasukan/pengeluaran, §3.2). */
val Transaction.amountKind: AmountKind
    get() = when (type) {
        TransactionType.INCOME -> AmountKind.INCOME
        TransactionType.EXPENSE -> AmountKind.EXPENSE
        else -> AmountKind.NEUTRAL
    }
