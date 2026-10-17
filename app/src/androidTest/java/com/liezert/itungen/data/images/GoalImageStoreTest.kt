package com.liezert.itungen.data.images

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/** Fase 2: gambar dari Photo Picker disalin ke penyimpanan app (bukan URI mentah), dikecilkan, dan dibersihkan. */
@RunWith(AndroidJUnit4::class)
class GoalImageStoreTest {
    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()
    private lateinit var dir: File
    private lateinit var store: GoalImageStore

    @Before
    fun setUp() {
        dir = File(context.cacheDir, "goal_images_test").apply { deleteRecursively() }
        store = GoalImageStore(dir)
    }

    @After
    fun tearDown() {
        dir.deleteRecursively()
    }

    private fun picked(width: Int, height: Int): Uri {
        val source = File(context.cacheDir, "picked.png")
        source.outputStream().use { Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).compress(Bitmap.CompressFormat.PNG, 100, it) }
        return Uri.fromFile(source)
    }

    @Test
    fun save_copiesAndShrinks() = runBlocking<Unit> {
        val name = store.save(context.contentResolver, picked(3000, 2000))
        val file = store.file(name)
        assertTrue(file.exists())
        assertEquals(dir, file.parentFile)
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.path, bounds)
        assertEquals(1080, bounds.outWidth)
        assertEquals(720, bounds.outHeight)
    }

    @Test
    fun delete_andOrphanSweep_leaveNoStrayFiles() = runBlocking<Unit> {
        val kept = store.save(context.contentResolver, picked(100, 100))
        val replaced = store.save(context.contentResolver, picked(100, 100))
        val removed = store.save(context.contentResolver, picked(100, 100))
        store.delete(removed)
        assertFalse(store.file(removed).exists())
        store.deleteOrphans(setOf(kept))
        assertTrue(store.file(kept).exists())
        assertFalse(store.file(replaced).exists())
        store.delete(null) // tanpa gambar: tidak apa-apa
    }
}
