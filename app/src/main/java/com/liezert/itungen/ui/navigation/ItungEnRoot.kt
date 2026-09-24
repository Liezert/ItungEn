package com.liezert.itungen.ui.navigation

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.liezert.glass.components.FloatingNavBar
import com.liezert.glass.components.LiquidButton
import com.liezert.glass.components.NavBarItem
import com.liezert.glass.tokens.GlassColors
import com.liezert.itungen.R
import com.liezert.itungen.ui.addtransaction.AddTransactionScreen
import com.liezert.itungen.ui.profile.ProfileScreen
import com.liezert.itungen.ui.transactions.TransactionsScreen

// ponytail: navigasi berbasis state (satu sub-layar per tab); pindah ke navigation-compose saat sub-layar kedua muncul.
@Composable
fun ItungEnRoot() {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var adding by rememberSaveable { mutableStateOf(false) }

    Box(Modifier.fillMaxSize().background(GlassColors.Background)) {
        if (adding) {
            // Layar penuh tanpa navbar (§6.3).
            AddTransactionScreen(onClose = { adding = false })
            return@Box
        }

        val backdrop = rememberLayerBackdrop()
        Box(Modifier.fillMaxSize().layerBackdrop(backdrop)) {
            when (tab) {
                0 -> TransactionsScreen()
                3 -> ProfileScreen()
                else -> Text(
                    "Menyusul di Tahap 4",
                    color = GlassColors.TextSecondary,
                    modifier = Modifier.align(Alignment.Center),
                )
            }
        }

        // Tombol tambah melayang di kanan atas navbar, seperti FAB Telegram.
        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(start = 19.5.dp, end = 19.5.dp, bottom = 17.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.End,
        ) {
            LiquidButton(
                onClick = { adding = true },
                backdrop = backdrop,
                modifier = Modifier.size(56.dp).semantics { contentDescription = "Tambah transaksi" },
                surface = GlassColors.AccentBlue.copy(alpha = 0.55f),
            ) {
                Image(painterResource(R.drawable.ic_add), null, Modifier.size(28.dp), colorFilter = ColorFilter.tint(Color.White))
            }
            FloatingNavBar(
                items = listOf(
                    NavBarItem("Beranda", painterResource(R.drawable.ic_nav_home)),
                    NavBarItem("Insight", painterResource(R.drawable.ic_nav_insight)),
                    NavBarItem("Goals", painterResource(R.drawable.ic_nav_goals)),
                    NavBarItem("Profil", painterResource(R.drawable.ic_nav_profile)),
                ),
                selectedIndex = tab,
                onSelect = { tab = it },
                backdrop = backdrop,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
