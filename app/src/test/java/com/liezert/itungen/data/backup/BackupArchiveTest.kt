package com.liezert.itungen.data.backup

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.file.Files
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/** Berkas cadangan ZIP: isi JSON + gambar goal, nama berbahaya dilewati, ukuran ekstraksi dibatasi. */
class BackupArchiveTest {
    private val image = byteArrayOf(1, 2, 3)

    private fun zip(vararg entries: Pair<String, ByteArray>): ByteArray = ByteArrayOutputStream().also { out ->
        ZipOutputStream(out).use { zip ->
            entries.forEach { (name, bytes) ->
                zip.putNextEntry(ZipEntry(name))
                zip.write(bytes)
                zip.closeEntry()
            }
        }
    }.toByteArray()

    private fun read(bytes: ByteArray, limits: ArchiveLimits = ArchiveLimits()): Pair<String?, Map<String, ByteArray>> = runBlocking {
        val images = mutableMapOf<String, ByteArray>()
        readArchive(ByteArrayInputStream(bytes), limits) { name, data -> images[name] = data } to images
    }

    @Test
    fun roundTrip_keepsJsonAndImages_skipsMissingFile() {
        val dir = Files.createTempDirectory("goal_images").toFile()
        val kept = File(dir, "goal_a.webp").apply { writeBytes(image) }
        val out = ByteArrayOutputStream()
        writeArchive(out, """{"app":"ItungEn"}""", mapOf("goal_a.webp" to kept, "goal_gone.webp" to File(dir, "goal_gone.webp")))

        val (json, images) = read(out.toByteArray())
        assertTrue(isZip(out.toByteArray()))
        assertEquals("""{"app":"ItungEn"}""", json)
        assertEquals(setOf("goal_a.webp"), images.keys) // file yang sudah terhapus dilewati, bukan galat
        assertArrayEquals(image, images["goal_a.webp"])
    }

    @Test
    fun legacyJson_isNotZip() {
        assertFalse(isZip("{\n  \"app\": \"ItungEn\"".toByteArray()))
        assertFalse(isZip(byteArrayOf(0x50, 0x4B)))
    }

    @Test
    fun zipWithoutJson_hasNoBackup() {
        assertNull(read(zip("images/goal_a.webp" to image)).first)
    }

    @Test
    fun dangerousNames_areSkipped() {
        val dangerous = listOf(
            "../evil.webp", "images/../../evil.webp", "/data/evil.webp", "images/sub/evil.webp",
            "images\\..\\evil.webp", "images/.hidden", "images/..", "evil.webp",
        )
        val (_, images) = read(zip(*(dangerous.map { it to image } + ("images/goal_ok.webp" to image)).toTypedArray()))
        assertEquals(setOf("goal_ok.webp"), images.keys)
        dangerous.forEach { assertNull(it, safeImageName(it)) }
        assertEquals("goal_1f2e.webp", safeImageName("images/goal_1f2e.webp"))
    }

    @Test
    fun limits_countExtractedBytes_notHeaderSizes() {
        // 2.000 byte nol terkompresi jadi belasan byte: batasnya harus dari hasil ekstraksi.
        assertThrows(BackupTooLargeException::class.java) { read(zip("images/a.webp" to ByteArray(2_000)), ArchiveLimits(maxEntryBytes = 1_000)) }
        assertThrows(BackupTooLargeException::class.java) {
            read(zip("images/a.webp" to ByteArray(600), "images/b.webp" to ByteArray(600)), ArchiveLimits(maxTotalBytes = 1_000))
        }
        // Entri yang dilewati (nama berbahaya) tetap dihitung.
        assertThrows(BackupTooLargeException::class.java) { read(zip("../a" to ByteArray(2_000)), ArchiveLimits(maxEntryBytes = 1_000)) }
        assertThrows(BackupTooLargeException::class.java) { read(zip("images/a.webp" to image, "images/b.webp" to image), ArchiveLimits(maxEntries = 1)) }
    }
}
