package com.liezert.itungen.ui

import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import com.liezert.glass.components.GlassOverlayHost
import com.liezert.glass.components.LocalGlassBackdrop
import com.liezert.glass.components.LocalGlassOverlayHost
import com.liezert.itungen.domain.model.Category
import com.liezert.itungen.domain.model.CategoryType
import com.liezert.itungen.domain.model.Transaction
import com.liezert.itungen.domain.model.TransactionType
import com.liezert.itungen.ui.transactions.TransactionsContent
import com.liezert.itungen.ui.transactions.TransactionsUiState
import org.junit.Assert.assertEquals
import java.time.LocalDate
import java.time.ZoneOffset
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kyant.backdrop.backdrops.emptyBackdrop
import com.liezert.glass.components.LiquidBottomTab
import com.liezert.glass.components.LiquidBottomTabs
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Semantik untuk TalkBack. */
@RunWith(AndroidJUnit4::class)
class AccessibilityTest {
    @get:Rule
    val compose = createComposeRule()

    /** Bug: tab navbar tidak menyatakan mana yang aktif, jadi TalkBack hanya membaca "Tab, Insight". */
    @Test
    fun navbarTabs_announceSelection() {
        compose.setContent {
            LiquidBottomTabs(selectedTabIndex = { 1 }, onTabSelected = {}, backdrop = emptyBackdrop(), tabsCount = 2) {
                LiquidBottomTab({}, selected = false) { Text("Beranda") }
                LiquidBottomTab({}, selected = true) { Text("Insight") }
            }
        }
        compose.onNodeWithText("Insight").assertIsSelected()
        compose.onNodeWithText("Beranda").assertIsNotSelected()
    }

    /**
     * Bug: setelah geser-hapus lalu Urungkan, baris yang sama (key sama) memakai lagi status geser "terhapus" yang
     * disimpan LazyColumn, sehingga langsung terhapus lagi tanpa disadari.
     */
    @Test
    fun undoneRow_isNotDeletedAgain() {
        val food = Category(1, "Makanan", "food", 0, CategoryType.EXPENSE)
        val today = LocalDate.of(2026, 10, 16)
        val tx = Transaction(7, 1, food, TransactionType.EXPENSE, 15_000, "Makan siang", today)
        val older = Transaction(6, 1, food, TransactionType.EXPENSE, 9_000, "Sarapan", today.minusDays(1))
        var rows by mutableStateOf(listOf(tx, older))
        var deletes = 0
        compose.setContent {
            CompositionLocalProvider(LocalGlassBackdrop provides emptyBackdrop(), LocalGlassOverlayHost provides GlassOverlayHost()) {
                TransactionsContent(
                    TransactionsUiState(today, ZoneOffset.UTC, loading = false, transactions = rows, hasAny = true),
                    onBack = {},
                    onDelete = {
                        deletes++
                        rows = rows - it
                    },
                )
            }
        }
        compose.onNodeWithText("Makan siang").performTouchInput { swipeLeft() }
        compose.waitForIdle()
        assertEquals(1, deletes)

        rows = listOf(tx, older) // Urungkan: baris yang sama kembali (di atas baris yang sedang tampil)
        compose.waitForIdle()
        compose.onAllNodes(hasScrollToIndexAction()).onLast().performScrollToIndex(0) // LazyColumn (yang pertama: chip filter)
        compose.waitForIdle()
        compose.onNodeWithText("Makan siang").assertExists()
        assertEquals(1, deletes)
    }
}
