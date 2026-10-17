package com.liezert.itungen.ui.addtransaction

import androidx.compose.foundation.layout.heightIn
import com.liezert.itungen.ui.components.SegmentLabel
import java.time.temporal.ChronoUnit
import com.liezert.itungen.domain.model.RupiahPrefix
import androidx.compose.ui.res.stringResource
import com.liezert.itungen.ui.components.displayName
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.maxLength
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.text.input.TextFieldDecorator
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.flow.drop
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.liezert.itungen.R
import com.liezert.itungen.domain.model.MAX_AMOUNT
import com.liezert.itungen.domain.model.MAX_NOTE
import com.liezert.itungen.domain.model.Category
import com.liezert.itungen.domain.model.CategoryType
import com.liezert.itungen.domain.model.TransactionError
import com.liezert.itungen.domain.model.formatRupiah
import com.liezert.itungen.domain.usecase.evaluateKeypad
import com.liezert.itungen.domain.usecase.formatKeypad
import com.liezert.itungen.domain.usecase.hasOperator
import com.liezert.itungen.domain.usecase.keypadDigit
import com.liezert.itungen.domain.usecase.keypadOperator
import com.liezert.itungen.domain.usecase.keypadRounds
import com.liezert.itungen.ui.components.ItungIconButton
import com.liezert.itungen.ui.components.Ico
import com.liezert.itungen.ui.components.IconCircle
import com.liezert.itungen.ui.components.ItungAlertDialog
import com.liezert.itungen.ui.components.ItungDatePickerDialog
import androidx.activity.compose.BackHandler
import com.liezert.itungen.ui.components.rememberAppDatePickerState
import com.liezert.itungen.ui.components.relativeDay
import com.liezert.itungen.ui.components.screenPad
import com.liezert.itungen.ui.components.style
import com.liezert.itungen.ui.theme.Bold
import com.liezert.itungen.ui.theme.Ink
import com.liezert.itungen.ui.theme.Medium
import com.liezert.itungen.ui.theme.Regular
import com.liezert.itungen.ui.theme.SemiBold
import com.liezert.itungen.ui.theme.appText
import com.liezert.itungen.ui.theme.formatDay
import java.time.LocalDate
import com.liezert.itungen.ui.design.ItungTabs
import com.liezert.itungen.ui.design.ItungTab
import com.liezert.itungen.ui.design.ItungSurface
import com.liezert.itungen.ui.design.ItungDefaults
import com.liezert.itungen.ui.design.ItungButton
import com.liezert.itungen.ui.design.ItungDivider

private const val DayMillis = 86_400_000L

