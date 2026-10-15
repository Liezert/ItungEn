package com.liezert.itungen.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kyant.shapes.Capsule
import com.liezert.glass.components.GlassOverlay
import com.liezert.glass.components.GlassSurface
import com.liezert.glass.components.LiquidButton
import com.liezert.itungen.ui.theme.Ink
import com.liezert.itungen.ui.theme.Medium
import com.liezert.itungen.ui.theme.SemiBold
import com.liezert.itungen.ui.theme.appText

/** Jarak dari bawah layar ke atas navbar (17 + 64 dp navbar + napas 12 dp). */
val AboveNavBar = 93.dp

/**
 * Kapsul kaca "… · Urungkan" di atas navbar, mis. setelah menghapus atau mencatat cepat.
 * [message] null = tersembunyi (pesan terakhir tetap tampil selama animasi keluar).
 */
@Composable
fun UndoToast(message: String?, onUndo: () -> Unit, bottomOffset: Dp = AboveNavBar) {
    var last by remember { mutableStateOf("") }
    if (message != null) last = message
    val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    GlassOverlay {
        Box(Modifier.fillMaxSize().padding(bottom = bottomOffset + navBottom), contentAlignment = Alignment.BottomCenter) {
            AnimatedVisibility(
                visible = message != null,
                enter = slideInVertically(spring(dampingRatio = 0.8f, stiffness = 500f)) { it } + fadeIn(),
                exit = slideOutVertically { it / 2 } + fadeOut(),
            ) {
                GlassSurface(
                    Modifier.fillMaxWidth().padding(horizontal = 20.dp).height(56.dp),
                    shape = Capsule(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 20.dp, end = 6.dp),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                        Text(last, Modifier.weight(1f), style = appText(14f, 20f, Medium), maxLines = 1)
                        LiquidButton(onClick = onUndo, modifier = Modifier.height(44.dp), contentPadding = 16.dp) {
                            Text("Urungkan", style = appText(14f, 20f, SemiBold, Ink.Blue))
                        }
                    }
                }
            }
        }
    }
}
