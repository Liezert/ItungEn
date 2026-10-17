package com.liezert.itungen.ui.design.themes.brutal

import androidx.compose.foundation.layout.defaultMinSize
import com.liezert.itungen.R
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
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
import com.liezert.itungen.ui.theme.Bold
import com.liezert.itungen.ui.theme.Ink
import com.liezert.itungen.ui.theme.InterFamily
import com.liezert.itungen.ui.theme.appText

/**
 * Tema "Neo-brutalism": datar dan berani. Tepi hitam tebal, palet cerah terbatas, bayangan keras tanpa blur
 * (offset solid), sudut kecil, huruf lebih tebal. Tombol yang ditekan bergeser ke bayangannya. Mode gelap: latar
 * gelap dengan tepi dan bayangan terang, agar identitasnya tetap.
 */
object BrutalTheme : ThemeSpec {
    override val id = "neo_brutalism"
    override val nameRes = R.string.theme_brutal

    private val Light = ItungColors(
        background = Color(0xFFFFF1C9),
        text = Color(0xFF111111),
        title = Color(0xFF111111),
        gray = Color(0xFF4A4A4A),
        muted = Color(0xFF2E2E2E),
        subtle = Color(0xFF555555),
        orange = Color(0xFFE85D04),
        green = Color(0xFF107F46), // ≥ 4,5:1 dengan teks putih (chip Tercapai)
        blue = Color(0xFF2F5BFF),
        red = Color(0xFFD62828),
        cyan = Color(0xFF00869E),
        purple = Color(0xFF7C3AED),
        yellow = Color(0xFFB98900),
        pink = Color(0xFFD6336C),
        accent = Color(0xFF2F5BFF),
        accentLight = Color(0xFF1F3FCC),
        onAccent = Color(0xFFFFFFFF),
        track = Color(0xFFE6DCC0),
        emptyIconBg = Color(0xFFFFD84D),
        avatarBg = Color(0xFFFFD84D),
        onGreen = Color(0xFFFFFFFF),
        navHome = Color(0xFFFFC800),
        navInsight = Color(0xFF39D0EA),
        navGoals = Color(0xFF3DDC84),
        navProfile = Color(0xFFB98CFF),
        income = Color(0xFF0B7A3B),
        expense = Color(0xFFC81E1E),
        surface = Color(0xFFFFFFFF),
        border = Color(0xFF111111),
    )

    private val Dark = Light.copy(
        background = Color(0xFF161616),
        text = Color(0xFFF5F5F5),
        title = Color(0xFFFFFFFF),
        gray = Color(0xFFB5B5B5),
        muted = Color(0xFFD6D6D6),
        subtle = Color(0xFFA6A6A6),
        orange = Color(0xFFFF8A3D),
        green = Color(0xFF2FA866),
        blue = Color(0xFF4A6CFF),
        red = Color(0xFFE5484D),
        cyan = Color(0xFF39D0EA),
        purple = Color(0xFFA78BFA),
        yellow = Color(0xFFFFD43B),
        pink = Color(0xFFFF77B3),
        accent = Color(0xFF4A6CFF),
        accentLight = Color(0xFF9DB0FF),
        track = Color(0xFF3A3A3A),
        emptyIconBg = Color(0xFF3D3415),
        avatarBg = Color(0xFF3D3415),
        onGreen = Color(0xFF0A0A0A),
        income = Color(0xFF4ADE80),
        expense = Color(0xFFFF6B6B),
        surface = Color(0xFF232323),
        border = Color(0xFFF5F5F5),
    )

    override fun colors(dark: Boolean, context: Context?) = if (dark) Dark else Light

    // Bobot dinaikkan satu tingkat (font Inter yang dibundel mencakup 400–700).
    override val type = ItungType(InterFamily, weight = {
        when (it) {
            FontWeight.Normal -> FontWeight.Medium
            FontWeight.Medium -> FontWeight.SemiBold
            else -> FontWeight.Bold
        }
    })
    override val metrics = ItungMetrics(cardRadius = 4.dp, controlRadius = 4.dp, borderWidth = 2.5.dp, shadowOffset = 4.dp)

    private val Corner = RoundedCornerShape(4.dp)
    private val Border = 2.5.dp
    private val Shadow = 4.dp

    private fun fill(color: Color) = if (color.isSpecified) color.compositeOver(Ink.Surface) else Ink.Surface

    /** Bayangan keras: bentuk yang sama, digeser [offset], warna tepi, tanpa blur. */
    private fun Modifier.hardShadow(shape: Shape, offset: Dp = Shadow) = drawBehind {
        val o = offset.toPx()
        translate(o, o) { drawOutline(shape.createOutline(size, layoutDirection, this), Ink.Border) }
    }

    private fun Modifier.brutal(fill: Color, shape: Shape = Corner) =
        hardShadow(shape).clip(shape).background(fill).border(Border, Ink.Border, shape)

