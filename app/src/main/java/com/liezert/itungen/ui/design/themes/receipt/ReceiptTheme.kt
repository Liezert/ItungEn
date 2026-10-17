package com.liezert.itungen.ui.design.themes.receipt

import android.content.Context
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
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
import com.liezert.itungen.ui.design.themes.common.DashedLine
import com.liezert.itungen.ui.design.themes.common.FlatDialog
import com.liezert.itungen.ui.design.themes.common.FlatSheet
import com.liezert.itungen.ui.design.themes.common.ZigzagBottomShape
import com.liezert.itungen.ui.theme.Bold
import com.liezert.itungen.ui.theme.Ink
import com.liezert.itungen.ui.theme.appText

/**
 * Tema "Struk": kertas struk termal. Font monospace (font sistem, tanpa berkas tambahan), sudut hampir tajam, tanpa
 * bayangan, pemisah putus-putus, kartu bertepi bawah bergerigi, dan titik pengisi antara judul dan nominal.
 * Mode gelap = kertas gelap dengan tinta terang.
 */
object ReceiptTheme : ThemeSpec {
    override val id = "receipt"
    override val name = "Struk"

    private val Paper = ItungColors(
        background = Color(0xFFEDE7DB),
        text = Color(0xFF2A2927),
        title = Color(0xFF1E1D1B),
        gray = Color(0xFF6F6A61),
        muted = Color(0xFF55514A),
        subtle = Color(0xFF7D786E),
        orange = Color(0xFFA45A10),
        green = Color(0xFF1E7038),
        blue = Color(0xFF244E8A),
        red = Color(0xFFAE2A21),
        cyan = Color(0xFF1C6670),
        purple = Color(0xFF65408F),
        yellow = Color(0xFF7D6400),
        pink = Color(0xFF9A2B57),
        accent = Color(0xFF2A2927), // tombol utama = tinta
        accentLight = Color(0xFF244E8A),
        onAccent = Color(0xFFFBF8F1),
        track = Color(0xFFDAD3C4),
        emptyIconBg = Color(0xFFE6DFD1),
        avatarBg = Color(0xFFE6DFD1),
        onGreen = Color(0xFFFBF8F1),
        navHome = Color(0xFF2A2927),
        navInsight = Color(0xFF2A2927),
        navGoals = Color(0xFF2A2927),
        navProfile = Color(0xFF2A2927),
        income = Color(0xFF1E7038),
        expense = Color(0xFFAE2A21),
        surface = Color(0xFFFBF8F1), // kertas
        border = Color(0xFF2A2927).copy(alpha = 0.55f),
    )

    private val DarkPaper = Paper.copy(
        background = Color(0xFF12110F),
        text = Color(0xFFECE6D8),
        title = Color(0xFFF5F0E5),
        gray = Color(0xFFA59E90),
        muted = Color(0xFFCAC3B4),
        subtle = Color(0xFF948E81),
        orange = Color(0xFFE3A15C),
        green = Color(0xFF4F9F68),
        blue = Color(0xFF4F7FC4),
        red = Color(0xFFC9584D),
        cyan = Color(0xFF6FC3CE),
        purple = Color(0xFFB79CEA),
        yellow = Color(0xFFE2C767),
        pink = Color(0xFFE897B8),
        accent = Color(0xFF4F6E9E),
        accentLight = Color(0xFF9BBDF0),
        onAccent = Color(0xFF12110F),
        track = Color(0xFF39362F),
        emptyIconBg = Color(0xFF2B2924),
        avatarBg = Color(0xFF2B2924),
        onGreen = Color(0xFF12110F),
        navHome = Color(0xFFECE6D8),
        navInsight = Color(0xFFECE6D8),
        navGoals = Color(0xFFECE6D8),
        navProfile = Color(0xFFECE6D8),
        income = Color(0xFF7FD69A),
        expense = Color(0xFFF2948A),
        surface = Color(0xFF201E1A),
        border = Color(0xFFECE6D8).copy(alpha = 0.45f),
    )

    override fun colors(dark: Boolean, context: Context?) = if (dark) DarkPaper else Paper

    // Monospace ±15% lebih lebar dari Inter: dikecilkan sedikit agar nominal dan label tidak terpotong.
    override val type = ItungType(FontFamily.Monospace, sizeScale = 0.88f)
    override val metrics = ItungMetrics(cardRadius = 2.dp, controlRadius = 2.dp, borderWidth = 1.dp, shadowOffset = 0.dp)

    private val Corner = RoundedCornerShape(2.dp)

    /** Isi kertas: warna yang diminta pemanggil (mis. chip terpilih, transparan) ditumpuk di atas kertas. */
    private fun fill(color: Color) = if (color.isSpecified) color.compositeOver(Ink.Surface) else Ink.Surface