@Composable
fun AddTransactionScreen(
    onClose: () -> Unit,
    viewModel: AddTransactionViewModel = viewModel(factory = AddTransactionViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(state.saved) { if (state.saved) onClose() }
    AddTransactionContent(
        state = state,
        onClose = onClose,
        onType = viewModel::selectType,
        onCategory = viewModel::selectCategory,
        onDate = viewModel::selectDate,
        onSave = viewModel::save,
        onEdited = viewModel::clearError,
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddTransactionContent(
    state: AddTransactionUiState,
    onClose: () -> Unit,
    onType: (CategoryType) -> Unit,
    onCategory: (Long) -> Unit,
    onDate: (LocalDate) -> Unit,
    onSave: (amountDigits: String, note: String) -> Unit,
    modifier: Modifier = Modifier,
    /** Nominal atau catatan berubah: pesan dari percobaan simpan sebelumnya dihapus. */
    onEdited: () -> Unit = {},
) {
    var amount by rememberSaveable { mutableStateOf("") } // ekspresi keypad, mis. "15000+2000"
    val edit = { next: String ->
        amount = next
        onEdited()
    }
    val note = rememberTextFieldState()
    // Mengubah transaksi: isi nominal & catatan lama sekali setelah dimuat (tidak menimpa ketikan setelah rotasi).
    LaunchedEffect(state.initialAmount) {
        val initial = state.initialAmount ?: return@LaunchedEffect
        if (amount.isEmpty()) {
            amount = initial.toString()
            note.setTextAndPlaceCursorAtEnd(state.initialNote.orEmpty())
        }
    }
    var showPicker by rememberSaveable { mutableStateOf(false) }
    // Tanggal di masa depan tidak boleh dipilih (DESIGN_SPEC §7.1).
    val pickerState = rememberAppDatePickerState(state.date) { it <= state.today }
    LaunchedEffect(state.date) { pickerState.selectedDateMillis = state.date.toEpochDay() * DayMillis }
    val focus = LocalFocusManager.current

    val income = state.type == CategoryType.INCOME
    val tint = if (income) Ink.Income else Ink.Expense
    val result = evaluateKeypad(amount)
    // Nominal divalidasi dari isian saat ini (bukan disimpan sebagai galat), jadi pesannya hilang begitu isian benar.
    val amountMessage = when {
        amount.isEmpty() -> null
        hasOperator(amount) && result == null -> stringResource(R.string.keypad_divide_zero)
        result != null && result > MAX_AMOUNT -> TransactionError.AMOUNT_TOO_LARGE.message()
        result != null && result <= 0 -> stringResource(R.string.keypad_result_positive)
        else -> null
    } ?: state.error?.message()
    LaunchedEffect(note) { snapshotFlow { note.text.toString() }.drop(1).collect { onEdited() } }
    // Tutup/Kembali dengan isian yang belum disimpan: tanya dulu (dulu langsung dibuang).
    val dirty = if (state.editing) {
        state.initialAmount != null && (amount != state.initialAmount.toString() || note.text.toString() != state.initialNote.orEmpty())
    } else {
        amount.isNotEmpty() || note.text.isNotEmpty()
    }
    var confirmClose by rememberSaveable { mutableStateOf(false) }
    val close = { if (dirty) confirmClose = true else onClose() }
    BackHandler(enabled = dirty) { confirmClose = true }

    Column(
        modifier
            .fillMaxSize()
            .background(Ink.Black)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
    ) {
        // header khusus (frame ini menggantikan top app bar)
        Row(
            Modifier
                .fillMaxWidth()
                .height(64.dp)
                .padding(horizontal = 12.dp),
            Arrangement.SpaceBetween,
            Alignment.CenterVertically,
        ) {
            ItungIconButton(R.drawable.ic_close, close, stringResource(R.string.action_close))
            Text(stringResource(if (state.editing) R.string.edit_transaction_title else R.string.add_transaction_title), style = appText(16f, 24f, Bold))
            Spacer(Modifier.size(48.dp))
        }

        // Jenis, tanggal, nominal. Scroll hanya cadangan untuk layar yang sangat pendek / saat keyboard catatan terbuka;
        // galat nominal tampil di kartu ini sehingga keypad di bawah tidak bergeser di bawah jari.
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(top = 8.dp, bottom = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            TypeToggle(state.type, tint, Modifier.screenPad(), onType)
            DateSelector(state.date, state.today, Modifier.screenPad()) { showPicker = true }
            // Tap area nominal menutup keyboard catatan sehingga keypad muncul lagi.
            AmountCard(amount, result, amountMessage, note, Modifier.screenPad()) { focus.clearFocus() }
        }

        // Kategori selalu terlihat (dulu grid kategori ada di area scroll, tertutup keypad), lalu keypad + CTA.
        CategoryRow(state.categories, state.categoryId, onCategory)
        // Saat keyboard sistem terbuka (isi catatan) keypad disembunyikan.
        Column(
            Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            AnimatedVisibility(
                visible = !WindowInsets.isImeVisible,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut(),
            ) {
                AmountKeypad(
                    onDigit = { edit(keypadDigit(amount, it)) },
                    onOperator = { edit(keypadOperator(amount, it)) },
                    onBackspace = { edit(amount.dropLast(1)) },
                    onClear = { edit("") },
                    // "=": ekspresi diganti hasilnya. Simpan juga memakai hasil, jadi "=" tidak wajib.
                    onEquals = { result?.takeIf { it in 1..MAX_AMOUNT }?.let { edit(it.toString()) } },
                )
            }

            // Tinggi tetap: alasan tombol belum aktif / gagal simpan tertulis di tombolnya, bukan di baris tambahan.
            val enabled = result != null && result in 1..MAX_AMOUNT && state.categoryId != null && !state.saving
            ItungButton(
                onClick = { onSave(result.toString(), note.text.toString()) },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                tint = if (enabled) tint else Color.Unspecified,
                enabled = enabled,
            ) {
                Text(
                    stringResource(
                        when {
                            state.categoryId == null -> R.string.pick_category_first
                            state.saveFailed -> R.string.save_failed_retry
                            state.editing -> R.string.save_changes
                            else -> R.string.save_transaction
                        }
                    ),
                    style = appText(16f, 24f, Bold, if (enabled) Color.White else Ink.Muted),
                )
            }
        }
    }

    ItungAlertDialog(
        visible = confirmClose,
        title = stringResource(R.string.discard_title),
        text = stringResource(R.string.discard_body),
        confirmLabel = stringResource(R.string.discard_action),
        onConfirm = {
            confirmClose = false
            onClose()
        },
        onDismiss = { confirmClose = false },
        dismissLabel = stringResource(R.string.keep_editing),
        confirmColor = Ink.Red,
    )

    ItungDatePickerDialog(
        visible = showPicker,
        state = pickerState,
        onConfirm = {
            pickerState.selectedDateMillis?.let { onDate(LocalDate.ofEpochDay(it / DayMillis)) }
            showPicker = false
        },
        onDismiss = { showPicker = false },
    )
}

@Composable
private fun TransactionError.message() = stringResource(
    when (this) {
        TransactionError.AMOUNT_REQUIRED -> R.string.transfer_error_amount
        TransactionError.AMOUNT_TOO_LARGE -> R.string.tx_error_too_large
        TransactionError.CATEGORY_REQUIRED -> R.string.tx_error_category
        TransactionError.DATE_IN_FUTURE -> R.string.tx_error_future
    }
)

private val Types = listOf(CategoryType.EXPENSE to R.string.type_expense, CategoryType.INCOME to R.string.type_income)

/** Segmented control iOS 26: panel kaca LiquidBottomTabs dengan indikator yang bisa diseret. */
@Composable
private fun TypeToggle(type: CategoryType, tint: Color, modifier: Modifier = Modifier, onChange: (CategoryType) -> Unit) {
    val index = Types.indexOfFirst { it.first == type }
    ItungTabs(
        selectedTabIndex = { index },
        onTabSelected = { onChange(Types[it].first) },
        tabsCount = Types.size,
        modifier = modifier.fillMaxWidth(),
        accentColor = tint,
        height = 52.dp,
    ) {
        Types.forEach { (t, label) ->
            ItungTab({ onChange(t) }, selected = t == type) {
                SegmentLabel(stringResource(label), selected = t == type)
            }
        }
    }
}

/**
 * Kartu nominal ala GoPay: "Rp" + ekspresi keypad (mengecil otomatis bila panjang) + hasil hitung bila ada operator,
 * lalu catatan opsional di bagian bawah kartu yang sama.
 */
@Composable
private fun AmountCard(
    expr: String,
    result: Long?,
    /** Galat nominal saat ini; menggantikan baris hasil. */
    message: String?,
    note: TextFieldState,
    modifier: Modifier = Modifier,
    onAmountClick: () -> Unit,
) {
    val amountLabel = result?.let { stringResource(R.string.amount_a11y, formatRupiah(it)) } ?: stringResource(R.string.amount_a11y_empty)
    val noteLabel = stringResource(R.string.note)
    ItungSurface(modifier.fillMaxWidth()) {
        Column {
            Column(
                Modifier
                    .fillMaxWidth()
                    .clickable(interactionSource = null, indication = null, onClick = onAmountClick)
                    .padding(horizontal = 24.dp, vertical = 18.dp)
                    .semantics(mergeDescendants = true) {
                        contentDescription = amountLabel
                    },
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(RupiahPrefix, style = appText(24f, 32f, SemiBold, Ink.Gray))
                    BasicText(
                        if (expr.isEmpty()) "0" else formatKeypad(expr),
                        style = appText(40f, 49f, Bold, if (expr.isEmpty()) Ink.white(0.3f) else Ink.White),
                        maxLines = 1,
                        autoSize = TextAutoSize.StepBased(minFontSize = 18.sp, maxFontSize = 40.sp),
                    )
                }
                when {
                    message != null -> Text(message, Modifier.padding(top = 2.dp), style = appText(15f, 20f, Medium, Ink.Red))
                    hasOperator(expr) && result != null -> Text(
                        stringResource(if (keypadRounds(expr)) R.string.keypad_result_rounded else R.string.keypad_result, formatRupiah(result)),
                        Modifier.padding(top = 2.dp),
                        style = appText(15f, 20f, Medium, Ink.Muted),
                    )
                }
            }
            ItungDivider(color = Ink.white(0.08f))
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 17.dp, vertical = 14.dp),
                Arrangement.spacedBy(12.dp),
                Alignment.CenterVertically,
            ) {
                Ico(R.drawable.ic_add_note)
                BasicTextField(
                    state = note,
                    modifier = Modifier.weight(1f).semantics { contentDescription = noteLabel },
                    inputTransformation = InputTransformation.maxLength(MAX_NOTE),
                    textStyle = appText(15f, 20f, Regular, Ink.White),
                    cursorBrush = SolidColor(Ink.White),
                    lineLimits = TextFieldLineLimits.SingleLine,
                    decorator = TextFieldDecorator { inner ->
                        Box(contentAlignment = Alignment.CenterStart) {
                            if (note.text.isEmpty()) {
                                Text(stringResource(R.string.note_hint), style = appText(15f, 20f, Regular, Ink.Muted.copy(alpha = 0.5f)))
                            }
                            inner()
                        }
                    },
                )
            }
        }
    }
}

