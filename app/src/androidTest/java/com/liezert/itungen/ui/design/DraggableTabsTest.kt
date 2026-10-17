package com.liezert.itungen.ui.design

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.liezert.itungen.ui.design.themes.common.DraggableTabs
import com.liezert.itungen.ui.design.themes.common.FlatTab
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Navbar tema tanpa kaca mempertahankan perilaku navbar kaca: indikator ditahan lalu digeser ke tab lain. */
@RunWith(AndroidJUnit4::class)
class DraggableTabsTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun holdAndDrag_selectsTab_andTapOnActiveTabReselects() {
        var selected by mutableIntStateOf(0)
        var reselected = -1
        compose.setContent {
            DraggableTabs({ selected }, { selected = it }, { reselected = it }, 3, Modifier.size(300.dp, 64.dp).testTag("tabs"), Modifier) {
                repeat(3) { i -> FlatTab({ selected = i }, Modifier, i == selected) { Text("T$i") } }
            }
        }
        val tabs = compose.onNodeWithTag("tabs")
        // Tahan di tab pertama, geser ke tab ketiga bertahap (tiap langkah diberi frame agar spring mengikuti jari).
        tabs.performTouchInput { down(Offset(50.dp.toPx(), 32.dp.toPx())) }
        repeat(4) {
            tabs.performTouchInput { moveBy(Offset(50.dp.toPx(), 0f)) }
            compose.waitForIdle()
        }
        tabs.performTouchInput { up() }
        compose.waitForIdle()
        assertEquals(2, selected)

        tabs.performTouchInput { click(Offset(250.dp.toPx(), 32.dp.toPx())) }
        compose.waitForIdle()
        assertEquals(2, reselected)
    }
}
