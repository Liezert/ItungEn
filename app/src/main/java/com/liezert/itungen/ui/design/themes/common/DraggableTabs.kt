package com.liezert.itungen.ui.design.themes.common

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.selection.selectable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastCoerceIn
import androidx.compose.ui.util.fastRoundToInt
import com.liezert.glass.utils.DampedDragAnimation
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.sign

/**
 * Navbar/segmen tema tanpa kaca dengan perilaku LiquidBottomTabs: indikator bisa ditahan lalu digeser ke tab lain
 * (spring, efek jelly dari kecepatan, panel ikut terdorong sedikit); ketuk tab aktif = [onTabReselected].
 * [modifier] = panel tema (tinggi, latar, tepi, padding), [indicator] = gaya indikator tab aktif. Isinya [FlatTab].
 */
@Composable
fun DraggableTabs(
    selectedTabIndex: () -> Int,
    onTabSelected: (Int) -> Unit,
    onTabReselected: (Int) -> Unit,
    tabsCount: Int,
    modifier: Modifier,
    indicator: Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    val onTabSelected by rememberUpdatedState(onTabSelected)
    val onTabReselected by rememberUpdatedState(onTabReselected)
    val selectedTabIndex by rememberUpdatedState(selectedTabIndex)
    val isLtr = LocalLayoutDirection.current == LayoutDirection.Ltr
    val scope = rememberCoroutineScope()
    val offset = remember { Animatable(0f) }

    BoxWithConstraints(
        Modifier
            .graphicsLayer {
                val fraction = (offset.value / size.width).fastCoerceIn(-1f, 1f)
                translationX = 4.dp.toPx() * fraction.sign * EaseOut.transform(abs(fraction))
            }
            .then(modifier)
            // Seluruh panel menerima sentuhan, agar sela antartab tidak meneruskan ketukan ke konten di belakangnya.
            .pointerInput(Unit) { awaitPointerEventScope { while (true) awaitPointerEvent() } },
        contentAlignment = Alignment.CenterStart,
    ) {
        val tabWidth = constraints.maxWidth.toFloat() / tabsCount
        var currentIndex by remember { mutableIntStateOf(selectedTabIndex()) }
        val drag = remember(scope) {
            DampedDragAnimation(
                animationScope = scope,
                initialValue = selectedTabIndex().toFloat(),
                valueRange = 0f..(tabsCount - 1).toFloat(),
                visibilityThreshold = 0.001f,
                initialScale = 1f,
                // Lebih kecil dari kaca (78/56): indikator datar yang membesar banyak keluar dari panelnya.
                pressedScale = 1.08f,
                onDragStarted = {},
                onDragStopped = {
                    val target = targetValue.fastRoundToInt().fastCoerceIn(0, tabsCount - 1)
                    if (target == currentIndex) onTabReselected(target)
                    currentIndex = target
                    animateToValue(target.toFloat())
                    scope.launch { offset.animateTo(0f, spring(1f, 300f, 0.5f)) }
                },
                onDrag = { _, amount ->
                    updateValue((targetValue + amount.x / tabWidth * if (isLtr) 1f else -1f).fastCoerceIn(0f, (tabsCount - 1).toFloat()))
                    scope.launch { offset.snapTo(offset.value + amount.x) }
                },
            )
        }
        LaunchedEffect(Unit) { snapshotFlow { selectedTabIndex() }.collectLatest { currentIndex = it } }
        LaunchedEffect(drag) {
            snapshotFlow { currentIndex }.drop(1).collectLatest {
                drag.animateToValue(it.toFloat())
                onTabSelected(it)
            }
        }
        val position = { (if (isLtr) drag.value else tabsCount - 1 - drag.value) * tabWidth }

        // Indikator di belakang isi tab, lalu tab, lalu penangkap gestur tak terlihat di atas tab aktif (seperti kaca).
        Box(
            Modifier
                .graphicsLayer {
                    translationX = position()
                    val velocity = drag.velocity / 10f
                    scaleX = drag.scaleX / (1f - (velocity * 0.75f).fastCoerceIn(-0.2f, 0.2f))
                    scaleY = drag.scaleY * (1f - (velocity * 0.25f).fastCoerceIn(-0.2f, 0.2f))
                }
                .fillMaxHeight()
                .fillMaxWidth(1f / tabsCount)
                .then(indicator),
        )
        Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically, content = content)
        Box(
            Modifier
                .graphicsLayer { translationX = position() }
                .then(drag.modifier)
                .fillMaxHeight()
                .fillMaxWidth(1f / tabsCount),
        )
    }
}

/** Tab untuk [DraggableTabs]; tanda tab aktif digambar indikator, bukan tab ini. */
@Composable
fun RowScope.FlatTab(onClick: () -> Unit, modifier: Modifier, selected: Boolean, content: @Composable ColumnScope.() -> Unit) =
    Column(
        modifier
            .weight(1f)
            .fillMaxHeight()
            .selectable(selected, interactionSource = null, indication = null, role = Role.Tab, onClick = onClick),
        verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
        content = content,
    )