private val KeypadRows = listOf(
    listOf("1", "2", "3", "÷"),
    listOf("4", "5", "6", "×"),
    listOf("7", "8", "9", "-"),
    listOf("000", "0", "<", "+"),
)

private val OperatorNames = mapOf("÷" to R.string.key_divide, "×" to R.string.key_times, "-" to R.string.key_minus, "+" to R.string.key_plus)
private val KeyHeight = 44.dp

/**
 * Keypad kalkulator (pengganti keyboard sistem untuk nominal): angka kaca, operator kaca lebih terang,
 * baris bawah C (kosongkan) dan = (ganti ekspresi dengan hasilnya).
 */
@Composable
private fun AmountKeypad(
    onDigit: (String) -> Unit,
    onOperator: (Char) -> Unit,
    onBackspace: () -> Unit,
    onClear: () -> Unit,
    onEquals: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        KeypadRows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { key ->
                    val operator = key in OperatorNames
                    Key(
                        onClick = {
                            when {
                                key == "<" -> onBackspace()
                                operator -> onOperator(key[0])
                                else -> onDigit(key)
                            }
                        },
                        description = when {
                            key == "<" -> stringResource(R.string.key_backspace)
                            operator -> stringResource(OperatorNames.getValue(key))
                            else -> key
                        },
                        operator = operator,
                        modifier = Modifier.weight(1f),
                    ) {
                        if (key == "<") Ico(R.drawable.ic_backspace)
                        else Text(key, style = appText(if (operator) 26f else 24f, 30f, if (operator) Medium else SemiBold))
                    }
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Key(onClear, stringResource(R.string.key_clear), operator = false, Modifier.weight(1f)) {
                Text("C", style = appText(22f, 28f, SemiBold, Ink.Red))
            }
            Key(onEquals, stringResource(R.string.key_equals), operator = true, Modifier.weight(3f)) {
                Text("=", style = appText(26f, 30f, Medium))
            }
        }
    }
}

