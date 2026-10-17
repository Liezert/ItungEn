package com.liezert.itungen.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.liezert.glass.components.LiquidSheet
import com.liezert.itungen.ui.theme.Bold
import com.liezert.itungen.ui.theme.Ink
import com.liezert.itungen.ui.theme.Medium
import com.liezert.itungen.ui.theme.appText
import androidx.compose.ui.text.style.TextOverflow

/** Sheet kaca berisi pilihan bulat sekali ketuk (tanggal, hari, jam), bukan − / + berkali-kali. */
@Composable
fun ChoiceSheet(
    title: String,
    options: List<Pair<Int, String>>,
    columns: Int,
    selected: Int,
    visible: Boolean,
    onPick: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    LiquidSheet(visible, onDismiss) {
        Text(title, Modifier.padding(start = 24.dp, top = 4.dp, bottom = 12.dp), style = appText(18f, 24f, Bold))
        Column(Modifier.padding(start = 16.dp, end = 16.dp, bottom = 20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            options.chunked(columns).forEach { row ->
                Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(6.dp)) {
                    row.forEach { (value, label) ->
                        val on = value == selected
                        Box(
                            Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .clip(CircleShape)
                                .background(if (on) Ink.Blue else Ink.white(0.06f))
                                .clickable(role = Role.Button) { onPick(value) }
                                .semantics { this.selected = on },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                label,
                                style = appText(15f, 20f, if (on) Bold else Medium, if (on) Color.White else Ink.White),
                                textAlign = TextAlign.Center,
                                maxLines = 1, overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                    repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}
