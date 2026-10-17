package com.liezert.itungen.ui.home

import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kyant.backdrop.backdrops.emptyBackdrop
import com.liezert.glass.components.LocalGlassBackdrop
import com.liezert.itungen.domain.usecase.BudgetPlan
import com.liezert.itungen.domain.usecase.computeBudget
import com.liezert.itungen.ui.theme.ItungEnTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate
import java.time.ZoneOffset

/** Beranda: Total Saldo paling atas, di atas Jatah hari ini. */
@RunWith(AndroidJUnit4::class)
class HomeLayoutTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun totalBalance_isAboveDailyAllowance() {
        val today = LocalDate.of(2026, 10, 17)
        val plan = BudgetPlan(income = 3_000_000, cycleStartDay = 25, fixedBills = 500_000, savingsTarget = 300_000)
        val state = HomeUiState(today = today, zone = ZoneOffset.UTC, loading = false, balance = -50_000, budget = computeBudget(plan, emptyList(), 0, today))
        compose.setContent {
            ItungEnTheme {
                CompositionLocalProvider(LocalGlassBackdrop provides emptyBackdrop()) {
                    HomeContent(state, onHistory = {}, onSeeAll = {})
                }
            }
        }
        val balance = compose.onNodeWithText("Total Saldo").fetchSemanticsNode().boundsInRoot.top
        val allowance = compose.onNodeWithText("Jatah hari ini", substring = true).fetchSemanticsNode().boundsInRoot.top
        assertTrue("saldo $balance, jatah $allowance", balance < allowance)
        compose.onNodeWithText("-Rp50.000", substring = true, useUnmergedTree = true).assertExists() // negatif = tanda minus
    }
}
