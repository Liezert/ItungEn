package com.liezert.itungen.ui.components

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Ruang di bawah konten agar item terakhir lolos dari navbar dan FAB "+":
 * navbar 17 + 64, celah 16, FAB 56, lalu napas 24 dp.
 */
@Composable
fun bottomClearance(base: Dp = 177.dp): Dp =
    base + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
