package com.liezert.itungen.ui.settings

import android.app.LocaleManager
import android.content.Context
import android.os.LocaleList

/** Pilihan bahasa di Pengaturan. [SYSTEM] = ikut bahasa HP (bila tidak didukung, teks jatuh ke Indonesia). */
enum class AppLanguage(val tag: String?) { SYSTEM(null), INDONESIAN("id"), ENGLISH("en") }

/**
 * Bahasa per-app bawaan Android 13+ (LocaleManager, tanpa library): pilihan ikut tercatat di Pengaturan sistem
 * (lewat res/xml/locales_config.xml), dan sistem membuat ulang Activity sehingga teks langsung berganti. State
 * yang disimpan (rute navigasi, isian form, ViewModel) ikut bertahan seperti saat rotasi.
 */
object AppLanguages {
    fun current(context: Context): AppLanguage {
        val locales = context.getSystemService(LocaleManager::class.java).applicationLocales
        if (locales.isEmpty) return AppLanguage.SYSTEM
        return when (locales[0].language) {
            "en" -> AppLanguage.ENGLISH
            "id", "in" -> AppLanguage.INDONESIAN // "in" = kode lama bahasa Indonesia di sebagian versi Java
            else -> AppLanguage.SYSTEM
        }
    }

    fun set(context: Context, language: AppLanguage) {
        context.getSystemService(LocaleManager::class.java).applicationLocales =
            language.tag?.let { LocaleList.forLanguageTags(it) } ?: LocaleList.getEmptyLocaleList()
    }
}
