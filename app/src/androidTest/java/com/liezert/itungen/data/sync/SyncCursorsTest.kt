package com.liezert.itungen.data.sync

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SyncCursorsTest {
    private val prefs = ApplicationProvider.getApplicationContext<Context>().getSharedPreferences("cursor-test", Context.MODE_PRIVATE)

    /**
     * Bug: keluar akun menghapus data lokal tetapi kursornya tertinggal, jadi saat masuk lagi dengan akun yang sama
     * tarikan mulai dari posisi lama dan catatan yang dihapus tadi tidak pernah kembali.
     */
    @Test
    fun clear_startsOverOnNextSignIn() {
        val cursors = SyncCursors(prefs)
        cursors.save("u1", "transactions", PullCursor(1_700_000_000, 5, "doc-9"))
        cursors.save("u2", "transactions", PullCursor(42, 0, "x"))

        cursors.clear("u1", listOf("goals", "transactions"))

        assertEquals(PullCursor.Start, cursors.load("u1", "transactions"))
        assertEquals(PullCursor(42, 0, "x"), cursors.load("u2", "transactions")) // akun lain tidak tersentuh
    }
}
