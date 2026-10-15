package com.liezert.itungen.ui.transactions

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
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
import com.liezert.itungen.ui.components.GlassIconButton
import com.liezert.itungen.ui.components.Ico
import com.liezert.itungen.ui.components.IconCircle
import com.liezert.itungen.ui.components.LiquidAlertDialog
import com.liezert.itungen.ui.components.categoryLabel
import com.liezert.itungen.ui.components.toastMessage
import com.liezert.itungen.ui.components.PillChip
import com.liezert.itungen.ui.components.TopBar
import com.liezert.itungen.ui.components.UndoToast
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
    val undo by viewModel.undo.entries.collectAsStateWithLifecycle()
    TransactionsContent(
        state, onBack,
        onDelete = viewModel::delete,
        onSearch = viewModel::search,
        onFilter = viewModel::filter,
        onLoadMore = viewModel::loadMore,
    )
    UndoToast(undo.toastMessage(), viewModel.undo::undoLatest)
}

@Composable
fun TransactionsContent(
    state: TransactionsUiState,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    /** Pemasukan/pengeluaran yang digeser ke kiri. Transfer goal tidak bisa dihapus. */
    onDelete: (Transaction) -> Unit = {},
    onSearch: (String) -> Unit = {},
    /** null = "Semua". */
    onFilter: (String?) -> Unit = {},
    onLoadMore: () -> Unit = {},
) {
    // Tekan lama = alternatif geser (geser dari tepi kanan bisa bentrok dengan gestur Kembali).
    var confirmDelete by remember { mutableStateOf<Transaction?>(null) }
    val labels = listOf<String?>(null) + state.filters

    Column(modifier.fillMaxSize()) {
        TopBar(
            leading = { GlassIconButton(R.drawable.ic_back, onBack, "Kembali") },
            trailing = { Spacer(Modifier.size(48.dp)) },
        )
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(24.dp)) {
            Column(Modifier.screenPad(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Semua Transaksi", style = appText(20f, 28f, Bold, Ink.White, tracking = -0.5f))
                Text(
                    "${state.monthCount} transaksi bulan ini · geser ke kiri atau tekan lama untuk menghapus",
                    style = appText(14f, 20f, Medium, Ink.Muted),
                )
            }
            SearchPill(state.query, onSearch, Modifier.screenPad())
            LazyRow(
                Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(labels) { label ->
                    PillChip(
                        label = label ?: "Semua",
                        selected = label == state.category,
                        onClick = { onFilter(label) },
                        selectedText = appText(14f, 20f, SemiBold, Ink.Black),
                        unselectedText = appText(14f, 20f, Medium, Ink.Muted),
                    )
                }
            }
            TransactionListCard(state, onDelete, onLongPress = { confirmDelete = it }, onLoadMore, Modifier.screenPad().weight(1f))
        }
        Spacer(Modifier.height(bottomClearance()))
    }

    val pending = confirmDelete
    LiquidAlertDialog(
        visible = pending != null,
        title = "Hapus transaksi?",
        text = pending?.let { "${it.note ?: it.category?.name ?: "Transaksi"} · ${formatSigned(it.amount, it.incoming)}" }.orEmpty(),
        confirmLabel = "Hapus",
        onConfirm = {
            pending?.let(onDelete)
            confirmDelete = null
        },
        onDismiss = { confirmDelete = null },
        confirmColor = Ink.Red,
    )
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
    state: TransactionsUiState,
    onDelete: (Transaction) -> Unit,
    onLongPress: (Transaction) -> Unit,
    onLoadMore: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    // Muat lebih banyak saat tinggal 10 item lagi sebelum akhir yang sudah dimuat.
    LaunchedEffect(listState) {
        snapshotFlow { listState.layoutInfo.run { (visibleItemsInfo.lastOrNull()?.index ?: 0) >= totalItemsCount - 10 } }
            .collect { nearEnd -> if (nearEnd) onLoadMore() }
    }
    val groups = state.transactions.groupBy { it.date }.toList() // urutan sumber: terbaru dulu
    GlassSurface(
        modifier.fillMaxWidth(),
        contentPadding = PaddingValues(start = 1.dp, end = 1.dp, top = 9.dp, bottom = 1.dp),
    ) {
        if (groups.isEmpty()) {
            if (state.loading) return@GlassSurface
            Text(
                if (state.hasAny) "Tidak ada transaksi yang cocok." else "Belum ada transaksi. Tekan + untuk mencatat.",
                Modifier.padding(24.dp),
                style = appText(14f, 20f, Regular, Ink.Muted),
            )
            return@GlassSurface
        }
        // Satu item per baris (bukan per tanggal): hari dengan banyak transaksi tetap ringan discroll.
        LazyColumn(state = listState, contentPadding = PaddingValues(16.dp)) {
            groups.forEachIndexed { index, (date, rows) ->
                if (index > 0) {
                    item(key = "divider-$date", contentType = "divider") {
                        Box(Modifier.padding(top = 20.dp, bottom = 16.dp).fillMaxWidth().height(1.dp).background(Ink.white(0.05f)))
                    }
                }
                item(key = "group-$date", contentType = "header") {
                    Text(
                        groupLabel(date, state.today),
                        Modifier.padding(start = 8.dp, end = 8.dp, bottom = 16.dp),
                        style = appText(12f, 16f, Bold, Ink.NavInactive, tracking = 1.2f),
                    )
                }
                items(rows, key = { it.id }, contentType = { "row" }) {
                    Box(Modifier.padding(bottom = 4.dp)) { SwipeToDelete(it, onDelete, onLongPress) { TxRow(it, state) } }
                }
            }
        }
    }
}

