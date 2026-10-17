package com.liezert.itungen.data.repository

import androidx.room.withTransaction
import com.liezert.itungen.data.local.AppDatabase
import com.liezert.itungen.data.local.entity.DeletedTransactionEntity
import com.liezert.itungen.data.local.entity.CategoryEntity
import com.liezert.itungen.data.local.entity.TransactionEntity
import com.liezert.itungen.data.local.entity.TransactionWithCategory
import com.liezert.itungen.domain.model.Category
import com.liezert.itungen.domain.model.CategoryType
import com.liezert.itungen.domain.model.Transaction
import com.liezert.itungen.domain.model.TransactionType
import com.liezert.itungen.domain.repository.TransactionRepository
import com.liezert.itungen.domain.repository.Totals
import com.liezert.itungen.domain.usecase.CategorySpend
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import java.time.Clock
import java.time.Instant
import java.time.LocalDate

class RoomTransactionRepository(
    private val db: AppDatabase,
    private val clock: Clock,
) : TransactionRepository {

    private suspend fun mainWalletId(): Long = db.walletDao().getMain().id

    /** Kueri dompet utama; Room mengirim ulang setiap tabel berubah, jadi hasil yang sama tidak diteruskan. */
    private fun <T> main(query: (walletId: Long) -> Flow<T>): Flow<T> =
        flow { emitAll(query(mainWalletId())) }.distinctUntilChanged()

    private val dao get() = db.transactionDao()

    override fun observeRecent(limit: Int) = main { dao.observeRecent(it, limit) }.map { rows -> rows.map { it.toDomain() } }

    override fun observeBetween(from: LocalDate, to: LocalDate) =
        main { dao.observeBetween(it, from, to) }.map { rows -> rows.map { it.toDomain() } }

    override fun observeSearch(query: String, categoryName: String?, limit: Int) =
        main { dao.observeSearch(it, query.trim(), amountDigits(query), categoryName, limit) }.map { rows -> rows.map { it.toDomain() } }

    override fun observeTotals(from: LocalDate, to: LocalDate) =
        main { dao.observeTotals(it, from, to) }.map { Totals(it.income, it.expense, it.count) }

    override fun observeCategoryTotals(type: TransactionType, from: LocalDate, to: LocalDate) =
        main { dao.observeCategoryTotals(it, type, from, to) }.map { rows ->
            rows.map { CategorySpend(it.category.toDomain(), it.count, it.total) }
        }

    override fun observeDailyExpense(from: LocalDate, to: LocalDate) =
        main { dao.observeDailyExpense(it, from, to) }.map { rows -> rows.associate { it.date to it.total } }

    override fun observeRecordedDays(from: LocalDate, to: LocalDate) =
        main { dao.observeRecordedDays(it, from, to) }.map { it.toSet() }

    override fun observeFirstDate() = main { dao.observeFirstDate(it) }

    override fun observeHasTransactions() = main { dao.observeHasAny(it) }

    override suspend fun hasTransactionOn(date: LocalDate) = dao.hasOn(mainWalletId(), date)

    override suspend fun allTransactions() = dao.allForWallet(mainWalletId()).map { it.toDomain() }

    override fun observeMainBalance(): Flow<Long> = main { dao.observeBalance(it) }

    override fun observeCategories(type: CategoryType): Flow<List<Category>> =
        db.categoryDao().observeByType(type).map { rows -> rows.map { it.toDomain() } }

    override suspend fun addToMainWallet(
        type: TransactionType,
        amount: Long,
        categoryId: Long,
        note: String?,
        date: LocalDate,
    ): Long {
        require(type == TransactionType.INCOME || type == TransactionType.EXPENSE) { "Transfer lewat TransferToGoal" }
        return db.transactionDao().insert(
            TransactionEntity(
                walletId = mainWalletId(),
                categoryId = categoryId,
                type = type,
                amount = amount,
                note = note,
                date = date,
                createdAt = Instant.now(clock),
            )
        )
    }

    override suspend fun getTransaction(id: Long): Transaction? = dao.getWithCategory(id)?.toDomain()

    override suspend fun updateTransaction(
        id: Long,
        type: TransactionType,
        amount: Long,
        categoryId: Long,
        note: String?,
        date: LocalDate,
    ) = db.withTransaction {
        val tx = dao.get(id)
        require(tx.type.editable && type.editable) { "Transfer goal tidak bisa diubah" }
        dao.update(
            tx.copy(
                type = type, amount = amount, categoryId = categoryId, note = note, date = date,
                updatedAt = clock.millis(), synced = false,
            )
        )
    }

    override suspend fun deleteTransaction(id: Long): suspend () -> Unit {
        val tx = db.withTransaction {
            // Sudah terhapus (mis. ketuk/geser ganda, atau dihapus di HP lain): tidak ada yang perlu diurungkan.
            val tx = db.transactionDao().find(id) ?: return@withTransaction null
            require(tx.type.editable) { "Transfer goal tidak bisa dihapus" }
            db.transactionDao().delete(id)
            // Penanda selalu dikirim (bila masuk akun), termasuk untuk baris yang belum tersinkron: unggahannya bisa
            // sedang berjalan saat ini, jadi dokumennya mungkin sudah ada di server. Penanda juga mencegah pemulihan
            // cadangan lama memunculkannya lagi.
            db.syncDao().insertTombstone(DeletedTransactionEntity(tx.uuid))
            tx
        }
        if (tx == null) return {}
        // Urungkan: baris yang sama kembali (id & uuid tetap) dan diunggah lagi, menimpa penanda hapus di server.
        return {
            db.withTransaction {
                db.syncDao().deleteTombstone(tx.uuid)
                db.syncDao().insertTransaction(tx.copy(synced = false))
            }
        }
    }

    // atZone, bukan LocalDate.ofInstant: yang terakhir baru ada di API 34 (minSdk 33).
    override suspend fun firstUseDate(): LocalDate = db.walletDao().getMain().createdAt.atZone(clock.zone).toLocalDate()
}

private fun CategoryEntity.toDomain() = Category(id, name, icon, color, type, uuid)

/** Hanya pemasukan/pengeluaran yang dicatat pengguna; transfer goal mengikuti goal-nya. */
private val TransactionType.editable get() = this == TransactionType.INCOME || this == TransactionType.EXPENSE

private fun TransactionWithCategory.toDomain() = Transaction(
    id = transaction.id,
    walletId = transaction.walletId,
    category = category?.toDomain(),
    type = transaction.type,
    amount = transaction.amount,
    note = transaction.note,
    date = transaction.date,
    createdAt = transaction.createdAt,
)

/** "Rp 15.000" / "15.000" / "15000" → "15000"; teks lain → "" (bukan pencarian nominal). */
internal fun amountDigits(query: String): String {
    val digits = query.trim().removePrefix("Rp").removePrefix("rp").filterNot { it == '.' || it.isWhitespace() || it == '\u00A0' }
    return digits.takeIf { it.isNotEmpty() && it.all(Char::isDigit) }.orEmpty()
}
