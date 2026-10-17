package com.liezert.itungen.data.sync

import com.liezert.itungen.data.local.dao.PendingGoal
import com.liezert.itungen.data.local.dao.PendingTransaction
import com.liezert.itungen.data.local.entity.GoalEntity
import com.liezert.itungen.data.local.entity.TransactionEntity
import com.liezert.itungen.domain.model.GoalStatus
import com.liezert.itungen.domain.model.TransactionType
import com.liezert.itungen.domain.usecase.BudgetPeriod
import com.liezert.itungen.domain.usecase.BudgetPlan
import java.time.Instant
import java.time.LocalDate

// Batas firestore.rules. Input dibatasi lebih pendek (MAX_NOTE / MAX_TITLE); ini menjaga data lama atau hasil
// pemulihan cadangan agar tidak membuat sinkron ditolak selamanya.
private const val MaxSyncedNote = 500
private const val MaxSyncedTitle = 100

// Dokumen Firestore hanya berisi String / Long / Boolean / null (tanpa tipe Firebase), jadi pemetaan ini
// bisa diuji di JVM. Field syncedAt (server timestamp) ditambahkan SyncManager saat mengunggah.

fun PendingTransaction.toDoc(): Map<String, Any?> = mapOf(
    "walletUuid" to walletUuid,
    "categoryUuid" to categoryUuid,
    "type" to tx.type.name,
    "amount" to tx.amount,
    "note" to tx.note?.take(MaxSyncedNote),
    "date" to tx.date.toEpochDay(),
    "transferGroupId" to tx.transferGroupId,
    "createdAt" to tx.createdAt.toEpochMilli(),
    "updatedAt" to tx.updatedAt,
)

/** [walletId] / [categoryId] = id lokal hasil pencarian uuid di HP ini. */
fun transactionFromDoc(uuid: String, doc: Map<String, Any?>, walletId: Long, categoryId: Long?) = TransactionEntity(
    walletId = walletId,
    categoryId = categoryId,
    type = TransactionType.valueOf(doc["type"] as String),
    amount = (doc["amount"] as Number).toLong(),
    note = doc["note"] as String?,
    date = LocalDate.ofEpochDay((doc["date"] as Number).toLong()),
    transferGroupId = doc["transferGroupId"] as String?,
    createdAt = Instant.ofEpochMilli((doc["createdAt"] as Number).toLong()),
    uuid = uuid,
    synced = true,
    updatedAt = (doc["updatedAt"] as Number?)?.toLong() ?: 0, // dokumen dari versi sebelum fitur ubah
)

fun PendingGoal.toDoc(): Map<String, Any?> = mapOf(
    "walletUuid" to walletUuid,
    "walletArchived" to walletArchived,
    "title" to goal.title.take(MaxSyncedTitle),
    "targetAmount" to goal.targetAmount,
    "targetDate" to goal.targetDate?.toEpochDay(),
    "plannedMonthly" to goal.plannedMonthly,
    "status" to goal.status.name,
    "createdAt" to goal.createdAt.toEpochMilli(),
)

/** [local] = goal yang sudah ada di HP ini: kolom yang hanya lokal (gambar produk) dipertahankan. */
fun goalFromDoc(uuid: String, doc: Map<String, Any?>, walletId: Long, localId: Long = 0, local: GoalEntity? = null) = GoalEntity(
    id = localId,
    walletId = walletId,
    title = doc["title"] as String,
    targetAmount = (doc["targetAmount"] as Number).toLong(),
    targetDate = (doc["targetDate"] as Number?)?.let { LocalDate.ofEpochDay(it.toLong()) },
    plannedMonthly = (doc["plannedMonthly"] as Number).toLong(),
    status = GoalStatus.valueOf(doc["status"] as String),
    createdAt = Instant.ofEpochMilli((doc["createdAt"] as Number).toLong()),
    uuid = uuid,
    synced = true,
    imagePath = local?.imagePath,
)

/** Tanggal disimpan sebagai epoch-day, sama seperti transaksi. */
fun BudgetPlan.toDoc(): Map<String, Any?> = mapOf(
    "income" to income,
    "cycleStartDay" to cycleStartDay,
    "fixedBills" to fixedBills,
    "savingsTarget" to savingsTarget,
    "period" to period.name,
    "startedOn" to startedOn?.toEpochDay(),
    "nextIncomeOn" to nextIncomeOn?.toEpochDay(),
)

/** Juga membaca cadangan versi awal yang menyimpan tanggal sebagai teks "2026-10-15". */
fun budgetPlanFromDoc(doc: Map<String, Any?>) = BudgetPlan(
    income = (doc["income"] as Number).toLong(),
    cycleStartDay = (doc["cycleStartDay"] as Number).toInt(),
    fixedBills = (doc["fixedBills"] as Number?)?.toLong() ?: 0,
    savingsTarget = (doc["savingsTarget"] as Number?)?.toLong() ?: 0,
    period = (doc["period"] as String?)?.let(BudgetPeriod::valueOf) ?: BudgetPeriod.MONTHLY,
    startedOn = dateFromDoc(doc["startedOn"]),
    nextIncomeOn = dateFromDoc(doc["nextIncomeOn"]),
)

private fun dateFromDoc(value: Any?): LocalDate? = when (value) {
    is Number -> LocalDate.ofEpochDay(value.toLong())
    is String -> LocalDate.parse(value)
    else -> null
}
