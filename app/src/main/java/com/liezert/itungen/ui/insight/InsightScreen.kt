package com.liezert.itungen.ui.insight

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.liezert.glass.components.GlassCard
import com.liezert.glass.components.GlassSurface
import com.liezert.itungen.R
import com.liezert.itungen.domain.model.formatRupiah
import com.liezert.itungen.domain.usecase.CategorySpend
import com.liezert.itungen.ui.components.GlassIconButton
import com.liezert.itungen.ui.components.IconCircle
import com.liezert.itungen.ui.components.TopBar
import com.liezert.itungen.ui.components.bottomClearance
import com.liezert.itungen.ui.components.screenPad
import com.liezert.itungen.ui.components.style
import com.liezert.itungen.ui.theme.Bold
import com.liezert.itungen.ui.theme.Ink
import com.liezert.itungen.ui.theme.Medium
import com.liezert.itungen.ui.theme.Regular
import com.liezert.itungen.ui.theme.SemiBold
import com.liezert.itungen.ui.theme.appText

@Composable
fun InsightScreen(
    onHistory: () -> Unit,
    viewModel: InsightViewModel = viewModel(factory = InsightViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    InsightContent(state, onHistory)
}

@Composable
fun InsightContent(state: InsightUiState, onHistory: () -> Unit, modifier: Modifier = Modifier) {
    val total = state.total

    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        TopBar(
            leading = { Spacer(Modifier.size(48.dp)) }, // hamburger ditunda sampai punya isi (DESIGN_SPEC §11.2)
            trailing = { GlassIconButton(R.drawable.ic_history, onHistory, "Semua transaksi") },
        )
        Spacer(Modifier.height(8.dp))
        Column(Modifier.screenPad(), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text("Insight", style = appText(20f, 25f, Bold, Ink.White, tracking = -0.5f))
            Text("Pengeluaran bulan ini", style = appText(13f, 19.5f, Medium, Ink.Gray))
        }
        Spacer(Modifier.height(24.dp))
        SummaryCard(state.spending, total, Modifier.screenPad())
        Spacer(Modifier.height(22.dp))
        Column(Modifier.screenPad(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("Detail Kategori", style = appText(15f, 22.5f, SemiBold, Ink.white(0.9f)))
            if (state.spending.isEmpty() && !state.loading) {
                Text("Belum ada pengeluaran bulan ini.", style = appText(14f, 20f, Regular, Ink.Gray))
            }
            state.spending.forEach { BreakdownItem(it, it.total.toFloat() / total) }
        }
        Spacer(Modifier.height(bottomClearance()))
    }
}

private fun shortName(name: String) = when (name) {
    "Makanan" -> "Makan"
    "Transportasi" -> "Trans"
    else -> name
}

@Composable
private fun SummaryCard(spending: List<CategorySpend>, total: Long, modifier: Modifier = Modifier) {
    GlassCard(
        modifier.fillMaxWidth(),
        tint = Brush.verticalGradient(listOf(Ink.Red.copy(alpha = if (total > 0) 0.14f else 0f), Color.Transparent)),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("TOTAL PENGELUARAN BULAN INI", style = appText(14f, 20f, SemiBold, Ink.Gray, tracking = 0.7f))
            Text(
                if (total > 0) "-${formatRupiah(total)}" else formatRupiah(0),
                style = appText(28f, 42f, Bold, if (total > 0) Ink.Red else Ink.White, tracking = -0.7f),
            )
        }
        // Bar bersegmen per kategori; kosong bila belum ada pengeluaran.
        Box(Modifier.fillMaxWidth().height(16.dp).padding(top = 8.dp)) {
            Row(Modifier.fillMaxSize().clip(CircleShape).background(Ink.white(0.1f))) {
                spending.forEach { Box(Modifier.weight(it.total.toFloat()).fillMaxHeight().background(it.category.style().color)) }
            }
        }
        if (spending.isNotEmpty()) {
            FlowRow(
                Modifier.fillMaxWidth().padding(top = 4.dp, start = 4.dp, end = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                spending.forEach {
                    val pct = (it.total * 100f / total).toInt()
                    Text("${shortName(it.category.name)} $pct%", style = appText(11f, 16.5f, Medium, Ink.Gray))
                }
            }
        }
    }
}

@Composable
private fun BreakdownItem(item: CategorySpend, share: Float) {
    val style = item.category.style()
    GlassSurface(Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().padding(start = 17.dp, end = 17.dp, top = 17.dp, bottom = 29.dp),
            Arrangement.SpaceBetween,
            Alignment.CenterVertically,
        ) {
            Row(
                Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconCircle(style.bigIcon, 48.dp, style.color.copy(alpha = 0.15f))
                Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                    Text(item.category.name, style = appText(16f, 24f, SemiBold), maxLines = 1)
                    Text("${item.count} transaksi", style = appText(13f, 19.5f, Regular, Ink.Gray))
                }
            }
            Text("-${formatRupiah(item.total)}", Modifier.padding(start = 12.dp), style = appText(16f, 24f, Bold, Ink.Red), maxLines = 1)
        }
        // garis progres tipis di tepi bawah kartu
        Box(Modifier.align(Alignment.BottomStart).fillMaxWidth().height(4.dp).background(style.color.copy(alpha = 0.2f))) {
            Box(Modifier.fillMaxWidth(share).fillMaxHeight().background(style.color))
        }
    }
}
