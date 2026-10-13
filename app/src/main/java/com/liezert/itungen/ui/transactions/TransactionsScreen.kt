package com.liezert.itungen.ui.transactions

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kyant.shapes.Capsule
import com.liezert.glass.components.GlassSurface
import com.liezert.itungen.R
import com.liezert.itungen.domain.model.Transaction
import com.liezert.itungen.domain.model.TransactionType
import com.liezert.itungen.domain.model.formatSigned
import com.liezert.itungen.domain.usecase.filterTransactions
import com.liezert.itungen.ui.components.GlassIconButton
import com.liezert.itungen.ui.components.Ico
import com.liezert.itungen.ui.components.IconCircle
import com.liezert.itungen.ui.components.PillChip
import com.liezert.itungen.ui.components.TopBar
import com.liezert.itungen.ui.components.amountColor
import com.liezert.itungen.ui.components.bottomClearance
import com.liezert.itungen.ui.components.clockLabel
import com.liezert.itungen.ui.components.groupLabel
import com.liezert.itungen.ui.components.incoming
import com.liezert.itungen.ui.components.screenPad
import com.liezert.itungen.ui.components.style
import com.liezert.itungen.ui.theme.Bold
import com.liezert.itungen.ui.theme.Ink
import com.liezert.itungen.ui.theme.Medium
import com.liezert.itungen.ui.theme.Regular
import com.liezert.itungen.ui.theme.SemiBold
import com.liezert.itungen.ui.theme.appText

@Composable
fun TransactionsScreen(
    onBack: () -> Unit,
    viewModel: TransactionsViewModel = viewModel(factory = TransactionsViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    TransactionsContent(state, onBack)
}

@Composable
fun TransactionsContent(state: TransactionsUiState, onBack: () -> Unit, modifier: Modifier = Modifier) {
    var query by rememberSaveable { mutableStateOf("") }
    var selected by rememberSaveable { mutableIntStateOf(0) } // 0 = "Semua"
    val labels = listOf("Semua") + state.filters
    val visible = filterTransactions(state.transactions, query, state.filters.getOrNull(selected - 1))

    Column(modifier.fillMaxSize()) {
        TopBar(
            leading = { GlassIconButton(R.drawable.ic_back, onBack, "Kembali") },
            trailing = { Spacer(Modifier.size(48.dp)) },
        )
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(24.dp)) {
            Column(Modifier.screenPad(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Semua Transaksi", style = appText(20f, 28f, Bold, Ink.White, tracking = -0.5f))
                Text("${state.monthCount} transaksi bulan ini", style = appText(14f, 20f, Medium, Ink.Muted))
            }
            SearchPill(query, { query = it }, Modifier.screenPad())
            LazyRow(
                Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                itemsIndexed(labels) { i, label ->
                    PillChip(
                        label = label,
                        selected = i == selected,
                        onClick = { selected = i },
                        selectedText = appText(14f, 20f, SemiBold, Ink.Black),
                        unselectedText = appText(14f, 20f, Medium, Ink.Muted),
                    )
                }
            }
            TransactionListCard(visible, state, hasAny = state.transactions.isNotEmpty(), Modifier.screenPad().weight(1f))
        }
        Spacer(Modifier.height(bottomClearance()))
    }
}

@Composable
private fun SearchPill(query: String, onChange: (String) -> Unit, modifier: Modifier = Modifier) {
    GlassSurface(
        modifier.fillMaxWidth(),
        shape = Capsule(),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 17.dp, vertical = 13.dp),
            Arrangement.spacedBy(12.dp),
            Alignment.CenterVertically,
        ) {
            Ico(R.drawable.ic_search)
            BasicTextField(
                value = query,
                onValueChange = onChange,
                singleLine = true,
                textStyle = appText(14f, 20f, Regular, Ink.White),
                cursorBrush = SolidColor(Ink.White),
                modifier = Modifier.weight(1f).semantics { contentDescription = "Cari transaksi" },
                decorationBox = { inner ->
                    Box(Modifier.height(20.dp), contentAlignment = Alignment.CenterStart) {
                        if (query.isEmpty()) Text("Cari transaksi...", style = appText(14f, 20f, Regular, Ink.NavInactive))
                        inner()
                    }
                },
            )
        }
    }
}

@Composable
private fun TransactionListCard(
    transactions: List<Transaction>,
    state: TransactionsUiState,
    hasAny: Boolean,
    modifier: Modifier = Modifier,
) {
    val groups = transactions.groupBy { it.date }.toList() // urutan sumber: terbaru dulu
    GlassSurface(
        modifier.fillMaxWidth(),
        contentPadding = PaddingValues(start = 1.dp, end = 1.dp, top = 9.dp, bottom = 1.dp),
    ) {
        if (groups.isEmpty()) {
            Text(
                if (hasAny) "Tidak ada transaksi yang cocok." else "Belum ada transaksi. Tekan + untuk mencatat.",
                Modifier.padding(24.dp),
                style = appText(14f, 20f, Regular, Ink.Muted),
            )
            return@GlassSurface
        }
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            groups.forEachIndexed { index, (date, rows) ->
                if (index > 0) {
                    item(key = "divider-$date") { Box(Modifier.fillMaxWidth().height(1.dp).background(Ink.white(0.05f))) }
                }
                item(key = "group-$date") {
                    Column(
                        Modifier.padding(bottom = if (index < groups.lastIndex) 8.dp else 0.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        Text(
                            groupLabel(date, state.today),
                            Modifier.padding(horizontal = 8.dp),
                            style = appText(12f, 16f, Bold, Ink.NavInactive, tracking = 1.2f),
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            rows.forEach { TxRow(it, state) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TxRow(item: Transaction, state: TransactionsUiState) {
    val style = item.style()
    val categoryName = item.category?.name ?: "Transfer"
    val clock = item.clockLabel(state.zone)
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable { }.padding(8.dp),
        Arrangement.SpaceBetween,
        Alignment.CenterVertically,
    ) {
        Row(
            Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconCircle(style.rowIcon, 40.dp, if (item.type == TransactionType.INCOME) Ink.Green.copy(alpha = 0.2f) else Ink.white(0.1f))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(item.note ?: categoryName, style = appText(14f, 20f, SemiBold), maxLines = 1)
                Text(
                    if (clock != null) "$clock • $categoryName" else categoryName,
                    style = appText(12f, 16f, Regular, Ink.Muted),
                    maxLines = 1,
                )
            }
        }
        Text(
            formatSigned(item.amount, item.incoming),
            Modifier.padding(start = 12.dp),
            style = appText(14f, 20f, Bold, item.amountColor),
            maxLines = 1,
        )
    }
}
