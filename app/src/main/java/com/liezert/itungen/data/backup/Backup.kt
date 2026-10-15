package com.liezert.itungen.data.backup

import androidx.room.withTransaction
import com.liezert.itungen.data.local.AppDatabase
import com.liezert.itungen.data.local.defaultCategoryUuid
import com.liezert.itungen.data.local.entity.DeletedTransactionEntity
import com.liezert.itungen.data.local.entity.WalletEntity
import com.liezert.itungen.data.settings.SyncedSettings
import com.liezert.itungen.data.sync.budgetPlanFromDoc
import com.liezert.itungen.data.sync.goalFromDoc
import com.liezert.itungen.data.sync.toDoc
import com.liezert.itungen.data.sync.transactionFromDoc
import com.liezert.itungen.domain.model.CategoryType
import com.liezert.itungen.domain.model.Transaction
import com.liezert.itungen.domain.model.TransactionType
import com.liezert.itungen.domain.model.WalletType
import org.json.JSONArray
import org.json.JSONObject
import java.io.InputStream
import java.io.OutputStream
import java.time.Instant

/** File cadangan bukan buatan ItungEn atau rusak. */
class InvalidBackupException : Exception()

/** [added] = baris baru; [planRestored] = rencana anggaran ikut dipulihkan (HP ini belum punya). */
data class ImportResult(val added: Int, val planRestored: Boolean)

/**
 * Cadangan manual (PRD P0): JSON yang bisa dipulihkan + CSV untuk dibuka di spreadsheet.
 * Isi JSON memakai bentuk dokumen sinkron (uuid sebagai kunci), jadi memulihkan ke HP yang sudah punya sebagian
 * data tidak menggandakan baris. Baris yang dipulihkan ditandai belum tersinkron agar ikut terunggah bila masuk akun.
 * Rencana anggaran ikut tersimpan, dan transaksi yang sudah dihapus tidak dipulihkan lagi.
 */