    override val components = object : ItungComponents {
        @Composable
        override fun Background(vignette: Boolean, modifier: Modifier) = Box(modifier.fillMaxSize().background(Ink.Black))

        @Composable
        override fun StatusBarCover(modifier: Modifier) =
            Box(modifier.background(Brush.verticalGradient(0f to Ink.Black, 0.6f to Ink.Black, 1f to Ink.Black.copy(alpha = 0f))))

        // Ditekan: tombol bergeser ke posisi bayangannya, bayangannya hilang (efek "ditekan masuk").
        @Composable
        override fun Button(
            onClick: () -> Unit,
            modifier: Modifier,
            tint: Color,
            surfaceColor: Color,
            enabled: Boolean,
            contentPadding: Dp,
            content: @Composable RowScope.() -> Unit,
        ) {
            val interaction = remember { MutableInteractionSource() }
            val pressed by interaction.collectIsPressedAsState()
            val shift = if (pressed && enabled) Shadow else 0.dp
            Row(
                modifier
                    .offset(shift, shift)
                    .hardShadow(Corner, Shadow - shift)
                    .clip(Corner)
                    .background(if (tint.isSpecified) tint else fill(surfaceColor))
                    .border(Border, Ink.Border, Corner)
                    .clickable(interaction, indication = null, enabled = enabled, role = Role.Button, onClick = onClick)
                    .defaultMinSize(minHeight = 48.dp)
                    .padding(horizontal = contentPadding, vertical = 6.dp),
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
        ) = Box(
            modifier
                // Kolom input: tepi tebal tanpa bayangan; kartu & panel: tepi + bayangan keras.
                .then(if (role == SurfaceRole.FIELD) Modifier.clip(Corner).background(fill(surfaceColor)).border(Border, Ink.Border, Corner) else Modifier.brutal(fill(surfaceColor)))
                .then(if (tint != null) Modifier.background(tint) else Modifier)
                .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)
                .padding(contentPadding),
            contentAlignment = contentAlignment,
            content = content,
        )

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
            modifier.height(height).brutal(Ink.Surface).padding(2.5.dp),
            // Tab aktif: blok kuning dengan tepi tebal.
            indicator = Modifier.padding(2.5.dp).clip(Corner).background(Ink.Yellow.copy(alpha = 0.45f)).border(2.dp, Ink.Border, Corner),
            content = content,
        )

        @Composable
        override fun RowScope.Tab(onClick: () -> Unit, modifier: Modifier, selected: Boolean, content: @Composable ColumnScope.() -> Unit) =
            FlatTab(onClick, modifier, selected, content)

        @Composable
        override fun Switch(checked: Boolean, onCheckedChange: (Boolean) -> Unit, modifier: Modifier) = Box(
            modifier
                .size(50.dp, 28.dp)
                .clip(Corner)
                .background(if (checked) Ink.Green else Ink.Surface)
                .border(Border, Ink.Border, Corner)
                .clickable(role = Role.Switch) { onCheckedChange(!checked) }
                .padding(4.dp),
            contentAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart,
        ) { Box(Modifier.size(18.dp).clip(RoundedCornerShape(2.dp)).background(Ink.Surface).border(2.dp, Ink.Border, RoundedCornerShape(2.dp))) }

        @Composable
        override fun Sheet(visible: Boolean, onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) =
            FlatSheet(visible, onDismiss, Color.Black.copy(alpha = 0.45f), Ink.Border, Modifier.brutal(Ink.Surface), content)

        @Composable
        override fun Dialog(visible: Boolean, onDismiss: () -> Unit, modifier: Modifier, content: @Composable ColumnScope.() -> Unit) =
            FlatDialog(visible, onDismiss, Color.Black.copy(alpha = 0.45f), Modifier.brutal(Ink.Surface), modifier, content)

        @Composable
        override fun Divider(modifier: Modifier, color: Color) = Box(modifier.fillMaxWidth().height(2.dp).background(Ink.Border))

        @Composable
        override fun TransactionRow(
            icon: @Composable () -> Unit,
            title: @Composable () -> Unit,
            subtitle: @Composable () -> Unit,
            amount: @Composable () -> Unit,
            dense: Boolean,
            modifier: Modifier,
        ) = Row(modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
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
        ) = Column(modifier.brutal(Ink.Surface)) {
            Box(Modifier.fillMaxWidth().height(imageHeight).background(Ink.EmptyIconBg), contentAlignment = Alignment.Center, content = image)
            Box(Modifier.fillMaxWidth().height(Border).background(Ink.Border))
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(16.dp), content = content)
        }

        @Composable
        override fun TopBarTitle(text: String) = Text(text.uppercase(), style = appText(20f, 28f, Bold, Ink.Title, tracking = 1.5f))
    }
}
