package com.liezert.glass.components

import android.os.Build
import android.provider.Settings
import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.liezert.glass.tokens.GlassColors
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch

// Angka §6.4, dikunci setelah uji di Realme 13+ (Tahap 2).
private val NavShape = RoundedCornerShape(50)
private val Padding = 4.dp
private const val PressedScale = 1.15f
private const val StretchPerVelocity = 0.04f // per (tab/detik)
private const val MaxStretch = 0.3f
private val PressSpring = spring<Float>(dampingRatio = 0.6f, stiffness = 500f)
private val FollowSpring = spring<Float>(dampingRatio = 1f, stiffness = 1500f)
private val SettleSpring = spring<Float>(dampingRatio = 0.7f, stiffness = 400f)

/** Satu tab: ikon di atas label, [accent] untuk tab aktif (§6.1: Beranda #FFAF4E). */
data class NavBarItem(
    val label: String,
    val icon: Painter,
    val accent: Color = GlassColors.NavActive,
)

/**
 * Floating navbar Glass Riil dengan indikator hold-and-drag.
 * Posisi seret hidup di sini saja; [onSelect] hanya dipanggil saat jari dilepas.
 */
@Composable
fun FloatingNavBar(
    items: List<NavBarItem>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    backdrop: Backdrop,
    modifier: Modifier = Modifier,
) {
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    val currentSelected by rememberUpdatedState(selectedIndex)
    val currentOnSelect by rememberUpdatedState(onSelect)

    val position = remember { Animatable(selectedIndex.toFloat()) } // dalam satuan tab
    val press = remember { Animatable(0f) }
    val zone by remember { derivedStateOf { position.value.roundToInt() } }
    val stretchEnabled = remember {
        Settings.Global.getFloat(view.context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) != 0f
    }

    // Navigasi dari luar (mis. tombol back) menggeser indikator juga.
    LaunchedEffect(selectedIndex) {
        if (press.targetValue == 0f) position.animateTo(selectedIndex.toFloat(), SettleSpring)
    }
    // Satu haptic per pergantian zona, hanya saat jari menempel.
    LaunchedEffect(Unit) {
        snapshotFlow { zone }.drop(1).collect {
            if (press.targetValue == 1f) {
                view.performHapticFeedback(
                    if (Build.VERSION.SDK_INT >= 34) HapticFeedbackConstants.SEGMENT_TICK
                    else HapticFeedbackConstants.CLOCK_TICK
                )
            }
        }
    }

    Box(
        modifier
            .height(64.dp)
            .drawBackdrop(
                backdrop = backdrop,
                shape = { NavShape },
                effects = {
                    vibrancy()
                    blur(16.dp.toPx())
                    lens(16.dp.toPx(), 32.dp.toPx())
                },
                onDrawSurface = { drawRect(GlassColors.GlassTint) },
            )
            .border(1.dp, GlassColors.NavBorder, NavShape)
            .pointerInput(items.size) {
                val padPx = Padding.toPx()
                val tabWidth = (size.width - 2 * padPx) / items.size
                fun tabAt(x: Float) = ((x - padPx) / tabWidth - 0.5f).coerceIn(0f, items.lastIndex.toFloat())

                awaitEachGesture {
                    val down = awaitFirstDown()
                    scope.launch { press.animateTo(1f, PressSpring) }
                    var dragging = false
                    var releaseX: Float? = null
                    while (true) {
                        val change = awaitPointerEvent().changes.firstOrNull { it.id == down.id } ?: break
                        if (!change.pressed) {
                            releaseX = change.position.x
                            break
                        }
                        if (!dragging && abs(change.position.x - down.position.x) > viewConfiguration.touchSlop) {
                            dragging = true
                        }
                        if (dragging) {
                            change.consume()
                            scope.launch { position.animateTo(tabAt(change.position.x), FollowSpring) }
                        }
                    }
                    // Tap dan lepas-setelah-geser sama: pilih tab di bawah jari. Batal: kembali ke tab aktif.
                    val target = releaseX?.let { tabAt(it).roundToInt() } ?: currentSelected
                    scope.launch { press.animateTo(0f, PressSpring) }
                    scope.launch { position.animateTo(target.toFloat(), SettleSpring) }
                    if (target != currentSelected) currentOnSelect(target)
                }
            },
    ) {
        // Indikator: Glass Ringan (tanpa blur riil) agar anggaran 3 permukaan riil tetap aman.
        Box(
            Modifier
                .padding(Padding)
                .fillMaxHeight()
                .fillMaxWidth(1f / items.size)
                .graphicsLayer {
                    translationX = position.value * size.width
                    val stretch =
                        if (stretchEnabled) (abs(position.velocity) * StretchPerVelocity).coerceAtMost(MaxStretch) else 0f
                    val scale = lerp(1f, PressedScale, press.value)
                    scaleX = scale + stretch
                    scaleY = scale - stretch / 2
                }
                .drawBehind {
                    // Pil beraksen seperti Figma: isi tipis + garis tepi warna tab di bawahnya.
                    val accent = items[zone.coerceIn(0, items.lastIndex)].accent
                    val radius = CornerRadius(25.dp.toPx())
                    drawRoundRect(accent.copy(alpha = lerp(0.14f, 0.22f, press.value).coerceIn(0f, 1f)), cornerRadius = radius)
                    drawRoundRect(accent.copy(alpha = 0.45f), cornerRadius = radius, style = Stroke(1.dp.toPx()))
                },
        )
        Row(Modifier.fillMaxSize().padding(Padding).selectableGroup()) {
            items.forEachIndexed { index, item ->
                val color = if (index == zone) item.accent else GlassColors.TabInactive
                Column(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .semantics(mergeDescendants = true) {
                            role = Role.Tab
                            selected = index == selectedIndex
                            onClick { currentOnSelect(index); true }
                        },
                    verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Image(item.icon, contentDescription = null, Modifier.size(22.dp), colorFilter = ColorFilter.tint(color))
                    BasicText(
                        item.label,
                        style = TextStyle(
                            color = color,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center,
                        ),
                        maxLines = 1,
                    )
                }
            }
        }
    }
}
