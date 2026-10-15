package com.liezert.itungen.ui

import androidx.compose.material3.Text
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
}
