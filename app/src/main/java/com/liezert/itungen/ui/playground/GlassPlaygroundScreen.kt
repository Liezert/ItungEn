package com.liezert.itungen.ui.playground

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.liezert.glass.components.FloatingNavBar
import com.liezert.glass.components.LiquidButton
import com.liezert.glass.tokens.GlassColors

private val Tabs = listOf("Beranda", "Insight", "Goals", "Profil")

/** Layar uji Tahap 2: bukan fitur, dibuang setelah angka glass ditetapkan. */
@Composable
fun GlassPlaygroundScreen() {
    var selected by rememberSaveable { mutableIntStateOf(0) }
    var plusTaps by rememberSaveable { mutableIntStateOf(0) }
    val backdrop = rememberLayerBackdrop()

    Box(Modifier.fillMaxSize().background(GlassColors.Background)) {
        LazyColumn(
            Modifier.fillMaxSize().layerBackdrop(backdrop).statusBarsPadding(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Text(
                    "Tab: ${Tabs[selected]} · \"+\" ditekan $plusTaps×",
                    color = GlassColors.TextPrimary,
                    fontSize = 20.sp,
                )
            }
            items(30) { i ->
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(88.dp)
                        .background(Color.hsv((i * 37f) % 360f, 0.7f, 0.9f), RoundedCornerShape(12.dp))
                        .padding(16.dp),
                ) {
                    Text("Kartu ${i + 1}", color = Color.Black, fontSize = 16.sp)
                }
            }
        }

        // 351 dp di layar 390 dp: navbar menyusut, celah 8 dp, tombol "+" 64 dp (§6.4).
        Row(
            Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(start = 19.5.dp, end = 19.5.dp, bottom = 17.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FloatingNavBar(
                labels = Tabs,
                selectedIndex = selected,
                onSelect = { selected = it },
                backdrop = backdrop,
                modifier = Modifier.weight(1f),
            )
            LiquidButton(onClick = { plusTaps++ }, backdrop = backdrop, modifier = Modifier.size(64.dp)) {
                Text("+", color = GlassColors.TextPrimary, fontSize = 32.sp)
            }
        }
    }
}
