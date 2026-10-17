package com.liezert.itungen.data.backup

import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipException
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/*
 * Berkas cadangan ZIP: [JsonEntry] (isi sama dengan cadangan JSON lama, ditambah field "image" per goal) dan gambar
 * goal di [ImagesDir]. Nama di dalam ZIP tidak pernah dipakai sebagai path di HP: gambar disimpan ulang dengan nama
 * baru oleh GoalImageStore, jadi entri seperti "../../x" (zip-slip) tidak bisa menulis ke luar folder gambar.
 * Nama yang tidak lolos [safeImageName] tetap dilewati.
 */

internal const val JsonEntry = "backup.json"
internal const val ImagesDir = "images/"

/** Isi cadangan melebihi [ArchiveLimits]: bukan cadangan ItungEn yang wajar (mis. zip bomb). */
class BackupTooLargeException : Exception()

/**
 * Batas byte yang benar-benar diekstrak, bukan ukuran yang diklaim header ZIP. Cadangan normal jauh di bawahnya:
 * gambar goal ≤ 1080 px WebP (puluhan sampai ratusan KB), JSON 10.000 transaksi sekitar 3 MB.
 */
data class ArchiveLimits(
    val maxEntryBytes: Long = 32L shl 20,
    val maxTotalBytes: Long = 128L shl 20,
    val maxEntries: Int = 1_000,
)

private val ZipMagic = byteArrayOf(0x50, 0x4B, 0x03, 0x04)

/** [header] = byte pertama berkas. Bukan ZIP ("PK\u0003\u0004") = cadangan JSON lama. */
fun isZip(header: ByteArray) = header.size >= ZipMagic.size && header.copyOf(ZipMagic.size).contentEquals(ZipMagic)

/** Nama gambar langsung di bawah images/: huruf, angka, titik, garis bawah, strip; tanpa folder, tidak diawali titik. */
fun safeImageName(entryName: String): String? =
    entryName.removePrefix(ImagesDir).takeIf { entryName.startsWith(ImagesDir) && SafeName.matches(it) }

private val SafeName = Regex("[A-Za-z0-9_-][A-Za-z0-9._-]{0,127}")

/** Tulis cadangan ([images] = nama → file); gambar yang file-nya sudah tidak ada dilewati. */
fun writeArchive(out: OutputStream, json: String, images: Map<String, File>) {
    ZipOutputStream(out).use { zip ->
        zip.putNextEntry(ZipEntry(JsonEntry))
        zip.write(json.toByteArray())
        zip.closeEntry()
        for ((name, file) in images) {
            if (!file.isFile) continue
            zip.putNextEntry(ZipEntry(ImagesDir + name))
            file.inputStream().use { it.copyTo(zip) }
            zip.closeEntry()
        }
    }
}

/**
 * Baca cadangan ZIP: mengembalikan teks [JsonEntry] (null bila tidak ada) dan memanggil [onImage] untuk tiap gambar
 * bernama aman, satu per satu (gambar tidak ditahan di memori bersamaan). Entri lain dilewati.
 * @throws BackupTooLargeException bila melewati [limits]
 * @throws InvalidBackupException bila ZIP ditolak sistem (Android 14+ menolak nama berisi ".." atau diawali "/")
 */
suspend fun readArchive(
    input: InputStream,
    limits: ArchiveLimits = ArchiveLimits(),
    onImage: suspend (name: String, bytes: ByteArray) -> Unit,
): String? {
    var json: String? = null
    var total = 0L
    var entries = 0
    try {
        ZipInputStream(input).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                if (++entries > limits.maxEntries) throw BackupTooLargeException()
                // Entri yang dilewati tetap dihitung: membaca melewatinya juga mengekstrak isinya.
                val bytes = zip.readAtMost(minOf(limits.maxEntryBytes, limits.maxTotalBytes - total))
                total += bytes.size
                val image = safeImageName(entry.name)
                when {
                    entry.isDirectory -> Unit
                    entry.name == JsonEntry -> json = bytes.decodeToString()
                    image != null -> onImage(image, bytes)
                }
            }
        }
    } catch (_: ZipException) {
        throw InvalidBackupException()
    }
    return json
}

private fun InputStream.readAtMost(max: Long): ByteArray {
    val out = ByteArrayOutputStream()
    val buffer = ByteArray(64 * 1024)
    while (true) {
        val n = read(buffer)
        if (n < 0) return out.toByteArray()
        if (out.size() + n > max) throw BackupTooLargeException()
        out.write(buffer, 0, n)
    }
}
