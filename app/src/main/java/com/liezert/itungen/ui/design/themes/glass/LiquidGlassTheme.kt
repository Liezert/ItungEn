package com.liezert.itungen.ui.design.themes.glass

import com.liezert.itungen.R
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.liezert.glass.components.GlassDefaults
import com.liezert.glass.components.GlassSurface
import com.liezert.glass.components.LiquidBottomTab
import com.liezert.glass.components.LiquidBottomTabs
import com.liezert.glass.components.LiquidButton
import com.liezert.glass.components.LiquidDialog
import com.liezert.glass.components.LiquidSheet
import com.liezert.glass.components.LiquidToggle
import com.liezert.glass.components.LocalGlassBackdrop
import com.liezert.glass.components.LocalGlassEconomy
import com.liezert.glass.components.ProgressiveBlur
import com.liezert.itungen.ui.components.AppBackground
import com.liezert.itungen.ui.design.ItungColors
import com.liezert.itungen.ui.design.ItungComponents
import com.liezert.itungen.ui.design.ItungMetrics
import com.liezert.itungen.ui.design.ItungType
import com.liezert.itungen.ui.design.SurfaceRole
import com.liezert.itungen.ui.design.TabsRole
import com.liezert.itungen.ui.design.ThemeSpec
import com.liezert.itungen.ui.theme.Bold
import com.liezert.itungen.ui.theme.Ink
import com.liezert.itungen.ui.theme.InterFamily
import com.liezert.itungen.ui.theme.appText

/**
 * Tema bawaan: Liquid Glass (modul :glass, library Backdrop). Setiap komponen meneruskan parameter apa adanya ke
 * komponen kaca, jadi tampilannya sama persis dengan sebelum ada lapisan tema. :glass tidak tahu tentang tema
 * (ketergantungan satu arah: tema ini → :glass).
 */
object LiquidGlassTheme : ThemeSpec {
    override val id = "liquid_glass"
    override val nameRes = R.string.theme_liquid_glass
    override val capturesScreen = true

    // Token Figma (DESIGN_SPEC §6.1) untuk tema gelap; tema terang memakai warna sistem iOS.
    private val Dark = ItungColors(
        background = Color(0xFF000000),
        text = Color(0xFFFFFFFF),
        title = Color(0xFFE2E2E2),
        gray = Color(0xFF8E8E93),
        muted = Color(0xFFC0C6D6),
        subtle = Color(0xFF8B91A0),
        orange = Color(0xFFFF9F0A),
        green = Color(0xFF32D74B),
        blue = Color(0xFF0A84FF),
        red = Color(0xFFFF453A),
        cyan = Color(0xFF64D2FF),
        purple = Color(0xFFBF5AF2),
        yellow = Color(0xFFFFD60A),
        pink = Color(0xFFFF375F),
        accent = Color(0xFF3E90FF),
        accentLight = Color(0xFFAAC7FF),
        onAccent = Color(0xFF002957),
        track = Color(0xFF2A2A2A),
        emptyIconBg = Color(0xFF1F1F1F),
        avatarBg = Color(0xFF1C1C1E),
        onGreen = Color(0xFF00390A),
        navHome = Color(0xFFFFAF4E),
        navInsight = Color(0xFF38BDF8).copy(alpha = 0.7f),
        navGoals = Color(0xFF34D399).copy(alpha = 0.7f),
        navProfile = Color(0xFFC084FC).copy(alpha = 0.7f),
        income = Color(0xFF32D74B),
        expense = Color(0xFFFF453A),
        surface = Color(0xFF121212).copy(0.4f), // = GlassDefaults.Surface (gelap)
        border = Color(0xFFFFFFFF).copy(alpha = 0.14f),
    )

    private val Light = Dark.copy(
        background = Color(0xFFF2F2F7),
        text = Color(0xFF000000),
        title = Color(0xFF1C1C1E),
        muted = Color(0xFF55596A),
        subtle = Color(0xFF6E7383),
        orange = Color(0xFFFF9500),
        green = Color(0xFF34C759),
        blue = Color(0xFF007AFF),
        red = Color(0xFFFF3B30),
        cyan = Color(0xFF32ADE6),
        purple = Color(0xFFAF52DE),
        yellow = Color(0xFFFFCC00),
        pink = Color(0xFFFF2D55),
        accentLight = Color(0xFF0060DF),
        track = Color(0xFFE5E5EA),
        emptyIconBg = Color(0xFFE5E5EA),
        avatarBg = Color(0xFFE5E5EA),
        navHome = Color(0xFFFF9500),
        // Nominal: versi kontras tinggi warna sistem iOS (≥ 4,5:1 di atas kartu terang); hijau/merah biasa hanya 2–3:1.
        income = Color(0xFF1A7F37),
        expense = Color(0xFFD70015),
        surface = Color(0xFFFAFAFA).copy(0.4f), // = GlassDefaults.Surface (terang)
        border = Color(0xFF000000).copy(alpha = 0.14f),
    )

