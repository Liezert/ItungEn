package com.liezert.itungen.data.repository

import com.liezert.itungen.data.local.AppDatabase
import com.liezert.itungen.data.local.entity.CategoryEntity
import com.liezert.itungen.data.local.entity.TransactionEntity
import com.liezert.itungen.data.local.entity.TransactionWithCategory
import com.liezert.itungen.domain.model.Category
import com.liezert.itungen.domain.model.CategoryType
import com.liezert.itungen.domain.model.Transaction
import com.liezert.itungen.domain.model.TransactionType
import com.liezert.itungen.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
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

    override fun observeTransactions(): Flow<List<Transaction>> = flow {
        emitAll(db.transactionDao().observeForWallet(mainWalletId()).map { rows -> rows.map { it.toDomain() } })
    }

    override fun observeMainBalance(): Flow<Long> = flow {
        emitAll(db.transactionDao().observeBalance(mainWalletId()))
    }

    override fun observeCategories(type: CategoryType): Flow<List<Category>> =
        db.categoryDao().observeByType(type).map { rows -> rows.map { it.toDomain() } }

    override suspend fun addToMainWallet(
        type: TransactionType,
        amount: Long,
        categoryId: Long,
        note: String?,
        date: LocalDate,
    ) {
        require(type == TransactionType.INCOME || type == TransactionType.EXPENSE) { "Transfer lewat TransferToGoal" }
        db.transactionDao().insert(
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

    override suspend fun firstUseDate(): LocalDate = LocalDate.ofInstant(db.walletDao().getMain().createdAt, clock.zone)
}

private fun CategoryEntity.toDomain() = Category(id, name, icon, color, type)

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
