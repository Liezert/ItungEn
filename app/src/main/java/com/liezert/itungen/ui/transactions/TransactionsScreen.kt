package com.liezert.itungen.ui.transactions

import androidx.compose.foundation.layout.heightIn
import com.liezert.itungen.ui.components.displayNote
import com.liezert.itungen.ui.components.displayName
import com.liezert.itungen.ui.components.categoryNameLabel
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kyant.shapes.Capsule
import com.liezert.itungen.R
import com.liezert.itungen.domain.model.Transaction
import com.liezert.itungen.domain.model.TransactionType
import com.liezert.itungen.ui.components.amountDescription
import com.liezert.itungen.ui.components.AmountText
import com.liezert.itungen.ui.components.amountKind
import com.liezert.itungen.ui.components.ItungIconButton
import com.liezert.itungen.ui.components.Ico
import com.liezert.itungen.ui.components.IconCircle
import com.liezert.itungen.ui.components.ItungAlertDialog
import com.liezert.itungen.ui.components.categoryLabel
import com.liezert.itungen.ui.components.toastMessage
import com.liezert.itungen.ui.components.PillChip
import com.liezert.itungen.ui.components.TopBar
import com.liezert.itungen.ui.components.UndoToast
import com.liezert.itungen.ui.components.AboveNavBar
import com.liezert.itungen.ui.components.bottomClearance
import com.liezert.itungen.ui.components.clockLabel
import com.liezert.itungen.ui.components.groupLabel
import com.liezert.itungen.ui.components.screenPad
import com.liezert.itungen.ui.components.style
import com.liezert.itungen.ui.theme.Bold
import com.liezert.itungen.ui.theme.Ink
import com.liezert.itungen.ui.theme.Medium
import com.liezert.itungen.ui.theme.Regular
import com.liezert.itungen.ui.theme.SemiBold
import com.liezert.itungen.ui.theme.appText
import androidx.compose.ui.text.style.TextOverflow
import com.liezert.itungen.ui.design.ItungSurface
import com.liezert.itungen.ui.design.ItungDivider
import com.liezert.itungen.ui.design.ItungTransactionRow
import com.liezert.itungen.ui.design.SurfaceRole

@Composable
fun TransactionsScreen(
    onBack: () -> Unit,
    /** Ketuk baris pemasukan/pengeluaran: buka layar ubah. */
    onEdit: (Long) -> Unit = {},
    /** Ketuk baris transfer target: transfer dikelola di Goals. */
    onOpenGoals: () -> Unit = {},
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
        onEdit = { onEdit(it.id) },
        onOpenGoals = onOpenGoals,
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
    onEdit: (Transaction) -> Unit = {},
    onOpenGoals: () -> Unit = {},
) {
    // Tekan lama = alternatif geser (geser dari tepi kanan bisa bentrok dengan gestur Kembali).
    // Id (bukan Transaction) agar dialog konfirmasi hapus tetap terbuka setelah rotasi.
    var confirmDeleteId by rememberSaveable { mutableStateOf<Long?>(null) }
    val confirmDelete = confirmDeleteId?.let { id -> state.transactions.firstOrNull { it.id == id } }
    val labels = listOf<String?>(null) + state.filters

    Column(modifier.fillMaxSize()) {
        TopBar(
            leading = { ItungIconButton(R.drawable.ic_back, onBack, stringResource(R.string.action_back)) },
            trailing = { Spacer(Modifier.size(48.dp)) },
        )
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(24.dp)) {
            Column(Modifier.screenPad(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(stringResource(R.string.all_transactions_title), style = appText(20f, 28f, Bold, Ink.White, tracking = -0.5f))
                // Saat mencari/memfilter, jumlah yang relevan adalah hasilnya (dulu tetap "N transaksi bulan ini").
                val filtering = state.query.isNotBlank() || state.category != null
                val count = when {
                    !filtering -> pluralStringResource(R.plurals.tx_this_month, state.monthCount, state.monthCount)
                    state.canLoadMore -> stringResource(R.string.tx_matches_more, state.transactions.size)
                    else -> pluralStringResource(R.plurals.tx_matches, state.transactions.size, state.transactions.size)
                }
                Text(
                    stringResource(R.string.tx_list_hint, count),
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
                        label = label?.let { categoryNameLabel(it) } ?: stringResource(R.string.filter_all),
                        selected = label == state.category,
                        onClick = { onFilter(label) },
                        selectedText = appText(14f, 20f, SemiBold, Ink.Black),
                        unselectedText = appText(14f, 20f, Medium, Ink.Muted),
                    )
                }
            }
            TransactionListCard(state, onDelete, onLongPress = { confirmDeleteId = it.id }, onEdit, onOpenGoals, onLoadMore, Modifier.screenPad().weight(1f))
        }
        // Layar ini tanpa FAB: cukup lolos dari navbar (dulu menyisakan ruang FAB kosong di bawah kartu).
        Spacer(Modifier.height(bottomClearance(AboveNavBar)))
    }

    val pending = confirmDelete
    ItungAlertDialog(
        visible = pending != null,
        title = stringResource(R.string.delete_tx_title),
        text = pending?.let {
            stringResource(R.string.delete_tx_body, it.displayNote() ?: it.category?.displayName() ?: stringResource(R.string.transaction), amountDescription(it.amount, it.amountKind))
        }.orEmpty(),
        confirmLabel = stringResource(R.string.action_delete),
        onConfirm = {
            pending?.let(onDelete)
            confirmDeleteId = null
        },
        onDismiss = { confirmDeleteId = null },
        confirmColor = Ink.Red,
    )
}

