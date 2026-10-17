package com.liezert.itungen.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Setiap teks punya terjemahan Inggris dan tidak ada sisa terjemahan tanpa teks Indonesia-nya (lint
 * MissingTranslation juga dijadikan error). String translatable="false" (nama app, nama bahasa) dikecualikan.
 */
class StringsTranslationTest {
    private val names = Regex("""<(string|plurals) name="([^"]+)"([^>]*)>""")

    private fun names(path: String, translatableOnly: Boolean): Set<String> =
        names.findAll(File(path).readText())
            .filterNot { translatableOnly && "translatable=\"false\"" in it.groupValues[3] }
            .map { it.groupValues[2] }
            .toSet()

    @Test
    fun everyStringHasEnglish_andNoOrphans() {
        val indonesian = names("src/main/res/values/strings.xml", translatableOnly = true)
        val english = names("src/main/res/values-en/strings.xml", translatableOnly = false)
        assertTrue("terlalu sedikit string: ${indonesian.size}", indonesian.size > 300)
        assertEquals("belum diterjemahkan", emptySet<String>(), indonesian - english)
        assertEquals("terjemahan tanpa teks Indonesia", emptySet<String>(), english - indonesian)
    }

    @Test
    fun formatArgumentsMatchBetweenLanguages() {
        val arg = Regex("""%(\d+\$)?[sd]""")
        fun args(path: String) = Regex("""<string name="([^"]+)"[^>]*>(.*?)</string>""", RegexOption.DOT_MATCHES_ALL)
            .findAll(File(path).readText())
            .associate { it.groupValues[1] to arg.findAll(it.groupValues[2]).map { m -> m.value }.sorted().toList() }
        val id = args("src/main/res/values/strings.xml")
        val en = args("src/main/res/values-en/strings.xml")
        val mismatched = id.filter { (name, a) -> name in en && en[name] != a }.keys
        assertEquals("argumen format berbeda", emptySet<String>(), mismatched)
    }
}
