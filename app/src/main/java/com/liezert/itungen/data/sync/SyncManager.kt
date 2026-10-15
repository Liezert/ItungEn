package com.liezert.itungen.data.sync

import android.content.SharedPreferences
import android.os.SystemClock
import android.util.Log
import androidx.room.withTransaction
import com.google.firebase.Timestamp
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Source
import com.liezert.itungen.data.local.AppDatabase
import com.liezert.itungen.data.local.dao.SyncDao
import com.liezert.itungen.data.settings.SyncedSettings
import com.liezert.itungen.data.local.entity.DeletedTransactionEntity
import com.liezert.itungen.data.local.entity.WalletEntity
import com.liezert.itungen.domain.model.WalletType
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import java.time.Instant

sealed interface SyncStatus {
    data object Idle : SyncStatus
    data object Syncing : SyncStatus
    data class Synced(val at: Instant) : SyncStatus
    /** [reason] = penyebab yang bisa dipahami pengguna, mis. "Tidak ada koneksi internet." */
    data class Failed(val reason: String) : SyncStatus
}

/**
 * Sinkron Room ↔ Firestore (`users/{uid}/goals`, `users/{uid}/transactions`, plus dokumen pengaturan
 * `users/{uid}/settings/main`). Room tetap sumber data layar,
 * jadi app jalan penuh saat offline.
 *
 * - Unggah: baris `synced = 0` dikirim per batch, lalu ditandai `synced = 1` setelah server menerima.
 * - Tarik: dokumen dengan `syncedAt` (waktu server) setelah kursor terakhir, per akun. Goal dulu, baru transaksi,
 *   karena transaksi merujuk dompet goal.
 * - Bentrok: transaksi tidak pernah diubah, hanya dihapus (dokumen ditimpa `{deleted: true}`) atau dipulihkan lewat
 *   Urungkan (dokumen ditimpa lagi dengan isinya);
 *   goal yang masih punya perubahan lokal menang (akan diunggah).
 */
