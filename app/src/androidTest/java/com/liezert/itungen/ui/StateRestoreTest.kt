package com.liezert.itungen.ui

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kyant.backdrop.backdrops.emptyBackdrop
import com.liezert.glass.components.GlassOverlayHost
import com.liezert.glass.components.LocalGlassBackdrop
import com.liezert.glass.components.LocalGlassOverlayHost
import com.liezert.itungen.ui.goals.GoalsScreen
import com.liezert.itungen.ui.theme.ItungEnTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Audit B12: rotasi / proses dibuat ulang dulu menutup sheet Buat Target dan membuang isiannya. */
@RunWith(AndroidJUnit4::class)
class StateRestoreTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun createGoalSheet_andItsInput_surviveRecreation() {
        val restoration = StateRestorationTester(compose)
        restoration.setContent {
            val host = remember { GlassOverlayHost() }
            ItungEnTheme {
                CompositionLocalProvider(LocalGlassBackdrop provides emptyBackdrop(), LocalGlassOverlayHost provides host) {
                    Box(Modifier.fillMaxSize()) {
                        GoalsScreen(onOpenGoal = {})
                        host.Content()
                    }
                }
            }
        }
        compose.onNodeWithText("Tambah Target").performClick()
        compose.onAllNodes(hasSetTextAction()).onFirst().performTextInput("Sepeda gunung")

        restoration.emulateSavedInstanceStateRestore()

        assertTrue(compose.onAllNodesWithText("Buat Target").fetchSemanticsNodes().isNotEmpty()) // sheet masih terbuka
        compose.onNodeWithText("Sepeda gunung").assertExists()
    }
}
