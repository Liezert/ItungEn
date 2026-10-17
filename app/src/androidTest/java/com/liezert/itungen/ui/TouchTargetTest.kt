package com.liezert.itungen.ui

import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kyant.backdrop.backdrops.emptyBackdrop
import com.liezert.glass.components.LocalGlassBackdrop
import com.liezert.itungen.ui.home.HomeContent
import com.liezert.itungen.ui.home.HomeUiState
import com.liezert.itungen.ui.insight.InsightContent
import com.liezert.itungen.ui.insight.InsightUiState
import com.liezert.itungen.ui.theme.ItungEnTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneOffset

/** Audit B3–B5: kontrol kecil (ikon mata, "Lihat Semua", tombol bulan) dulu di bawah area sentuh minimum 48 dp. */
@RunWith(AndroidJUnit4::class)
class TouchTargetTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val today = LocalDate.of(2026, 10, 17)

    @Test
    fun homeControls_areAtLeast48dp() {
        compose.setContent {
            ItungEnTheme {
                CompositionLocalProvider(LocalGlassBackdrop provides emptyBackdrop()) {
                    HomeContent(HomeUiState(today = today, zone = ZoneOffset.UTC, loading = false, balance = 50_000), onHistory = {}, onSeeAll = {})
                }
            }
        }
        compose.onNodeWithContentDescription("Sembunyikan saldo").assertHeightIsAtLeast(48.dp).assertWidthIsAtLeast(48.dp)
        compose.onNodeWithText("Lihat Semua").assertHeightIsAtLeast(48.dp)
    }

    @Test
    fun insightMonthButtons_areAtLeast48dp() {
        compose.setContent {
            ItungEnTheme {
                CompositionLocalProvider(LocalGlassBackdrop provides emptyBackdrop()) {
                    InsightContent(
                        InsightUiState(YearMonth.from(today), loading = false, canGoBack = true, today = today),
                        onType = {}, onPrevious = {}, onNext = {},
                    )
                }
            }
        }
        compose.onNodeWithContentDescription("Bulan sebelumnya").assertHeightIsAtLeast(48.dp).assertWidthIsAtLeast(48.dp)
        compose.onNodeWithContentDescription("Bulan berikutnya").assertHeightIsAtLeast(48.dp).assertWidthIsAtLeast(48.dp)
    }
}
