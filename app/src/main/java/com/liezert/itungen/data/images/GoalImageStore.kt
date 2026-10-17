package com.liezert.itungen.data.images

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.ByteBuffer
import java.util.UUID

/** Sisi terpanjang gambar produk setelah dikecilkan: cukup tajam untuk kartu selebar layar, tetap kecil di disk. */
const val MaxImageSide = 1080

private const val Quality = 85

/** Ukuran setelah sisi terpanjang dibatasi [max] (tidak pernah diperbesar), rasio tetap. */
fun scaledSize(width: Int, height: Int, max: Int = MaxImageSide): Pair<Int, Int> {
    val longest = maxOf(width, height)
    if (longest <= max) return width to height
    return (width.toLong() * max / longest).toInt().coerceAtLeast(1) to (height.toLong() * max / longest).toInt().coerceAtLeast(1)
}

/** File di folder gambar yang tidak dipakai goal mana pun ([referenced] = nama file goal yang masih tampil). */
fun orphanImages(files: List<String>, referenced: Set<String?>): List<String> = files.filter { it !in referenced }

/**
 * Gambar produk goal di penyimpanan internal app (`filesDir/goal_images`). Gambar dari Photo Picker disalin ke sini
 * (izin baca URI-nya bisa hilang), dikecilkan, dan dikompres WebP. Database menyimpan nama file saja, bukan path
 * lengkap. Tanpa jaringan dan tanpa izin penyimpanan.
 */
class GoalImageStore(private val dir: File) {
    fun file(name: String): File = File(dir, name)

    /** Salin [uri] menjadi file baru; mengembalikan namanya untuk disimpan di goal. */
    suspend fun save(resolver: ContentResolver, uri: Uri): String = save(ImageDecoder.createSource(resolver, uri))

    /**
     * Gambar dari berkas cadangan: didekode dan dikompres ulang seperti gambar dari galeri, jadi isi yang rusak atau
     * bukan gambar gagal di sini (bukan saat tampil) dan namanya selalu nama baru, bukan nama dari berkas cadangan.
     */
    suspend fun save(bytes: ByteArray): String = save(ImageDecoder.createSource(ByteBuffer.wrap(bytes)))

    private suspend fun save(source: ImageDecoder.Source): String = withContext(Dispatchers.IO) {
        val bitmap = ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
            val (width, height) = scaledSize(info.size.width, info.size.height)
            decoder.setTargetSize(width, height)
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
        }
        dir.mkdirs()
        val name = "goal_${UUID.randomUUID()}.webp"
        // Tulis ke file sementara lalu ganti nama: file setengah jadi tidak pernah dipakai goal.
        val temp = File(dir, "$name.tmp")
        temp.outputStream().use { bitmap.compress(Bitmap.CompressFormat.WEBP_LOSSY, Quality, it) }
        bitmap.recycle()
        check(temp.renameTo(file(name))) { "Gagal menyimpan gambar" }
        name
    }

    suspend fun delete(name: String?) {
        if (name == null) return
        withContext(Dispatchers.IO) { file(name).delete() }
    }

    /** Hapus file yang tidak dipakai goal mana pun (mis. sheet ditutup tanpa menyimpan, app tertutup di tengah jalan). */
    suspend fun deleteOrphans(referenced: Set<String?>) = withContext(Dispatchers.IO) {
        val files = dir.list()?.toList().orEmpty()
        orphanImages(files, referenced).forEach { file(it).delete() }
    }
}
