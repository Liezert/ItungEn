package com.liezert.itungen.data.sync

import com.liezert.itungen.data.local.dao.PendingGoal
import com.liezert.itungen.data.local.dao.PendingTransaction
import com.liezert.itungen.data.local.entity.GoalEntity
import com.liezert.itungen.data.local.entity.TransactionEntity
import com.liezert.itungen.domain.model.GoalStatus
import com.liezert.itungen.domain.model.TransactionType
import java.time.Instant
import java.time.LocalDate

// Dokumen Firestore hanya berisi String / Long / Boolean / null (tanpa tipe Firebase), jadi pemetaan ini
// bisa diuji di JVM. Field syncedAt (server timestamp) ditambahkan SyncManager saat mengunggah.

fun PendingTransaction.toDoc(): Map<String, Any?> = mapOf(
    "walletUuid" to walletUuid,
    "categoryUuid" to categoryUuid,
    "type" to tx.type.name,
    "amount" to tx.amount,
    "note" to tx.note,
    "date" to tx.date.toEpochDay(),
    "transferGroupId" to tx.transferGroupId,
    "createdAt" to tx.createdAt.toEpochMilli(),
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
)

fun PendingGoal.toDoc(): Map<String, Any?> = mapOf(
    "walletUuid" to walletUuid,
    "walletArchived" to walletArchived,
    "title" to goal.title,
    "targetAmount" to goal.targetAmount,
    "targetDate" to goal.targetDate?.toEpochDay(),
    "plannedMonthly" to goal.plannedMonthly,
    "status" to goal.status.name,
    "createdAt" to goal.createdAt.toEpochMilli(),
)

fun goalFromDoc(uuid: String, doc: Map<String, Any?>, walletId: Long, localId: Long = 0) = GoalEntity(
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
)