@Composable
private fun Key(
    onClick: () -> Unit,
    description: String,
    operator: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    ItungButton(
        onClick = onClick,
        modifier = modifier.height(KeyHeight).semantics { contentDescription = description },
        surfaceColor = if (operator) Ink.white(0.14f) else ItungDefaults.surface,
        contentPadding = 0.dp,
    ) { content() }
}

/**
 * Satu baris chip kategori di atas keypad, selalu terlihat tanpa scroll layar. Nama utuh (tidak "Transport…");
 * chip terakhir yang terpotong di tepi menandakan baris bisa digeser, dan kategori terpilih digulir ke dalam layar.
 */
@Composable
private fun CategoryRow(categories: List<Category>, selectedId: Long?, onSelect: (Long) -> Unit) {
    val listState = rememberLazyListState()
    LaunchedEffect(selectedId, categories) {
        val index = categories.indexOfFirst { it.id == selectedId }
        val info = listState.layoutInfo
        val fullyVisible = info.visibleItemsInfo.any { it.index == index && it.offset >= 0 && it.offset + it.size <= info.viewportEndOffset }
        if (index >= 0 && !fullyVisible) listState.animateScrollToItem(index)
    }
    LazyRow(
        state = listState,
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        contentPadding = PaddingValues(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(categories, key = { it.id }) { category ->
            val selected = category.id == selectedId
            ItungButton(
                onClick = { onSelect(category.id) },
                modifier = Modifier.heightIn(min = 40.dp).semantics { this.selected = selected },
                surfaceColor = if (selected) Ink.white(0.22f) else ItungDefaults.surface,
                contentPadding = 12.dp,
            ) {
                Ico(category.style().chipIcon)
                Text(
                    category.displayName(),
                    Modifier.padding(start = 6.dp),
                    style = appText(13f, 18f, if (selected) SemiBold else Medium, if (selected) Ink.Title else Ink.Muted),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/** Pemilih tanggal ala baris dompet ShopeePay di GoPay: ikon, tanggal relatif + lengkap, panah bawah. */
@Composable
private fun DateSelector(date: LocalDate, today: LocalDate, modifier: Modifier = Modifier, onClick: () -> Unit) {
    ItungSurface(modifier.fillMaxWidth(), contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp), onClick = onClick) {
        Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(12.dp), Alignment.CenterVertically) {
            IconCircle(R.drawable.ic_add_calendar, 36.dp, Ink.white(0.1f))
            // Label relatif ("Hari ini") + tanggal lengkap; lebih dari 6 hari label relatifnya sudah tanggal, jadi tidak
            // diulang. Dua baris: huruf monospace (Struk) atau font besar dulu memotongnya menjadi "Sabtu, 17 Okt 2…".
            val recent = ChronoUnit.DAYS.between(date, today) in 0..6
            val relative = relativeDay(date, today)
            val full = date.formatDay()
            Text(
                buildAnnotatedString {
                    withStyle(SpanStyle(fontWeight = SemiBold, color = Ink.White)) { append(if (recent) relative else full) }
                    if (recent) withStyle(SpanStyle(fontWeight = Regular, color = Ink.Gray)) { append("  ·  $full") }
                },
                Modifier.weight(1f),
                style = appText(15f, 20f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Ico(R.drawable.ic_add_chevron, Modifier.rotate(90f))
        }
    }
}