class SyncManager(
    private val db: AppDatabase,
    /** Dibuat saat pertama dipakai: hanya setelah masuk, yang berarti Firebase sudah terkonfigurasi. */
    firestoreProvider: () -> FirebaseFirestore,
    private val account: GoogleAccount,
    private val settings: SyncedSettings,
    prefs: SharedPreferences,
    scope: CoroutineScope,
) {
    private val firestore by lazy(firestoreProvider)
    private val cursors = SyncCursors(prefs)
    private val dao = db.syncDao()
    private val mutex = Mutex()
    /** elapsedRealtime sinkron terakhir yang berhasil; 0 = belum pernah sejak proses dimulai. */
    @Volatile private var lastSuccessAt = 0L
    private val _status = MutableStateFlow<SyncStatus>(SyncStatus.Idle)
    val status: StateFlow<SyncStatus> = _status.asStateFlow()

    init {
        // Unggah otomatis setiap ada perubahan lokal selama masuk. Jeda singkat mengumpulkan beberapa perubahan;
        // bila gagal (offline), tunggu lebih lama agar tidak mencoba terus-menerus.
        scope.launch {
            combine(account.user, dao.observePendingCount(), settings.dirty) { user, pending, dirty ->
                user != null && (pending > 0 || dirty)
            }
                .conflate()
                .collect { needed ->
                    if (!needed) return@collect
                    delay(1_500)
                    if (!sync()) delay(30_000)
                }
        }
    }

    /** Unggah lalu tarik. false bila belum masuk atau gagal (mis. offline); data lokal tidak berubah karena gagal. */
    suspend fun sync(): Boolean = mutex.withLock {
        val uid = account.user.value?.uid ?: return false
        _status.value = SyncStatus.Syncing
        try {
            // Tanpa koneksi, commit Firestore menunggu selamanya; batas waktu mengembalikan kontrol ke pengguna.
            withTimeout(30_000) {
                push(uid)
                pull(uid)
                syncSettings(uid)
            }
            _status.value = SyncStatus.Synced(Instant.now())
            lastSuccessAt = SystemClock.elapsedRealtime()
            true
        } catch (e: CancellationException) {
            if (e !is TimeoutCancellationException) throw e
            _status.value = SyncStatus.Failed("Tidak ada koneksi internet atau server lambat merespons.")
            false
        } catch (e: Exception) {
            Log.w("ItungEn", "Sinkron gagal", e)
            _status.value = SyncStatus.Failed(failureReason(e))
            false
        }
    }

    /**
     * App kembali ke depan: tarik perubahan dari HP lain, paling sering sekali per [MIN_FOREGROUND_GAP_MS].
     * Unggah tidak menunggu ini (otomatis setiap ada perubahan), dan tidak ada tarik berkala di latar belakang.
     */
    suspend fun syncOnForeground() {
        if (lastSuccessAt == 0L || SystemClock.elapsedRealtime() - lastSuccessAt >= MIN_FOREGROUND_GAP_MS) sync()
    }

    /** Jaringan tersambung lagi: coba ulang hanya bila sinkron terakhir gagal (mis. tadi offline). */
    suspend fun retryAfterReconnect() {
        if (_status.value is SyncStatus.Failed) sync()
    }

    /**
     * Keluar akun. Data di HP dihapus (tetap aman di akun) agar akun lain yang masuk berikutnya tidak tercampur.
     * false = masih ada perubahan yang belum terunggah dan unggah gagal; tidak ada yang dihapus.
     */
    suspend fun signOut(): Boolean {
        if (hasPending() && !sync()) return false
        mutex.withLock {
            if (hasPending()) return false // berubah lagi selama unggah
            val uid = account.user.value?.uid
            account.signOut()
            db.withTransaction {
                dao.deleteTransactions()
                dao.deleteTombstones()
                dao.deleteGoals()
                dao.deleteGoalWallets()
            }
            settings.clear()
            if (uid != null) cursors.clear(uid, Collections)
            _status.value = SyncStatus.Idle
        }
        return true
    }

    private suspend fun push(uid: String) {
        val user = firestore.collection("users").document(uid)
        for (chunk in dao.pendingGoals().chunked(400)) {
            val batch = firestore.batch()
            chunk.forEach { batch.set(user.collection(GOALS).document(it.goal.uuid), it.toDoc() + SyncedAt) }
            batch.commit().await()
            chunk.forEach { dao.markGoalSynced(it.goal.uuid, it.goal.status) }
        }
        // Penghapusan dulu: dokumen ditimpa penanda agar HP lain ikut menghapus saat menarik.
        while (true) {
            val chunk = dao.pendingDeletes().ifEmpty { break }
            val batch = firestore.batch()
            chunk.forEach { batch.set(user.collection(TRANSACTIONS).document(it), Deleted + SyncedAt) }
            batch.commit().await()
            dao.markDeletesSynced(chunk)
        }
        while (true) {
            val chunk = dao.pendingTransactions().ifEmpty { break }
            val batch = firestore.batch()
            chunk.forEach { batch.set(user.collection(TRANSACTIONS).document(it.tx.uuid), it.toDoc() + SyncedAt) }
            batch.commit().await()
            dao.markTransactionsSynced(chunk.map { it.tx.uuid })
        }
    }

    private suspend fun hasPending() = dao.pendingCount() > 0 || settings.dirty.value

    /**
     * Satu dokumen pengaturan per akun (1 baca per sinkron). Perubahan terakhir menang: yang di server lebih baru
     * diterapkan; yang di HP ini lebih baru dan belum terunggah ditulis ke server.
     */
    private suspend fun syncSettings(uid: String) {
        val ref = firestore.collection("users").document(uid).collection(SETTINGS).document(SETTINGS_DOC)
        val remote = ref.get(Source.SERVER).await()
        val remoteAt = remote.getLong("updatedAt") ?: -1
        when {
            remote.exists() && remoteAt > settings.updatedAt -> settings.applyRemote(remote.data.orEmpty())
            settings.dirty.value -> {
                val at = settings.updatedAt
                ref.set(settings.toDoc() + SyncedAt).await()
                settings.markPushed(at)
            }
        }
    }

    private suspend fun pull(uid: String) {
        val user = firestore.collection("users").document(uid)
        pullCollection(uid, user.collection(GOALS), GOALS, ::applyGoal)
        pullCollection(uid, user.collection(TRANSACTIONS), TRANSACTIONS, ::applyTransaction)
    }

    /**
     * Tarik inkremental berhalaman: dokumen setelah kursor, urut (syncedAt, id). Kursor hanya maju setelah baris
     * berhasil diterapkan, jadi yang gagal dicoba lagi sinkron berikutnya.
     */
    private suspend fun pullCollection(
        uid: String,
        collection: CollectionReference,
        name: String,
        apply: suspend (uuid: String, doc: Map<String, Any?>) -> Boolean,
    ) {
        while (true) {
            val cursor = cursors.load(uid, name)
            val docs = collection.orderBy(SYNCED_AT).orderBy(FieldPath.documentId())
                .startAfter(Timestamp(cursor.seconds, cursor.nanos), cursor.docId)
                .limit(PAGE_SIZE)
                .get(Source.SERVER).await().documents
            for (doc in docs) {
                val at = doc.getTimestamp(SYNCED_AT) ?: continue
                val data = doc.data ?: continue
                if (!db.withTransaction { apply(doc.id, data) }) return
                cursors.save(uid, name, PullCursor(at.seconds, at.nanoseconds, doc.id))
            }
            if (docs.size < PAGE_SIZE) return
        }
    }

    private suspend fun applyGoal(uuid: String, doc: Map<String, Any?>): Boolean {
        val walletUuid = doc["walletUuid"] as String
        val archived = doc["walletArchived"] as Boolean
        val local = dao.goal(uuid)
        when {
            local == null -> {
                val walletId = dao.walletId(walletUuid) ?: dao.insertWallet(
                    WalletEntity(
                        name = doc["title"] as String,
                        type = WalletType.GOAL,
                        isArchived = archived,
                        createdAt = Instant.ofEpochMilli((doc["createdAt"] as Number).toLong()),
                        uuid = walletUuid,
                    )
                )
                dao.insertGoal(goalFromDoc(uuid, doc, walletId))
            }
            local.synced -> {
                dao.updateGoal(goalFromDoc(uuid, doc, local.walletId, local.id))
                dao.setWalletArchived(local.walletId, archived)
            }
            else -> Unit // perubahan lokal belum terunggah menang; akan menimpa dokumen ini saat diunggah
        }
        return true
    }

    private suspend fun applyTransaction(uuid: String, doc: Map<String, Any?>): Boolean = applyRemoteTransaction(dao, uuid, doc)

    private fun failureReason(e: Exception): String {
        val firestoreError = e as? FirebaseFirestoreException
        return when {
            firestoreError == null -> "Terjadi kesalahan tak terduga."
            // Pesan server saat project Firebase belum punya database Firestore.
            firestoreError.message.orEmpty().let { "has not been used" in it || "does not exist" in it || "disabled" in it } ->
                "Database Firestore belum dibuat di Firebase Console."
            firestoreError.code == FirebaseFirestoreException.Code.PERMISSION_DENIED ->
                "Akses ditolak oleh server. Periksa Rules Firestore."
            firestoreError.code == FirebaseFirestoreException.Code.UNAVAILABLE -> "Tidak ada koneksi internet."
            firestoreError.code == FirebaseFirestoreException.Code.UNAUTHENTICATED -> "Sesi akun berakhir. Keluar lalu masuk lagi."
            else -> "Server menolak (${firestoreError.code.name})."
        }
    }

    private companion object {
        const val GOALS = "goals"
        const val TRANSACTIONS = "transactions"
        const val SYNCED_AT = "syncedAt"
        const val PAGE_SIZE = 300L
        const val MIN_FOREGROUND_GAP_MS = 3 * 60_000L
        const val SETTINGS = "settings"
        const val SETTINGS_DOC = "main"
        val Collections = listOf(GOALS, TRANSACTIONS)
        val SyncedAt: Map<String, Any?> = mapOf(SYNCED_AT to FieldValue.serverTimestamp())
        val Deleted: Map<String, Any?> = mapOf("deleted" to true)
    }
}

/**
 * Menerapkan satu dokumen transaksi dari server ke Room (dipanggil di dalam transaksi database).
 * false = dompet goal-nya belum ada di HP ini; dicoba lagi setelah goal-nya tertarik.
 */
internal suspend fun applyRemoteTransaction(dao: SyncDao, uuid: String, doc: Map<String, Any?>): Boolean {
    if (doc["deleted"] == true) {
        dao.deleteTransaction(uuid)
        dao.insertTombstone(DeletedTransactionEntity(uuid, pushed = true))
        return true
    }
    // Dihapus di HP ini tetapi penandanya belum terkirim (mis. dihapus saat dokumennya sedang diunggah):
    // hapus lokal menang, penandanya akan menimpa dokumen ini di server.
    if (dao.hasPendingDelete(uuid)) return true
    dao.deleteTombstone(uuid) // dipulihkan (Urungkan) di HP lain
    val walletId = dao.walletId(doc["walletUuid"] as String) ?: return false
    val categoryId = (doc["categoryUuid"] as String?)?.let { dao.categoryId(it) }
    dao.insertTransaction(transactionFromDoc(uuid, doc, walletId, categoryId))
    return true
}