@Composable
private fun SearchPill(query: String, onChange: (String) -> Unit, modifier: Modifier = Modifier) {
    val searchLabel = stringResource(R.string.search_transactions)
    // Teks yang diketik dipegang di sini (sinkron dengan keyboard). Bila nilainya menunggu putaran ViewModel yang
    // asinkron, huruf yang diketik cepat hilang ("ojek" jadi "e"); ViewModel cukup menerima salinannya untuk kueri.
    var text by rememberSaveable { mutableStateOf(query) }
    ItungSurface(
        modifier.fillMaxWidth(),
        shape = Capsule(),
        role = SurfaceRole.FIELD,
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 17.dp, vertical = 13.dp),
            Arrangement.spacedBy(12.dp),
            Alignment.CenterVertically,
        ) {
            Ico(R.drawable.ic_search)
            BasicTextField(
                value = text,
                onValueChange = {
                    text = it
                    onChange(it)
                },
                singleLine = true,
                textStyle = appText(14f, 20f, Regular, Ink.White),
                cursorBrush = SolidColor(Ink.White),
                modifier = Modifier.weight(1f).semantics { contentDescription = searchLabel },
                decorationBox = { inner ->
                    Box(Modifier.heightIn(min = 20.dp), contentAlignment = Alignment.CenterStart) {
                        if (text.isEmpty()) Text(stringResource(R.string.search_hint), style = appText(14f, 20f, Regular, Ink.NavInactive))
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
    onEdit: (Transaction) -> Unit,
    onOpenGoals: () -> Unit,
    onLoadMore: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    // Cari/filter berubah: mulai dari atas lagi. LazyColumn menjaga posisi per key, jadi setelah pencarian dikosongkan
    // daftar dulu tetap di hasil terakhir (mis. September), seolah transaksi hari ini hilang. Kembali dari layar Ubah
    // (cari/filter sama) posisi tetap.
    val searchKey = state.query + "\u0000" + state.category
    var shownFor by rememberSaveable { mutableStateOf(searchKey) }
    LaunchedEffect(searchKey) {
        if (searchKey != shownFor) {
            listState.scrollToItem(0)
            shownFor = searchKey
        }
    }
    // Muat lebih banyak saat tinggal 10 item lagi sebelum akhir yang sudah dimuat.
    LaunchedEffect(listState) {
        snapshotFlow { listState.layoutInfo.run { (visibleItemsInfo.lastOrNull()?.index ?: 0) >= totalItemsCount - 10 } }
            .collect { nearEnd -> if (nearEnd) onLoadMore() }
    }
    val groups = state.transactions.groupBy { it.date }.toList() // urutan sumber: terbaru dulu
    ItungSurface(
        modifier.fillMaxWidth(),
        contentPadding = PaddingValues(start = 1.dp, end = 1.dp, top = 9.dp, bottom = 1.dp),
        role = SurfaceRole.CARD,
    ) {
        if (groups.isEmpty()) {
            if (state.loading) return@ItungSurface
            Text(
                stringResource(if (state.hasAny) R.string.tx_no_match else R.string.recent_empty),
                Modifier.padding(24.dp),
                style = appText(14f, 20f, Regular, Ink.Muted),
            )
            return@ItungSurface
        }
        // Satu item per baris (bukan per tanggal): hari dengan banyak transaksi tetap ringan discroll.
        LazyColumn(state = listState, contentPadding = PaddingValues(16.dp)) {
            groups.forEachIndexed { index, (date, rows) ->
                if (index > 0) {
                    item(key = "divider-$date", contentType = "divider") {
                        ItungDivider(Modifier.padding(top = 20.dp, bottom = 16.dp), Ink.white(0.05f))
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
                    Box(Modifier.padding(bottom = 4.dp)) { SwipeToDelete(it, onDelete, onLongPress, onEdit, onOpenGoals) { TxRow(it, state) } }
                }
            }
        }
    }
}

@Composable
private fun TxRow(item: Transaction, state: TransactionsUiState) {
    val style = item.style()
    val categoryName = item.categoryLabel()
    val clock = item.clockLabel(state.zone)
    ItungTransactionRow(
        icon = { IconCircle(style.rowIcon, 40.dp, if (item.type == TransactionType.INCOME) Ink.Income.copy(alpha = 0.2f) else Ink.white(0.1f)) },
        title = { Text(item.displayNote() ?: categoryName, style = appText(14f, 20f, SemiBold), maxLines = 2, overflow = TextOverflow.Ellipsis) },
        subtitle = {
            Text(
                if (clock != null) stringResource(R.string.time_and_category, clock, categoryName) else categoryName,
                style = appText(12f, 16f, Regular, Ink.Muted),
                maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
        },
        amount = { AmountText(item.amount, item.amountKind, appText(14f, 20f, Bold, Ink.Gray), arrow = true) },
        modifier = Modifier.padding(8.dp),
        dense = true,
    )
}

/**
 * Geser ke kiri → latar merah "Hapus"; lepas melewati batas = hapus (bisa diurungkan). Ketuk = ubah, tekan lama =
 * konfirmasi hapus; TalkBack membacakan keduanya ("Ubah", "Hapus"). Transfer goal tidak bisa diubah/dihapus.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeToDelete(
    item: Transaction,
    onDelete: (Transaction) -> Unit,
    onLongPress: (Transaction) -> Unit,
    onEdit: (Transaction) -> Unit,
    onOpenGoals: () -> Unit,
    content: @Composable () -> Unit,
) {
    val deleteLabel = stringResource(R.string.action_delete)
    // Transfer target tidak bisa diubah/dihapus di sini; ketukan membuka Goals (dulu tidak bereaksi sama sekali).
    if (item.type != TransactionType.INCOME && item.type != TransactionType.EXPENSE) {
        Box(Modifier.clip(RoundedCornerShape(12.dp)).clickable(onClickLabel = stringResource(R.string.open_goals), role = Role.Button, onClick = onOpenGoals)) {
            content()
        }
        return
    }
    // Hapus saat geseran dikonfirmasi, tetapi kotak tidak pernah masuk keadaan "terhapus": status itu disimpan
    // LazyColumn per key, jadi baris yang dipulihkan lewat Urungkan (key sama) akan langsung terhapus lagi.
    // confirmValueChange bisa terpanggil lebih dari sekali dalam satu geseran: hapus hanya sekali per tampilan baris.
    val onDeleteLatest by rememberUpdatedState(onDelete)
    val deleted = remember { booleanArrayOf(false) }
    val state = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart && !deleted[0]) {
                deleted[0] = true
                onDeleteLatest(item)
            }
            false
        },
    )
    SwipeToDismissBox(
        state = state,
        enableDismissFromStartToEnd = false,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .semantics { customActions = listOf(CustomAccessibilityAction(deleteLabel) { onDelete(item); true }) },
        backgroundContent = {
            if (state.dismissDirection != SwipeToDismissBoxValue.EndToStart) return@SwipeToDismissBox
            Row(
                Modifier.fillMaxSize().background(Ink.Red).padding(horizontal = 20.dp),
                Arrangement.End,
                Alignment.CenterVertically,
            ) {
                Image(painterResource(R.drawable.ic_delete), null, Modifier.size(20.dp), colorFilter = ColorFilter.tint(Color.White))
                Text(deleteLabel, Modifier.padding(start = 8.dp), style = appText(14f, 20f, SemiBold, Color.White))
            }
        },
    ) {
        // Saat digeser, baris diberi latar agar merah hanya terlihat di bagian yang terbuka; saat diam tetap transparan di atas kaca.
        val swiping = state.dismissDirection == SwipeToDismissBoxValue.EndToStart
        Box(
            Modifier
                .background(if (swiping) Ink.Black else Color.Transparent)
                .combinedClickable(
                    onClickLabel = stringResource(R.string.action_edit),
                    onLongClickLabel = deleteLabel,
                    onLongClick = { onLongPress(item) },
                    onClick = { onEdit(item) },
                ),
        ) { content() }
    }
}

