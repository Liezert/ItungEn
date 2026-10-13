package com.liezert.glass.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** [GlassSurface] berisi kolom dengan padding dalam 21 dp. */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    padding: Dp = 21.dp,
    surfaceColor: Color = GlassDefaults.Surface,
    tint: Brush? = null,
    dashedBorder: Color? = null,
    shape: Shape = GlassDefaults.CardShape,
    onClick: (() -> Unit)? = null,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    content: @Composable ColumnScope.() -> Unit,
) {
    GlassSurface(
        modifier = modifier,
        shape = shape,
        surfaceColor = surfaceColor,
        tint = tint,
        dashedBorder = dashedBorder,
        onClick = onClick,
        contentPadding = PaddingValues(padding),
    ) {
        Column(
            Modifier.fillMaxWidth(),
            verticalArrangement = verticalArrangement,
            horizontalAlignment = horizontalAlignment,
            content = content,
        )
    }
}
