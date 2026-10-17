package com.liezert.itungen.ui

import android.content.Context
import android.content.res.Configuration
import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kyant.backdrop.backdrops.emptyBackdrop
import com.liezert.glass.components.GlassOverlayHost
import com.liezert.glass.components.LocalGlassBackdrop
import com.liezert.glass.components.LocalGlassOverlayHost
import com.liezert.itungen.R
import com.liezert.itungen.domain.model.AmountKind
import com.liezert.itungen.domain.model.Goal
import com.liezert.itungen.ui.components.amountDescription
import com.liezert.itungen.ui.components.dateLocale
import com.liezert.itungen.ui.goals.GoalsContent
import com.liezert.itungen.ui.theme.ItungEnTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Locale

/** Bahasa app: teks, nominal (tetap Rupiah), dan tanggal mengikuti bahasa yang dipilih; berganti langsung di layar. */
@RunWith(AndroidJUnit4::class)
class LanguageTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val app = ApplicationProvider.getApplicationContext<Context>()

    private fun inLanguage(tag: String): Context =
        app.createConfigurationContext(Configuration(app.resources.configuration).apply { setLocale(Locale.forLanguageTag(tag)) })

    @Test
    fun resources_followLanguage_amountsStayRupiah() {
        val id = inLanguage("id")
        val en = inLanguage("en")
        assertEquals("Beranda" to "Home", id.getString(R.string.tab_home) to en.getString(R.string.tab_home))
        assertEquals("Pengeluaran Rp50.000", id.amountDescription(50_000, AmountKind.EXPENSE))
        assertEquals("Expense Rp50.000", en.amountDescription(50_000, AmountKind.EXPENSE))
        assertEquals("3 transactions this month", en.resources.getQuantityString(R.plurals.tx_this_month, 3, 3))
        assertEquals("1 transaction this month", en.resources.getQuantityString(R.plurals.tx_this_month, 1, 1))
        assertEquals("id" to "en", id.dateLocale().language.replace("in", "id") to en.dateLocale().language)
        // Bahasa sistem yang tidak didukung (mis. Prancis) → teks Indonesia, tanggal ikut Indonesia.
        assertEquals("Beranda", inLanguage("fr").getString(R.string.tab_home))
    }

    @Test
    fun switchingLanguage_changesScreenText() {
        var tag by mutableStateOf("id")
        compose.setContent {
            val context = inLanguage(tag)
            CompositionLocalProvider(LocalContext provides context, LocalConfiguration provides context.resources.configuration) {
                ItungEnTheme {
                    CompositionLocalProvider(LocalGlassBackdrop provides emptyBackdrop(), LocalGlassOverlayHost provides GlassOverlayHost()) {
                        GoalsContent(goals = listOf(Goal("Sepeda", saved = 900_000, target = 800_000, id = 1)), onAddGoal = {})
                    }
                }
            }
        }
        compose.onNodeWithText("Target Tabungan").assertExists()
        compose.onNodeWithText("Tambah Target").assertExists()

        tag = "en"
        compose.onNodeWithText("Savings Goals").assertExists()
        compose.onNodeWithText("Add goal").assertExists()
        compose.onNodeWithText("Target Tabungan").assertDoesNotExist()
        compose.onNodeWithText("Sepeda").assertExists() // nama goal milik pengguna tidak diterjemahkan
    }
}
