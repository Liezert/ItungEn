package com.liezert.itungen.ui.components

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Satu aksi yang masih bisa diurungkan. Identitas per objek: dua hapus dengan label sama tetap terpisah. */
class UndoEntry(val label: String, val undo: suspend () -> Unit)

/**
 * Tumpukan Urungkan: setiap aksi punya waktu [millis] sendiri, dan Urungkan membatalkan yang terbaru dulu.
 * Hapus dua kali dalam 5 detik → keduanya tetap bisa diurungkan satu per satu.
 */
class UndoStack(
    /** Untuk waktu kedaluwarsa (mis. viewModelScope). */
    private val timerScope: CoroutineScope,
    /** Untuk menjalankan Urungkan sampai selesai walau layar ditinggalkan. */
    private val workScope: CoroutineScope,
    private val millis: Long = 5_000,
) {
    private val _entries = MutableStateFlow<List<UndoEntry>>(emptyList())
    /** Terbaru di akhir. */
    val entries: StateFlow<List<UndoEntry>> = _entries.asStateFlow()

    fun push(label: String, undo: suspend () -> Unit) {
        val entry = UndoEntry(label, undo)
        _entries.update { it + entry }
        timerScope.launch {
            delay(millis)
            _entries.update { it - entry }
        }
    }

    fun undoLatest() {
        val entry = _entries.value.lastOrNull() ?: return
        _entries.update { it - entry }
        workScope.launch { entry.undo() }
    }
}

/** Pesan kapsul Urungkan untuk tumpukan: label terbaru, plus jumlah lain yang masih bisa diurungkan. */
fun List<UndoEntry>.toastMessage(): String? {
    val latest = lastOrNull() ?: return null
    return if (size > 1) "${latest.label} (+${size - 1})" else latest.label
}
