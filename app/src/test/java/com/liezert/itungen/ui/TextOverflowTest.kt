package com.liezert.itungen.ui

import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File

/**
 * Bug B7: teks satu baris tanpa `overflow` dipotong diam-diam ("Makan siang warteg" tampil "Makan siang").
 * Setiap `maxLines = 1` di app dan :glass harus menyertakan `overflow` (atau `autoSize` yang mengecilkan teks).
 */
class TextOverflowTest {
    @Test
    fun everySingleLineText_hasOverflow() {
        val roots = listOf(File("src/main/java"), File("../glass/src/main/java"))
        val offenders = roots.flatMap { root -> root.walk().filter { it.extension == "kt" }.toList() }.flatMap { file ->
            val lines = file.readLines()
            lines.indices.filter { "maxLines = 1" in lines[it] }.mapNotNull { i ->
                // `overflow` ditulis setelah `maxLines` (baris yang sama atau dua berikutnya); `autoSize` bisa di sekitarnya.
                val after = lines.subList(i, minOf(lines.size, i + 3)).joinToString("\n")
                val around = lines.subList(maxOf(0, i - 3), minOf(lines.size, i + 3)).joinToString("\n")
                if ("overflow" in after || "autoSize" in around) null else "${file.name}:${i + 1}"
            }
        }
        assertEquals(emptyList<String>(), offenders)
    }
}
