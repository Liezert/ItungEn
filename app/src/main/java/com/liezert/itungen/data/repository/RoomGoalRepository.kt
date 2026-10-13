package com.liezert.itungen.data.repository

import androidx.room.withTransaction
import com.liezert.itungen.data.local.AppDatabase
import com.liezert.itungen.data.local.entity.GoalEntity
import com.liezert.itungen.data.local.entity.TransactionEntity
import com.liezert.itungen.data.local.entity.WalletEntity
import com.liezert.itungen.domain.model.Goal
import com.liezert.itungen.domain.model.GoalStatus
import com.liezert.itungen.domain.model.TransactionType
import com.liezert.itungen.domain.model.WalletType
import com.liezert.itungen.domain.repository.GoalRepository
import com.liezert.itungen.domain.usecase.InsufficientBalanceException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

class RoomGoalRepository(
    private val db: AppDatabase,
    private val clock: Clock,
) : GoalRepository {

    override fun observeGoals(): Flow<List<Goal>> = db.goalDao().observeWithSaved().map { rows ->
        rows.map {
            Goal(
                name = it.goal.title,
                saved = it.saved,
                target = it.goal.targetAmount,
                id = it.goal.id,
                targetDate = it.goal.targetDate,
                plannedMonthly = it.goal.plannedMonthly,
                status = it.goal.status,
            )
        }
    }

    override suspend fun createGoal(title: String, target: Long, targetDate: LocalDate?, plannedMonthly: Long): Long =
        db.withTransaction {
            val now = Instant.now(clock)
            val walletId = db.walletDao().insert(WalletEntity(name = title, type = WalletType.GOAL, createdAt = now))
            db.goalDao().insert(
                GoalEntity(
                    walletId = walletId,
                    title = title,
                    targetAmount = target,
                    targetDate = targetDate,
                    plannedMonthly = plannedMonthly,
                    createdAt = now,
                )
            )
        }

    override suspend fun addFunds(goalId: Long, amount: Long) {
        require(amount > 0) { "Nominal harus > 0" }
        db.withTransaction {
            val goal = db.goalDao().get(goalId)
            val mainId = db.walletDao().getMain().id
            // Diperiksa ulang di dalam transaksi: layar bisa memvalidasi dengan saldo yang sudah usang.
            if (amount > db.transactionDao().balance(mainId)) throw InsufficientBalanceException()
            transfer(fromWallet = mainId, toWallet = goal.walletId, amount = amount, outNote = "Ke ${goal.title}", inNote = "Tambah dana")
            if (goal.status == GoalStatus.ACTIVE && db.transactionDao().balance(goal.walletId) >= goal.targetAmount) {
                db.goalDao().setStatus(goalId, GoalStatus.ACHIEVED)
            }
        }
    }

    override suspend fun cancelGoal(goalId: Long) {
        db.withTransaction {
            val goal = db.goalDao().get(goalId)
            val saved = db.transactionDao().balance(goal.walletId)
            if (saved > 0) {
                val mainId = db.walletDao().getMain().id
                transfer(
                    fromWallet = goal.walletId,
                    toWallet = mainId,
                    amount = saved,
                    outNote = "Dikembalikan ke saldo",
                    inNote = "Dari ${goal.title} (dibatalkan)",
                )
            }
            db.goalDao().setStatus(goalId, GoalStatus.CANCELLED)
            db.walletDao().archive(goal.walletId)
        }
    }

    /** Dua baris berpasangan yang diikat transfer_group_id; bukan pemasukan/pengeluaran (§3.2). */
    private suspend fun transfer(fromWallet: Long, toWallet: Long, amount: Long, outNote: String, inNote: String) {
        val group = UUID.randomUUID().toString()
        val now = Instant.now(clock)
        val today = LocalDate.now(clock)
        val dao = db.transactionDao()
        dao.insert(
            TransactionEntity(
                walletId = fromWallet, type = TransactionType.TRANSFER_OUT, amount = amount, note = outNote,
                date = today, transferGroupId = group, createdAt = now,
            )
        )
        dao.insert(
            TransactionEntity(
                walletId = toWallet, type = TransactionType.TRANSFER_IN, amount = amount, note = inNote,
                date = today, transferGroupId = group, createdAt = now,
            )
        )
    }
}
