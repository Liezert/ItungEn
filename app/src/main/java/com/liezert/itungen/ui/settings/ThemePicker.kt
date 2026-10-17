package com.liezert.itungen.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.liezert.itungen.domain.model.formatRupiah
import com.liezert.itungen.ui.design.ThemeSpec
import com.liezert.itungen.ui.design.Themes
import com.liezert.itungen.ui.theme.Ink
import com.liezert.itungen.ui.theme.Medium
import com.liezert.itungen.ui.theme.appText

/**
 * Pemilih tema dengan pratinjau kecil: tiap kotak digambar dengan warna, bentuk, dan font tema itu sendiri (bukan
 * tema aktif), dalam mode gelap/terang yang sedang dipakai. Pilihan langsung berlaku tanpa restart.
 */
@Composable
fun ThemePicker(selected: ThemeSpec, onSelect: (ThemeSpec) -> Unit, modifier: Modifier = Modifier) {
    LazyRow(modifier.fillMaxWidth(), contentPadding = PaddingValues(horizontal = 4.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        items(Themes.all, key = { it.id }) { spec -> ThemeTile(spec, spec.id == selected.id) { onSelect(spec) } }
    }
}

@Composable
private fun ThemeTile(spec: ThemeSpec, selected: Boolean, onClick: () -> Unit) {
    val colors = spec.colors(Ink.dark, LocalContext.current)
    val font = spec.type.family
    val radius = spec.metrics.cardRadius.coerceAtMost(14.dp)
    Column(
        Modifier
            .width(112.dp)
            .selectable(selected, role = Role.RadioButton, onClick = onClick),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(84.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(colors.background)
                .border(if (selected) 2.dp else 1.dp, if (selected) Ink.Accent else Ink.white(0.14f), RoundedCornerShape(16.dp))
                .padding(10.dp),
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(radius))
                    .background(colors.surface.compositeOver(colors.background)) // kaca transparan: dilihat di atas latarnya
                    .then(if (spec.metrics.borderWidth > 0.dp) Modifier.border(spec.metrics.borderWidth, colors.border, RoundedCornerShape(radius)) else Modifier)
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                val sample = TextStyle(fontFamily = font, fontSize = 11.sp, lineHeight = 14.sp, fontWeight = spec.type.weight(FontWeight.SemiBold))
                Text(formatRupiah(50_000), style = sample.copy(color = colors.income), maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(formatRupiah(20_000), style = sample.copy(color = colors.expense), maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("Saldo", style = sample.copy(color = colors.text, fontWeight = spec.type.weight(FontWeight.Normal)), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        Text(
            spec.name,
            Modifier.padding(horizontal = 4.dp),
            style = appText(13f, 18f, Medium, if (selected) Ink.White else Ink.Muted),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
