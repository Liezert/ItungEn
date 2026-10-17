package com.liezert.itungen.data.backup

import androidx.room.withTransaction
import com.liezert.itungen.data.images.GoalImageStore
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
import kotlinx.coroutines.CancellationException
import org.json.JSONArray
import org.json.JSONObject
import java.io.InputStream
import java.io.OutputStream
import java.time.Instant

/** File cadangan bukan buatan ItungEn atau rusak. */
class InvalidBackupException : Exception()

/**
 * [added] = baris baru; [planRestored] = rencana anggaran ikut dipulihkan (HP ini belum punya);
 * [images] = gambar goal yang ikut dipulihkan.
 */
data class ImportResult(val added: Int, val planRestored: Boolean, val images: Int = 0)

/**
 * Cadangan manual (PRD P0): ZIP yang bisa dipulihkan (data + gambar goal, lihat BackupArchive.kt) + CSV untuk dibuka
 * di spreadsheet. Isi JSON memakai bentuk dokumen sinkron (uuid sebagai kunci), jadi memulihkan ke HP yang sudah punya
 * sebagian data tidak menggandakan baris. Baris yang dipulihkan ditandai belum tersinkron agar ikut terunggah bila
 * masuk akun. Rencana anggaran ikut tersimpan, dan transaksi yang sudah dihapus tidak dipulihkan lagi.
 * Cadangan JSON lama (sebelum ada gambar goal) tetap bisa dipulihkan.
 */
class Backup(
    private val db: AppDatabase,
    private val settings: SyncedSettings,
    private val images: GoalImageStore,
) {
    private val dao = db.syncDao()

    suspend fun export(out: OutputStream) {
        val goals = dao.allGoals()
        // Hanya gambar yang file-nya masih ada yang dirujuk, agar JSON tidak menunjuk ke entri yang tidak ikut.
        val files = goals.mapNotNull { it.goal.imagePath }.associateWith(images::file).filterValues { it.isFile }
        val goalDocs = goals.map { JSONObject(it.toDoc() + ("uuid" to it.goal.uuid) + ("image" to it.goal.imagePath?.takeIf(files::containsKey))) }
        val transactions = JSONArray(dao.allTransactions().map { JSONObject(it.toDoc() + ("uuid" to it.tx.uuid)) })
        val root = JSONObject()
            .put("app", APP)
            .put("version", 1)
            .put("exportedAt", Instant.now().toString())
            .put("goals", JSONArray(goalDocs))
            .put("transactions", transactions)
            .put("deleted", JSONArray(dao.deletedUuids()))
            .put("settings", JSONObject(settings.toDoc()))
        writeArchive(out, root.toString(2), files)
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

    /**
     * Pulihkan dari cadangan ZIP atau JSON lama; yang sudah ada dilewati. Gambar yang rusak atau tidak ada di berkas
     * dilewati: goal-nya tetap dipulihkan, tanpa gambar. Gambar disimpan lewat [GoalImageStore] dengan nama baru.
     */
    suspend fun import(input: InputStream): ImportResult {
        val stream = input.buffered()
        stream.mark(4)
        val header = ByteArray(4)
        var read = 0
        while (read < header.size) read += stream.read(header, read, header.size - read).takeIf { it > 0 } ?: break
        stream.reset()
        if (!isZip(header)) return restore(parse(stream.bufferedReader().readText()), emptyMap(), mutableSetOf())

        val saved = mutableMapOf<String, String>() // nama di cadangan → nama baru di goal_images
        val json = try {
            readArchive(stream) { name, bytes ->
                try {
                    saved[name] = images.save(bytes)
                } catch (e: Exception) {
                    if (e is CancellationException) throw e
                }
            } ?: throw InvalidBackupException()
        } catch (e: Exception) {
            saved.values.forEach { images.delete(it) }
            throw e
        }
        val used = mutableSetOf<String>()
        try {
            return restore(parse(json), saved, used)
        } finally {
            // Gambar milik goal yang sudah ada di HP ini (dilewati) atau impor yang gagal: jangan jadi file yatim.
            (saved.values - used).forEach { images.delete(it) }
        }
    }

    private fun parse(text: String): JSONObject {
        val root = try {
            JSONObject(text)
        } catch (_: Exception) {
            throw InvalidBackupException()
        }
        if (root.optString("app") != APP) throw InvalidBackupException()
        return root
    }

    /** [images] = nama gambar di cadangan → nama baru; nama yang dipakai goal baru masuk ke [used] setelah tersimpan. */
    private suspend fun restore(root: JSONObject, images: Map<String, String>, used: MutableSet<String>): ImportResult {
        var added = 0
        val restoredImages = mutableListOf<String>()
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
                val image = (doc["image"] as String?)?.let(images::get)
                dao.insertGoal(goalFromDoc(uuid, doc, walletId).copy(synced = false, imagePath = image))
                if (image != null) restoredImages += image
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
        used += restoredImages
        // Cadangan versi awal menyimpan rencana di "budgetPlan" (tanggal berupa teks).
        val planDoc = root.optJSONObject("settings")?.optJSONObject("budgetPlan") ?: root.optJSONObject("budgetPlan")
        val backupPlan = planDoc?.let { runCatching { budgetPlanFromDoc(it.toMap()) }.getOrNull() }
        val planRestored = backupPlan != null && settings.plan.value == null
        if (planRestored) settings.savePlan(backupPlan!!)
        root.optJSONObject("settings")?.let { settings.restoreBackup(it.toMap().mapValues { (_, v) -> v.toPlain() }) }
        return ImportResult(added, planRestored, restoredImages.size)
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

/** Nilai bersarang org.json (JSONObject/JSONArray) → Map/List biasa. */
private fun Any?.toPlain(): Any? = when (this) {
    is JSONObject -> toMap().mapValues { (_, v) -> v.toPlain() }
    is JSONArray -> (0 until length()).map { get(it).toPlain() }
    else -> this
}

private fun TransactionType.label() = when (this) {
    TransactionType.INCOME -> "Pemasukan"
    TransactionType.EXPENSE -> "Pengeluaran"
    TransactionType.TRANSFER_OUT -> "Ke target"
    TransactionType.TRANSFER_IN -> "Dari target"
}

/** Kutip bila perlu (pemisah, kutip, baris baru), sesuai RFC 4180. */
private fun csv(value: String): String =
    if (value.any { it == ';' || it == ',' || it == '"' || it == '\n' }) "\"" + value.replace("\"", "\"\"") + "\"" else value