@Composable
private fun TxRow(item: Transaction, state: TransactionsUiState) {
    val style = item.style()
    val categoryName = item.categoryLabel
    val clock = item.clockLabel(state.zone)
    Row(
        Modifier.fillMaxWidth().padding(8.dp),
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

/**
 * Geser ke kiri → latar merah "Hapus"; lepas melewati batas = hapus (bisa diurungkan). Tekan lama = konfirmasi hapus,
 * dan TalkBack mendapat aksi "Hapus". Transfer goal tidak bisa dihapus.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeToDelete(
    item: Transaction,
    onDelete: (Transaction) -> Unit,
    onLongPress: (Transaction) -> Unit,
    content: @Composable () -> Unit,
) {
    if (item.type != TransactionType.INCOME && item.type != TransactionType.EXPENSE) {
        content()
        return
    }
    val state = rememberSwipeToDismissBoxState()
    LaunchedEffect(state.currentValue) {
        if (state.currentValue == SwipeToDismissBoxValue.EndToStart) onDelete(item)
    }
    SwipeToDismissBox(
        state = state,
        enableDismissFromStartToEnd = false,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .pointerInput(item.id) { detectTapGestures(onLongPress = { onLongPress(item) }) }
            .semantics { customActions = listOf(CustomAccessibilityAction("Hapus") { onDelete(item); true }) },
        backgroundContent = {
            if (state.dismissDirection != SwipeToDismissBoxValue.EndToStart) return@SwipeToDismissBox
            Row(
                Modifier.fillMaxSize().background(Ink.Red).padding(horizontal = 20.dp),
                Arrangement.End,
                Alignment.CenterVertically,
            ) {
                Image(painterResource(R.drawable.ic_delete), null, Modifier.size(20.dp), colorFilter = ColorFilter.tint(Color.White))
                Text("Hapus", Modifier.padding(start = 8.dp), style = appText(14f, 20f, SemiBold, Color.White))
            }
        },
    ) {
        // Saat digeser, baris diberi latar agar merah hanya terlihat di bagian yang terbuka; saat diam tetap transparan di atas kaca.
        val swiping = state.dismissDirection == SwipeToDismissBoxValue.EndToStart
        Box(Modifier.background(if (swiping) Ink.Black else Color.Transparent)) { content() }
    }
}

