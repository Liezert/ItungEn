package com.liezert.itungen

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.room.withTransaction
import com.liezert.itungen.data.demo.seedDemoData
import com.liezert.itungen.data.local.entity.TransactionEntity
import com.liezert.itungen.domain.model.CategoryType
import com.liezert.itungen.domain.model.TransactionType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import kotlin.random.Random

/**
 * Hanya build "bench": mengisi data uji besar untuk pengukuran (docs/PERFORMANCE.md).
 * Data contoh biasa + [count] pengeluaran/pemasukan acak selama ±3 tahun terakhir, satu transaksi database.
 */
class BenchSeedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val container = (context.applicationContext as ItungEnApp).container
        val count = intent.getIntExtra("count", 30_000)
        val pending = goAsync()
        container.appScope.launch {
            try {
                container.onboarded = true
                seedDemoData(container)
                val repo = container.transactionRepository
                val expense = repo.observeCategories(CategoryType.EXPENSE).first().map { it.id }
                val income = repo.observeCategories(CategoryType.INCOME).first().map { it.id }
                val db = container.database
                val mainId = db.walletDao().getMain().id
                val today = LocalDate.now(container.clock)
                val random = Random(42)
                db.withTransaction {
                    repeat(count) { i ->
                        val isIncome = i % 25 == 0
                        db.transactionDao().insert(
                            TransactionEntity(
                                walletId = mainId,
                                categoryId = if (isIncome) income.random(random) else expense.random(random),
                                type = if (isIncome) TransactionType.INCOME else TransactionType.EXPENSE,
                                amount = if (isIncome) random.nextLong(500_000, 5_000_000) else random.nextLong(1, 200) * 1_000,
                                note = if (random.nextInt(3) == 0) null else "Uji ${i % 50}",
                                date = today.minusDays(random.nextLong(0, 1_100)),
                                createdAt = Instant.now(container.clock),
                            )
                        )
                    }
                }
            } finally {
                pending.finish()
            }
        }
    }
}