class Backup(
    private val db: AppDatabase,
    private val settings: SyncedSettings,
) {
    private val dao = db.syncDao()

    suspend fun exportJson(out: OutputStream) {
        val goals = JSONArray(dao.allGoals().map { JSONObject(it.toDoc() + ("uuid" to it.goal.uuid)) })
        val transactions = JSONArray(dao.allTransactions().map { JSONObject(it.toDoc() + ("uuid" to it.tx.uuid)) })
        val root = JSONObject()
            .put("app", APP)
            .put("version", 1)
            .put("exportedAt", Instant.now().toString())
            .put("goals", goals)
            .put("transactions", transactions)
            .put("deleted", JSONArray(dao.deletedUuids()))
            .put("settings", JSONObject(settings.toDoc()))
        out.writer().use { it.write(root.toString(2)) }
    }

    /**
     * Transaksi dompet utama (yang tampil di Semua Transaksi), satu baris per transaksi. Pemisah titik koma + BOM UTF-8:
     * Excel dengan setelan regional Indonesia memakai titik koma, dan Google Sheets mendeteksinya sendiri.
     */
    fun exportCsv(transactions: List<Transaction>, out: OutputStream) {
        out.writer().use { w ->
            w.write("\uFEFF")
            w.write("Tanggal;Jenis;Kategori;Catatan;Nominal\n")
            for (t in transactions.sortedBy { it.date }) {
                val signed = if (t.type == TransactionType.INCOME || t.type == TransactionType.TRANSFER_IN) t.amount else -t.amount
                w.write(listOf(t.date.toString(), t.type.label(), t.category?.name.orEmpty(), t.note.orEmpty(), signed.toString()).joinToString(";") { csv(it) })
                w.write("\n")
            }
        }
    }

    /** Yang sudah ada dilewati. */
    suspend fun importJson(input: InputStream): ImportResult {
        val root = try {
            JSONObject(input.bufferedReader().readText())
        } catch (_: Exception) {
            throw InvalidBackupException()
        }
        if (root.optString("app") != APP) throw InvalidBackupException()
        var added = 0
        db.withTransaction {
            // Dihapus di HP ini atau di HP asal cadangan: jangan dimunculkan lagi.
            val deleted = dao.deletedUuids().toMutableSet()
            root.optJSONArray("deleted")?.let { arr ->
                (0 until arr.length()).map { arr.getString(it) }.forEach {
                    deleted += it
                    dao.insertTombstone(DeletedTransactionEntity(it, pushed = true))
                }
            }
            root.optJSONArray("goals")?.objects()?.forEach { doc ->
                val uuid = doc["uuid"] as String
                if (dao.goal(uuid) != null) return@forEach
                val walletUuid = doc["walletUuid"] as String
                val walletId = dao.walletId(walletUuid) ?: dao.insertWallet(
                    WalletEntity(
                        name = doc["title"] as String,
                        type = WalletType.GOAL,
                        isArchived = doc["walletArchived"] as Boolean,
                        createdAt = Instant.ofEpochMilli((doc["createdAt"] as Number).toLong()),
                        uuid = walletUuid,
                    )
                )
                dao.insertGoal(goalFromDoc(uuid, doc, walletId).copy(synced = false))
                added++
            }
            root.optJSONArray("transactions")?.objects()?.forEach { doc ->
                val uuid = doc["uuid"] as String
                if (uuid in deleted) return@forEach
                val walletId = dao.walletId(doc["walletUuid"] as String) ?: return@forEach
                val row = transactionFromDoc(uuid, doc, walletId, categoryId(doc)).copy(synced = false)
                if (dao.insertTransaction(row) != -1L) added++
            }
        }
        // Cadangan versi awal menyimpan rencana di "budgetPlan" (tanggal berupa teks).
        val planDoc = root.optJSONObject("settings")?.optJSONObject("budgetPlan") ?: root.optJSONObject("budgetPlan")
        val backupPlan = planDoc?.let { runCatching { budgetPlanFromDoc(it.toMap()) }.getOrNull() }
        val planRestored = backupPlan != null && settings.plan.value == null
        if (planRestored) settings.savePlan(backupPlan!!)
        return ImportResult(added, planRestored)
    }

    /** Kategori tak dikenal (mis. berkas dari versi lain) masuk ke "Lainnya" jenisnya, bukan tanpa kategori. */
    private suspend fun categoryId(doc: Map<String, Any?>): Long? {
        (doc["categoryUuid"] as String?)?.let { uuid -> dao.categoryId(uuid)?.let { return it } }
        val type = when (doc["type"]) {
            TransactionType.INCOME.name -> CategoryType.INCOME
            TransactionType.EXPENSE.name -> CategoryType.EXPENSE
            else -> return null // transfer memang tanpa kategori
        }
        return dao.categoryId(defaultCategoryUuid("other", type))
    }

    private companion object {
        const val APP = "ItungEn"
    }
}

/** JSONObject → Map dengan null untuk field yang tidak ada (JSONObject membuang nilai null saat ekspor). */
private fun JSONArray.objects(): List<Map<String, Any?>> = (0 until length()).map { getJSONObject(it).toMap() }

private fun JSONObject.toMap(): Map<String, Any?> = keys().asSequence().associateWith { key -> get(key).takeUnless { it == JSONObject.NULL } }

private fun TransactionType.label() = when (this) {
    TransactionType.INCOME -> "Pemasukan"
    TransactionType.EXPENSE -> "Pengeluaran"
    TransactionType.TRANSFER_OUT -> "Ke target"
    TransactionType.TRANSFER_IN -> "Dari target"
}

/** Kutip bila perlu (pemisah, kutip, baris baru), sesuai RFC 4180. */
private fun csv(value: String): String =
    if (value.any { it == ';' || it == ',' || it == '"' || it == '\n' }) "\"" + value.replace("\"", "\"\"") + "\"" else value
