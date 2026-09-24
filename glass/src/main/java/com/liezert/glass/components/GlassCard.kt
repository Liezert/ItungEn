package com.liezert.glass.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.liezert.glass.tokens.GlassColors

private val CardShape = RoundedCornerShape(12.dp)

/** Glass Ringan (§6.2): fill transparan + border, tanpa blur riil, jadi bebas dipakai berulang. */
@Composable
fun GlassCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier
            .background(GlassColors.CardFill, CardShape)
            .border(1.dp, GlassColors.CardBorder, CardShape)
            .padding(16.dp),
        content = content,
    )
}
