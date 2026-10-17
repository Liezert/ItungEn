package com.liezert.glass.components

import androidx.compose.animation.core.animate
import androidx.compose.foundation.gestures.DraggableState
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/** Sheet tertutup bila ditarik lebih jauh dari ini atau dilepas lebih cepat dari [SheetDismissVelocity]. */
val SheetDismissDistance = 120.dp

/** Kecepatan lepas ke bawah (px/detik) yang menutup sheet walau jaraknya belum sampai [SheetDismissDistance]. */
const val SheetDismissVelocity = 1500f

/**
 * Tarik sheet ke bawah untuk menutup. Dipakai [LiquidSheet] dan sheet tema lain agar perilakunya sama: pegangan dan
 * permukaan sheet bisa diseret; isi yang bisa di-scroll baru ikut menyeret sheet setelah sampai di posisi paling atas
 * (nested scroll), dan menarik naik mengembalikan sheet dulu sebelum isinya digulir lagi. Di bawah ambang, sheet
 * kembali dengan spring. Bila onDismiss tidak menutup sheet (mis. meminta konfirmasi dulu), sheet juga kembali.
 *
 * Pasang [modifier] pada panel sheet SEBELUM panel digeser sebesar [offset] (graphicsLayer). Bila gesturnya ikut
 * tergeser, posisi jari terbaca relatif terhadap panel yang ikut turun, dan sheet hanya bergerak separuh gerakan jari.
 */
@Stable
class SheetDragState internal constructor(
    private val scope: CoroutineScope,
    private val dismissDistance: Float,
    private val visible: State<Boolean>,
    private val onDismiss: State<() -> Unit>,
) {
    /** Jarak panel dari posisi normalnya (px, ≥ 0). */
    var offset by mutableFloatStateOf(0f)
        private set
    private var settling: Job? = null

    private fun dragBy(delta: Float): Float {
        settling?.cancel()
        val next = (offset + delta).coerceAtLeast(0f)
        return (next - offset).also { offset = next }
    }

    private fun release(velocity: Float) {
        settling = scope.launch {
            if (offset > dismissDistance || velocity > SheetDismissVelocity) {
                onDismiss.value()
                // Sheet mulai menutup: biarkan keluar dari posisi sekarang. Masih tampil: kembali ke tempatnya.
                if (withTimeoutOrNull(150) { snapshotFlow { visible.value }.first { !it } } != null) return@launch
            }
            animate(offset, 0f, animationSpec = Bouncy) { value, _ -> offset = value }
        }
    }

    private val nestedScroll = object : NestedScrollConnection {
        // Sheet sedang tertarik dan jari naik: naikkan sheet dulu, baru isinya digulir.
        override fun onPreScroll(available: Offset, source: NestedScrollSource) =
            if (source == NestedScrollSource.UserInput && available.y < 0 && offset > 0) Offset(0f, dragBy(available.y)) else Offset.Zero

        // Isi sudah paling atas dan jari masih turun: sisanya menyeret sheet. Fling yang mentok di atas tidak.
        override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource) =
            if (source == NestedScrollSource.UserInput && available.y > 0) Offset(0f, dragBy(available.y)) else Offset.Zero

        override suspend fun onPreFling(available: Velocity): Velocity {
            if (offset == 0f) return Velocity.Zero
            release(available.y)
            return available // isi tidak ikut terlempar
        }
    }

    val modifier: Modifier = Modifier
        .nestedScroll(nestedScroll)
        .draggable(DraggableState { dragBy(it) }, Orientation.Vertical, onDragStopped = { release(it) })
}

/** Panggil di dalam isi AnimatedVisibility sheet, agar posisinya mulai dari nol tiap kali sheet dibuka. */
@Composable
fun rememberSheetDragState(visible: Boolean, onDismiss: () -> Unit): SheetDragState {
    val scope = rememberCoroutineScope()
    val visibleState = rememberUpdatedState(visible)
    val dismiss = rememberUpdatedState(onDismiss)
    val distance = with(LocalDensity.current) { SheetDismissDistance.toPx() }
    return remember(scope, distance) { SheetDragState(scope, distance, visibleState, dismiss) }
}