    private fun Modifier.dashedBorder(color: Color, shape: Shape) = drawWithContent {
        drawContent()
        drawOutline(
            shape.createOutline(size, layoutDirection, this),
            color,
            style = Stroke(1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 3.dp.toPx()))),
        )
    }

    override val components = object : ItungComponents {
        @Composable
        override fun Background(vignette: Boolean, modifier: Modifier) = Box(modifier.fillMaxSize().background(Ink.Black))

        @Composable
        override fun StatusBarCover(modifier: Modifier) =
            Box(modifier.background(Brush.verticalGradient(0f to Ink.Black, 0.7f to Ink.Black, 1f to Ink.Black.copy(alpha = 0f))))

        @Composable
        override fun Button(
            onClick: () -> Unit,
            modifier: Modifier,
            tint: Color,
            surfaceColor: Color,
            enabled: Boolean,
            contentPadding: Dp,
            content: @Composable RowScope.() -> Unit,
        ) = Row(
            modifier
                .clip(Corner)
                .background(if (tint.isSpecified) tint else fill(surfaceColor))
                .then(if (tint.isSpecified) Modifier else Modifier.border(1.dp, Ink.Border, Corner))
                .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
                .height(48.dp)
                .padding(horizontal = contentPadding),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
            content = content,
        )

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
            // Kartu = potongan struk: tepi bawah bergerigi (isinya diberi ruang agar tidak ikut terpotong).
            val outline: Shape = if (role == SurfaceRole.CARD) ZigzagBottomShape() else Corner
            Box(
                modifier
                    .clip(outline)
                    .background(fill(surfaceColor))
                    .then(if (tint != null) Modifier.background(tint) else Modifier)
                    .then(
                        when {
                            dashedBorder != null -> Modifier.dashedBorder(dashedBorder, outline)
                            role == SurfaceRole.FIELD -> Modifier.dashedBorder(Ink.Border, outline)
                            role == SurfaceRole.PANEL -> Modifier.border(1.dp, Ink.Border, outline)
                            else -> Modifier
                        }
                    )
                    .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)
                    .padding(contentPadding)
                    .then(if (role == SurfaceRole.CARD) Modifier.padding(bottom = 6.dp) else Modifier),
                contentAlignment = contentAlignment,
                content = content,
            )
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
                .clip(Corner)
                .background(Ink.Surface)
                .then(if (role == TabsRole.NAVBAR) Modifier.dashedBorder(Ink.Border, Corner) else Modifier.border(1.dp, Ink.Border, Corner))
                .padding(horizontal = 4.dp),
            // Tab aktif = garis tinta tebal di bawahnya (bukan blok terbalik: isi tab memakai warna tinta).
            indicator = Modifier.drawBehind {
                val h = 3.dp.toPx()
                drawRect(Ink.White, Offset(size.width * 0.2f, size.height - h - 4.dp.toPx()), Size(size.width * 0.6f, h))
            },
            content = content,
        )

        @Composable
        override fun RowScope.Tab(onClick: () -> Unit, modifier: Modifier, selected: Boolean, content: @Composable ColumnScope.() -> Unit) =
            FlatTab(onClick, modifier, selected, content)

        @Composable
        override fun Switch(checked: Boolean, onCheckedChange: (Boolean) -> Unit, modifier: Modifier) = Box(
            modifier
                .size(46.dp, 26.dp)
                .clip(Corner)
                .background(if (checked) Ink.White else Ink.Surface)
                .border(1.5.dp, Ink.White, Corner)
                .clickable(role = Role.Switch) { onCheckedChange(!checked) }
                .padding(3.dp),
            contentAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart,
        ) { Box(Modifier.size(18.dp).background(if (checked) Ink.Surface else Ink.White)) }

        @Composable
        override fun Sheet(visible: Boolean, onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) =
            FlatSheet(visible, onDismiss, Color.Black.copy(alpha = 0.35f), Ink.Subtle, Modifier.clip(Corner).background(Ink.Surface).border(1.dp, Ink.Border, Corner), content)

        @Composable
        override fun Dialog(visible: Boolean, onDismiss: () -> Unit, modifier: Modifier, content: @Composable ColumnScope.() -> Unit) =
            FlatDialog(visible, onDismiss, Color.Black.copy(alpha = 0.35f), Modifier.clip(Corner).background(Ink.Surface).border(1.dp, Ink.Border, Corner), modifier, content)

        @Composable
        override fun Divider(modifier: Modifier, color: Color) = DashedLine(Ink.Subtle, modifier)

        // "Makan siang ........ Rp15.000": titik pengisi di belakang judul dan nominal (keduanya berlatar kertas).
        @Composable
        override fun TransactionRow(
            icon: @Composable () -> Unit,
            title: @Composable () -> Unit,
            subtitle: @Composable () -> Unit,
            amount: @Composable () -> Unit,
            dense: Boolean,
            modifier: Modifier,
        ) = Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            icon()
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(if (dense) 2.dp else 0.dp)) {
                Box {
                    LeaderDots(Modifier.matchParentSize())
                    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.Bottom) {
                        Box(Modifier.weight(1f, fill = false).background(Ink.Surface).padding(end = 4.dp)) { title() }
                        Box(Modifier.background(Ink.Surface).padding(start = 4.dp)) { amount() }
                    }
                }
                subtitle()
            }
        }

        @Composable
        override fun ImageCard(
            modifier: Modifier,
            imageHeight: Dp,
            image: @Composable BoxScope.() -> Unit,
            content: @Composable ColumnScope.() -> Unit,
        ) = Column(modifier.clip(ZigzagBottomShape()).background(Ink.Surface).padding(bottom = 6.dp)) {
            Box(Modifier.fillMaxWidth().height(imageHeight).background(Ink.EmptyIconBg), contentAlignment = Alignment.Center, content = image)
            DashedLine(Ink.Subtle)
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp), content = content)
        }

        @Composable
        override fun TopBarTitle(text: String) = Text("· ${text.uppercase()} ·", style = appText(18f, 28f, Bold, Ink.Title, tracking = 2f))
    }

    /** Titik-titik di garis dasar baris (di belakang judul dan nominal). */
    @Composable
    private fun LeaderDots(modifier: Modifier) = Canvas(modifier) {
        val step = 6.dp.toPx()
        val y = size.height - 6.dp.toPx()
        var x = 0f
        while (x < size.width) {
            drawCircle(Ink.Subtle, radius = 1.dp.toPx(), center = Offset(x, y))
            x += step
        }
    }
}
