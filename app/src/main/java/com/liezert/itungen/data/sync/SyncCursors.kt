package com.liezert.itungen.data.sync

import android.content.SharedPreferences
import androidx.core.content.edit

/**
 * Posisi tarik terakhir: waktu server (detik + nano) dan id dokumen terakhir yang diterapkan. Satu batch unggah
 * memberi waktu server yang sama ke ratusan dokumen, jadi id dokumen ikut menjadi kunci urutan agar halaman
 * berikutnya mulai tepat setelahnya (bukan melompati sisa dokumen bertanggal sama).
 */
data class PullCursor(val seconds: Long, val nanos: Int, val docId: String) {
    companion object {
        val Start = PullCursor(0, 0, "")
    }
}

/** Kursor tarik sinkron di SharedPreferences, terpisah per akun agar ganti akun mulai dari awal. */
class SyncCursors(private val prefs: SharedPreferences) {
    fun load(uid: String, collection: String): PullCursor {
        val key = key(uid, collection)
        // Kursor versi lama tanpa id dokumen: "" = mulai dari dokumen pertama bertanggal sama (diterapkan ulang, aman).
        return PullCursor(prefs.getLong("$key:s", 0), prefs.getInt("$key:n", 0), prefs.getString("$key:id", null).orEmpty())
    }

    fun save(uid: String, collection: String, cursor: PullCursor) {
        val key = key(uid, collection)
        prefs.edit {
            putLong("$key:s", cursor.seconds)
            putInt("$key:n", cursor.nanos)
            putString("$key:id", cursor.docId)
        }
    }

    /** Keluar akun: data lokal dihapus, jadi masuk lagi harus menarik semuanya dari awal. */
    fun clear(uid: String, collections: List<String>) {
        prefs.edit {
            for (collection in collections) {
                val key = key(uid, collection)
                remove("$key:s")
                remove("$key:n")
                remove("$key:id")
            }
        }
    }

    private fun key(uid: String, collection: String) = "sync:$uid:$collection"
}
