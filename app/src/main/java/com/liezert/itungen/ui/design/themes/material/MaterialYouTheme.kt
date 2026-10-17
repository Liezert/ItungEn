package com.liezert.itungen.ui.design.themes.material

import com.liezert.itungen.R
import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface as M3Surface
import androidx.compose.material3.Switch as M3Switch
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.liezert.itungen.ui.design.ItungColors
import com.liezert.itungen.ui.design.ItungComponents
import com.liezert.itungen.ui.design.ItungMetrics
import com.liezert.itungen.ui.design.ItungType
import com.liezert.itungen.ui.design.SurfaceRole
import com.liezert.itungen.ui.design.TabsRole
import com.liezert.itungen.ui.design.ThemeSpec
import com.liezert.itungen.ui.design.themes.common.DraggableTabs
import com.liezert.itungen.ui.design.themes.common.FlatTab
import com.liezert.itungen.ui.design.themes.common.FlatDialog
import com.liezert.itungen.ui.design.themes.common.FlatSheet
import com.liezert.itungen.ui.theme.Ink
import com.liezert.itungen.ui.theme.InterFamily
import com.liezert.itungen.ui.theme.Medium
import com.liezert.itungen.ui.theme.appText

/**
 * Tema "Material You": warna dari wallpaper (dynamic color, Android 12+; minSdk 33) dan komponen Material 3 standar.
 * Hijau/merah nominal ditentukan tetap, tidak dari wallpaper, agar pemasukan/pengeluaran selalu konsisten.
 */
object MaterialYouTheme : ThemeSpec {
    override val id = "material_you"
    override val nameRes = R.string.theme_material_you