    override fun colors(dark: Boolean, context: Context?) = if (dark) Dark else Light

    override val type = ItungType(InterFamily)
    override val metrics = ItungMetrics(cardRadius = 24.dp, controlRadius = 999.dp, borderWidth = 0.dp, shadowOffset = 0.dp)

    override val components = object : ItungComponents {
        @Composable
        override fun Background(vignette: Boolean, modifier: Modifier) = AppBackground(vignette, modifier)

        // Efek tepi scroll iOS 26: isi yang discroll ke bawah status bar memudar dalam blur. Mode hemat: tanpa blur.
        @Composable
        override fun StatusBarCover(modifier: Modifier) {
            if (LocalGlassEconomy.current) return
            ProgressiveBlur(
                backdrop = LocalGlassBackdrop.current,
                modifier = modifier,
                // ponytail: tint gelap, bukan abu-abu demo (#808080 α0.8) yang terlalu terang di atas latar hitam.
                tint = Ink.Black,
                tintIntensity = 0.35f,
            )
        }

        @Composable
        override fun Button(
            onClick: () -> Unit,
            modifier: Modifier,
            tint: Color,
            surfaceColor: Color,
            enabled: Boolean,
            contentPadding: Dp,
            content: @Composable RowScope.() -> Unit,
        ) = LiquidButton(onClick = onClick, modifier = modifier, tint = tint, surfaceColor = surfaceColor, enabled = enabled, contentPadding = contentPadding, content = content)

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
        ) = GlassSurface(modifier, shape ?: GlassDefaults.CardShape, surfaceColor, tint, dashedBorder, contentPadding, contentAlignment, onClick, content)

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
        ) = LiquidBottomTabs(
            selectedTabIndex = selectedTabIndex,
            onTabSelected = onTabSelected,
            backdrop = LocalGlassBackdrop.current,
            tabsCount = tabsCount,
            modifier = modifier,
            accentColor = accentColor,
            height = height,
            onTabReselected = onTabReselected,
            content = content,
        )

        @Composable
        override fun RowScope.Tab(onClick: () -> Unit, modifier: Modifier, selected: Boolean, content: @Composable ColumnScope.() -> Unit) =
            LiquidBottomTab(onClick, modifier, selected, content)

        @Composable
        override fun Switch(checked: Boolean, onCheckedChange: (Boolean) -> Unit, modifier: Modifier) =
            LiquidToggle({ checked }, onCheckedChange, modifier = modifier)

        @Composable
        override fun Sheet(visible: Boolean, onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) =
            LiquidSheet(visible, onDismiss, content)

        @Composable
        override fun Dialog(visible: Boolean, onDismiss: () -> Unit, modifier: Modifier, content: @Composable ColumnScope.() -> Unit) =
            LiquidDialog(visible, onDismiss, modifier, content)

        @Composable
        override fun Divider(modifier: Modifier, color: Color) =
            Box(modifier.fillMaxWidth().height(1.dp).background(color))

        @Composable
        override fun TransactionRow(
            icon: @Composable () -> Unit,
            title: @Composable () -> Unit,
            subtitle: @Composable () -> Unit,
            amount: @Composable () -> Unit,
            dense: Boolean,
            modifier: Modifier,
        ) {
            Row(modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    icon()
                    Column(Modifier.weight(1f), verticalArrangement = if (dense) Arrangement.spacedBy(2.dp) else Arrangement.Top) {
                        title()
                        subtitle()
                    }
                }
                Box(Modifier.padding(start = 12.dp)) { amount() }
            }
        }

        @Composable
        override fun ImageCard(
            modifier: Modifier,
            imageHeight: Dp,
            image: @Composable BoxScope.() -> Unit,
            content: @Composable ColumnScope.() -> Unit,
        ) {
            GlassSurface(modifier, contentPadding = PaddingValues(1.dp)) {
                Column {
                    Box(Modifier.fillMaxWidth().height(imageHeight).background(Ink.EmptyIconBg), contentAlignment = Alignment.Center) {
                        // Sudut atas mengikuti kartu (24 dp) dikurangi tepi 1 dp, agar foto tidak keluar dari kaca.
                        Box(
                            Modifier.matchParentSize().clip(RoundedCornerShape(topStart = 23.dp, topEnd = 23.dp)),
                            contentAlignment = Alignment.Center,
                            content = image,
                        )
                    }
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp), content = content)
                }
            }
        }

        @Composable
        override fun TopBarTitle(text: String) = Text(text, style = appText(20f, 28f, Bold, Ink.Title))
    }
}
