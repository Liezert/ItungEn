package com.liezert.itungen.ui.profile

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.liezert.glass.components.GlassCard
import com.liezert.glass.tokens.GlassColors
import com.liezert.itungen.R

/** Tab Profil. Sub-layar dibuka di dalam tab agar navbar tetap tampil dengan Profil aktif. */
@Composable
fun ProfileScreen() {
    var showRecurring by rememberSaveable { mutableStateOf(false) }
    if (showRecurring) {
        RecurringRulesScreen(onBack = { showRecurring = false })
        return
    }

    Column(
        Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Profil", color = GlassColors.TextPrimary, fontSize = 20.sp)
        MenuItem("Transaksi Berulang", "Gaji & tagihan tetap") { showRecurring = true }
    }
}

@Composable
private fun MenuItem(title: String, subtitle: String, onClick: () -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    GlassCard(Modifier.fillMaxWidth().clip(shape).clickable(role = Role.Button, onClick = onClick)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, color = GlassColors.TextPrimary, fontSize = 16.sp)
                Text(subtitle, color = GlassColors.TextSecondary, fontSize = 12.sp)
            }
            Image(
                painterResource(R.drawable.ic_chevron_right),
                contentDescription = null,
                colorFilter = ColorFilter.tint(GlassColors.TextSecondary),
            )
        }
    }
}