    override fun materialScheme(dark: Boolean, context: Context): ColorScheme =
        if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)

    override fun colors(dark: Boolean, context: Context?): ItungColors {
        // Tanpa konteks (pratinjau/tes): skema bawaan Material 3.
        val s = context?.let { materialScheme(dark, it) } ?: if (dark) darkColorScheme() else lightColorScheme()
        return ItungColors(
            background = s.surface,
            text = s.onSurface,
            title = s.onSurface,
            gray = s.onSurfaceVariant,
            muted = s.onSurfaceVariant,
            subtle = s.outline,
            // Warna kategori: nada Material tetap (bukan dari wallpaper) agar kategori mudah dibedakan.
            orange = if (dark) Color(0xFFFFB870) else Color(0xFF9A4F00),
            green = if (dark) Color(0xFF7AD69A) else Color(0xFF1B6D3A),
            blue = s.primary,
            red = if (dark) Color(0xFFFFB4AB) else Color(0xFFBA1A1A),
            cyan = if (dark) Color(0xFF7DD3E3) else Color(0xFF006876),
            purple = if (dark) Color(0xFFD0BCFF) else Color(0xFF6750A4),
            yellow = if (dark) Color(0xFFE9C46A) else Color(0xFF7A5900),
            pink = if (dark) Color(0xFFFFB0C8) else Color(0xFF984061),
            accent = s.primary,
            accentLight = s.primary,
            onAccent = s.onPrimary,
            track = s.surfaceVariant,
            emptyIconBg = s.surfaceContainerHighest,
            avatarBg = s.secondaryContainer,
            onGreen = s.onPrimary,
            navHome = s.primary,
            navInsight = s.tertiary,
            navGoals = s.secondary,
            navProfile = s.primary,
            income = if (dark) Color(0xFF7FD99A) else Color(0xFF1B7A3A),
            expense = if (dark) Color(0xFFFF8A80) else Color(0xFFB3261E),
            surface = s.surfaceContainer,
            border = s.outlineVariant,
        )
    }

    override val type = ItungType(InterFamily)
    override val metrics = ItungMetrics(cardRadius = 20.dp, controlRadius = 20.dp, borderWidth = 1.dp, shadowOffset = 0.dp)

    private val Card = RoundedCornerShape(20.dp)
    private val Field = RoundedCornerShape(12.dp)
    private val Pill = RoundedCornerShape(50)

    /** Warna isi tombol berwarna. Mode gelap: nada primer Material terang, jadi digelapkan agar label putih terbaca. */
    @Composable
    private fun container(tint: Color, surfaceColor: Color): Color = when {
        tint.isSpecified -> if (Ink.dark) lerp(tint, Color.Black, 0.45f) else tint
        surfaceColor.isSpecified -> surfaceColor.compositeOver(MaterialTheme.colorScheme.surfaceContainerHigh)
        else -> MaterialTheme.colorScheme.secondaryContainer
    }

    override val components = object : ItungComponents {
        @Composable
        override fun Background(vignette: Boolean, modifier: Modifier) = Box(modifier.fillMaxSize().background(Ink.Black))

        @Composable
        override fun StatusBarCover(modifier: Modifier) =
            Box(modifier.background(Brush.verticalGradient(0f to Ink.Black, 0.6f to Ink.Black, 1f to Ink.Black.copy(alpha = 0f))))

        @Composable
        override fun Button(
            onClick: () -> Unit,
            modifier: Modifier,
            tint: Color,
            surfaceColor: Color,
            enabled: Boolean,
            contentPadding: Dp,
            content: @Composable RowScope.() -> Unit,
        ) = M3Surface(onClick = onClick, modifier = modifier, enabled = enabled, shape = Pill, color = container(tint, surfaceColor)) {
            Row(
                Modifier.height(48.dp).padding(horizontal = contentPadding),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
                content = content,
            )
        }

        @Composable
        override fun Surface(
            role: SurfaceRole,
            modifier: Modifier,
            shape: Shape?,
            surfaceColor: Color,
            tint: Brush?,
            dashedBorder: Color?,
            contentPadding: PaddingValues,
            contentAlignment: Alignment,
            onClick: (() -> Unit)?,
            content: @Composable BoxScope.() -> Unit,
        ) {
            val outline = if (role == SurfaceRole.FIELD) Field else Card
            val color = surfaceColor.compositeOver(MaterialTheme.colorScheme.surface)
            val border = when {
                dashedBorder != null -> BorderStroke(1.dp, dashedBorder)
                role == SurfaceRole.FIELD -> BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                else -> null
            }
            val inner: @Composable () -> Unit = {
                Box(
                    Modifier.then(if (tint != null) Modifier.background(tint) else Modifier).padding(contentPadding),
                    contentAlignment = contentAlignment,
                    content = content,
                )
            }
            if (onClick != null) M3Surface(onClick = onClick, modifier = modifier, shape = outline, color = color, border = border, content = inner)
            else M3Surface(modifier = modifier, shape = outline, color = color, border = border, content = inner)
        }

        @Composable
        override fun Tabs(
            role: TabsRole,
            selectedTabIndex: () -> Int,
            onTabSelected: (Int) -> Unit,
            onTabReselected: (Int) -> Unit,
            tabsCount: Int,
            modifier: Modifier,
            accentColor: Color,
            height: Dp,
            content: @Composable RowScope.() -> Unit,
        ) = DraggableTabs(
            selectedTabIndex, onTabSelected, onTabReselected, tabsCount,
            modifier
                .height(height)
                .clip(if (role == TabsRole.NAVBAR) RoundedCornerShape(28.dp) else Pill)
                .background(if (role == TabsRole.NAVBAR) MaterialTheme.colorScheme.surfaceContainer else Color.Transparent)
                .then(if (role == TabsRole.SEGMENTED) Modifier.border(1.dp, MaterialTheme.colorScheme.outline, Pill) else Modifier)
                .padding(2.dp),
            // Tab aktif: kapsul secondaryContainer, seperti indikator navigasi Material 3.
            indicator = Modifier.padding(2.dp).clip(Pill).background(MaterialTheme.colorScheme.secondaryContainer),
            content = content,
        )

        @Composable
        override fun RowScope.Tab(onClick: () -> Unit, modifier: Modifier, selected: Boolean, content: @Composable ColumnScope.() -> Unit) =
            FlatTab(onClick, modifier, selected, content)

        @Composable
        override fun Switch(checked: Boolean, onCheckedChange: (Boolean) -> Unit, modifier: Modifier) =
            M3Switch(checked = checked, onCheckedChange = onCheckedChange, modifier = modifier)

        @Composable
        override fun Sheet(visible: Boolean, onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) = FlatSheet(
            visible, onDismiss, Color.Black.copy(alpha = 0.32f), MaterialTheme.colorScheme.outline,
            Modifier.clip(RoundedCornerShape(28.dp)).background(MaterialTheme.colorScheme.surfaceContainerLow), content,
        )

        @Composable
        override fun Dialog(visible: Boolean, onDismiss: () -> Unit, modifier: Modifier, content: @Composable ColumnScope.() -> Unit) = FlatDialog(
            visible, onDismiss, Color.Black.copy(alpha = 0.32f),
            Modifier.clip(RoundedCornerShape(28.dp)).background(MaterialTheme.colorScheme.surfaceContainerHigh), modifier, content,
        )

        @Composable
        override fun Divider(modifier: Modifier, color: Color) = HorizontalDivider(modifier, color = MaterialTheme.colorScheme.outlineVariant)

        @Composable
        override fun TransactionRow(
            icon: @Composable () -> Unit,
            title: @Composable () -> Unit,
            subtitle: @Composable () -> Unit,
            amount: @Composable () -> Unit,
            dense: Boolean,
            modifier: Modifier,
        ) = Row(modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                icon()
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(if (dense) 2.dp else 0.dp)) {
                    title()
                    subtitle()
                }
            }
            Box(Modifier.padding(start = 12.dp)) { amount() }
        }

        @Composable
        override fun ImageCard(
            modifier: Modifier,
            imageHeight: Dp,
            image: @Composable BoxScope.() -> Unit,
            content: @Composable ColumnScope.() -> Unit,
        ) = M3Surface(modifier = modifier, shape = Card, color = MaterialTheme.colorScheme.surfaceContainer) {
            Column {
                Box(Modifier.fillMaxWidth().height(imageHeight).background(Ink.EmptyIconBg), contentAlignment = Alignment.Center, content = image)
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp), content = content)
            }
        }

        @Composable
        override fun TopBarTitle(text: String) = Text(text, style = appText(22f, 28f, Medium, Ink.Title))
    }
}
