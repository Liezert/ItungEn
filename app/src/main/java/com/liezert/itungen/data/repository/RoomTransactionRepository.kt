package com.liezert.itungen.data.repository

import androidx.room.withTransaction
import com.liezert.itungen.data.local.AppDatabase
import com.liezert.itungen.data.local.entity.CategoryEntity
import com.liezert.itungen.data.local.entity.RecurringRuleEntity
import com.liezert.itungen.data.local.entity.TransactionEntity
import com.liezert.itungen.data.local.entity.TransactionWithCategory
import com.liezert.itungen.domain.model.Category
import com.liezert.itungen.domain.model.CategoryType
import com.liezert.itungen.domain.model.Transaction
import com.liezert.itungen.domain.model.TransactionType
import com.liezert.itungen.domain.repository.TransactionRepository
import com.liezert.itungen.domain.usecase.recurringDueDates
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

    override fun observeTransactions(): Flow<List<Transaction>> =
        db.transactionDao().observeAll().map { rows -> rows.map { it.toDomain() } }

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
        repeatMonthly: Boolean,
    ) {
        require(type == TransactionType.INCOME || type == TransactionType.EXPENSE) { "Transfer lewat TransferToGoal" }
        if (!repeatMonthly) {
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
            return
        }
        db.withTransaction {
            db.recurringRuleDao().insert(
                RecurringRuleEntity(
                    walletId = mainWalletId(),
                    categoryId = categoryId,
                    type = type,
                    amount = amount,
                    note = note,
                    dayOfMonth = date.dayOfMonth,
                    startDate = date,
                )
            )
            // Transaksi pertama pun dicatat lewat aturan, jadi terikat recurring_rule_id dan terlindungi unique index.
            generateRecurring()
        }
    }

    override suspend fun generateRecurring(): Int = db.withTransaction {
        val today = LocalDate.now(clock)
        var inserted = 0
        for (rule in db.recurringRuleDao().getActive()) {
            val dates = recurringDueDates(rule.dayOfMonth, rule.startDate, rule.endDate, rule.lastGeneratedDate, today)
            if (dates.isEmpty()) continue
            for (date in dates) {
                val row = TransactionEntity(
                    walletId = rule.walletId,
                    categoryId = rule.categoryId,
                    type = rule.type,
                    amount = rule.amount,
                    note = rule.note,
                    date = date,
                    recurringRuleId = rule.id,
                    createdAt = Instant.now(clock),
                )
                if (db.transactionDao().insertOrIgnore(row) != -1L) inserted++
            }
            db.recurringRuleDao().setLastGenerated(rule.id, dates.last())
        }
        inserted
    }
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
)
