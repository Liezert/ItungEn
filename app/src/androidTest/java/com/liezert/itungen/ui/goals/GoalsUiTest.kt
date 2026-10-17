package com.liezert.itungen.ui.goals

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kyant.backdrop.backdrops.emptyBackdrop
import com.liezert.glass.components.GlassOverlayHost
import com.liezert.glass.components.LocalGlassBackdrop
import com.liezert.glass.components.LocalGlassOverlayHost
import com.liezert.itungen.domain.model.Goal
import com.liezert.itungen.domain.model.GoalDeposit
import com.liezert.itungen.ui.theme.ItungEnTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

/** Daftar Goals: indikator tercapai terbaca TalkBack, kartu membuka detail, dan Kembali sistem kembali ke daftar. */
@RunWith(AndroidJUnit4::class)
class GoalsUiTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val bike = Goal("Sepeda", saved = 900_000, target = 800_000, id = 1)
    private val laptop = Goal("Laptop", saved = 1_000_000, target = 2_000_000, id = 2)
    private val deposits = listOf(GoalDeposit(600_000, LocalDate.of(2026, 10, 12)), GoalDeposit(300_000, LocalDate.of(2026, 10, 1)))

    private fun show(content: @Composable () -> Unit) = compose.setContent {
        val host = remember { GlassOverlayHost() }
        ItungEnTheme {
            CompositionLocalProvider(LocalGlassBackdrop provides emptyBackdrop(), LocalGlassOverlayHost provides host) {
                Box(Modifier.fillMaxSize()) {
                    content()
                    host.Content()
                }
            }
        }
    }

    @Test
    fun onlyAchievedGoal_showsTercapaiChip() {
        show { GoalsContent(goals = listOf(bike, laptop)) }
        compose.onAllNodesWithContentDescription("Target tercapai", useUnmergedTree = true).assertCountEquals(1)
        compose.onNodeWithText("50% tercapai", useUnmergedTree = true).assertExists()
    }

    @Test
    fun tapCard_opensDetailWithHistory_andSystemBackReturns() {
        show {
            val nav = rememberNavController()
            NavHost(nav, startDestination = "goals") {
                composable("goals") { GoalsContent(goals = listOf(bike, laptop), onOpenGoal = { nav.navigate("goal/${it.id}") }) }
                composable("goal/{id}", arguments = listOf(navArgument("id") { type = NavType.LongType })) { entry ->
                    val goal = listOf(bike, laptop).first { it.id == entry.arguments!!.getLong("id") }
                    GoalDetailContent(goal, if (goal == bike) deposits else emptyList(), onBack = { nav.popBackStack() })
                }
            }
        }

        compose.onNodeWithText("Sepeda").performClick()
        compose.onNodeWithText("RIWAYAT UANG MASUK").assertExists()
        // AmountText membacakan nominalnya lewat contentDescription.
        compose.onNodeWithContentDescription("Rp600.000", substring = true, useUnmergedTree = true).assertExists()
        compose.onNodeWithContentDescription("Rp300.000", substring = true, useUnmergedTree = true).assertExists()

        compose.runOnUiThread { compose.activity.onBackPressedDispatcher.onBackPressed() }
        compose.onNodeWithText("Target Tabungan").assertExists()
        compose.onNodeWithText("RIWAYAT UANG MASUK").assertDoesNotExist()

        compose.onNodeWithText("Laptop").performClick()
        compose.onNodeWithText("Belum ada uang masuk", substring = true).assertExists()
    }
}
