package com.liezert.itungen.ui.addtransaction

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.text.input.TextFieldDecorator
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.liezert.glass.components.GlassDefaults
import com.liezert.glass.components.GlassSurface
import com.liezert.glass.components.LiquidBottomTab
import com.liezert.glass.components.LiquidBottomTabs
import com.liezert.glass.components.LiquidButton
import com.liezert.glass.components.LocalGlassBackdrop
import com.liezert.itungen.R
import com.liezert.itungen.domain.model.CategoryType
import com.liezert.itungen.domain.model.TransactionError
import com.liezert.itungen.domain.model.formatRupiah
import com.liezert.itungen.domain.usecase.evaluateKeypad
import com.liezert.itungen.domain.usecase.formatKeypad
import com.liezert.itungen.domain.usecase.hasOperator
import com.liezert.itungen.domain.usecase.keypadDigit
import com.liezert.itungen.domain.usecase.keypadOperator
import com.liezert.itungen.ui.components.GlassIconButton
import com.liezert.itungen.ui.components.Ico
import com.liezert.itungen.ui.components.IconCircle
import com.liezert.itungen.ui.components.LiquidDatePickerDialog
import com.liezert.itungen.ui.components.PillChip
import com.liezert.itungen.ui.components.relativeDay
import com.liezert.itungen.ui.components.screenPad
import com.liezert.itungen.ui.components.style
import com.liezert.itungen.ui.theme.Bold
import com.liezert.itungen.ui.theme.Ink
import com.liezert.itungen.ui.theme.Medium
import com.liezert.itungen.ui.theme.Regular
import com.liezert.itungen.ui.theme.SemiBold
import com.liezert.itungen.ui.theme.appText
import com.liezert.itungen.ui.theme.formatId
import java.time.LocalDate

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
) {
    var amount by rememberSaveable { mutableStateOf("") } // ekspresi keypad, mis. "15000+2000"
    val note = rememberTextFieldState()
    var showPicker by rememberSaveable { mutableStateOf(false) }
    val pickerState = rememberDatePickerState(
        initialSelectedDateMillis = state.date.toEpochDay() * DayMillis,
        // Tanggal di masa depan tidak boleh dipilih (DESIGN_SPEC §7.1).
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long) = utcTimeMillis / DayMillis <= state.today.toEpochDay()
        },
    )
    val focus = LocalFocusManager.current

    val income = state.type == CategoryType.INCOME
    val tint = if (income) Ink.Green else Ink.Red
    val result = evaluateKeypad(amount)

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
            GlassIconButton(R.drawable.ic_close, onClose, "Tutup")
            Text("Tambah Transaksi", style = appText(16f, 24f, Bold))
            Spacer(Modifier.size(48.dp))
        }

        // Muat satu layar tanpa scroll; scroll hanya cadangan untuk layar yang sangat pendek / saat keyboard catatan terbuka.
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(top = 8.dp, bottom = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            TypeToggle(state.type, tint, Modifier.screenPad(), onType)
            DateSelector(state.date, state.today, Modifier.screenPad()) { showPicker = true }
            // Tap area nominal menutup keyboard catatan sehingga keypad muncul lagi.
            AmountCard(amount, result, note, Modifier.screenPad()) { focus.clearFocus() }
            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(state.categories, key = { it.id }) { category ->
                    PillChip(
                        label = category.name,
                        selected = category.id == state.categoryId,
                        onClick = { onCategory(category.id) },
                        selectedText = appText(14f, 20f, SemiBold, Ink.Title),
                        unselectedText = appText(14f, 20f, Medium, Ink.Muted),
                        horizontal = 17.dp,
                        selectedFill = Ink.white(0.22f),
                        icon = category.style().chipIcon,
                    )
                }
            }
        }

        // Keypad + CTA menempel di bawah. Saat keyboard sistem terbuka (isi catatan) keypad disembunyikan.
        Column(
            Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            AnimatedVisibility(
                visible = !WindowInsets.isImeVisible,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut(),
            ) {
                AmountKeypad(
                    onDigit = { amount = keypadDigit(amount, it) },
                    onOperator = { amount = keypadOperator(amount, it) },
                    onBackspace = { amount = amount.dropLast(1) },
                )
            }

            val message = state.error?.message() ?: if (state.saveFailed) "Gagal menyimpan. Coba lagi." else null
            if (message != null) Text(message, style = appText(14f, 20f, Medium, Ink.Red))

            val enabled = (result ?: 0) > 0 && !state.saving
            LiquidButton(
                onClick = { onSave(result.toString(), note.text.toString()) },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                tint = tint.copy(alpha = if (enabled) 1f else 0.5f),
                enabled = enabled,
            ) {
                Text("Simpan Transaksi", style = appText(16f, 24f, Bold, Ink.White))
            }
        }
    }

    LiquidDatePickerDialog(
        visible = showPicker,
        state = pickerState,
        onConfirm = {
            pickerState.selectedDateMillis?.let { onDate(LocalDate.ofEpochDay(it / DayMillis)) }
            showPicker = false
        },
        onDismiss = { showPicker = false },
    )
}

private fun TransactionError.message() = when (this) {
    TransactionError.AMOUNT_REQUIRED -> "Isi nominal lebih dari 0."
    TransactionError.AMOUNT_TOO_LARGE -> "Nominal terlalu besar."
    TransactionError.CATEGORY_REQUIRED -> "Pilih kategori."
    TransactionError.DATE_IN_FUTURE -> "Tanggal tidak boleh di masa depan."
}

private val Types = listOf(CategoryType.EXPENSE to "Pengeluaran", CategoryType.INCOME to "Pemasukan")

/** Segmented control iOS 26: panel kaca LiquidBottomTabs dengan indikator yang bisa diseret. */
@Composable
private fun TypeToggle(type: CategoryType, tint: Color, modifier: Modifier = Modifier, onChange: (CategoryType) -> Unit) {
    val index = Types.indexOfFirst { it.first == type }
    LiquidBottomTabs(
        selectedTabIndex = { index },
        onTabSelected = { onChange(Types[it].first) },
        backdrop = LocalGlassBackdrop.current,
        tabsCount = Types.size,
        modifier = modifier.fillMaxWidth(),
        accentColor = tint,
        height = 52.dp,
    ) {
        Types.forEach { (t, label) ->
            LiquidBottomTab({ onChange(t) }) {
                Text(label, style = appText(14f, 20f, SemiBold, if (t == type) Ink.White else Ink.Muted))
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
    note: TextFieldState,
    modifier: Modifier = Modifier,
    onAmountClick: () -> Unit,
) {
    GlassSurface(modifier.fillMaxWidth()) {
        Column {
            Column(
                Modifier
                    .fillMaxWidth()
                    .clickable(interactionSource = null, indication = null, onClick = onAmountClick)
                    .padding(horizontal = 24.dp, vertical = 22.dp)
                    .semantics(mergeDescendants = true) {
                        contentDescription = "Nominal " + (result?.let(::formatRupiah) ?: "kosong")
                    },
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Rp", style = appText(24f, 32f, SemiBold, Ink.Gray))
                    BasicText(
                        if (expr.isEmpty()) "0" else formatKeypad(expr),
                        style = appText(40f, 49f, Bold, if (expr.isEmpty()) Ink.white(0.3f) else Ink.White),
                        maxLines = 1,
                        autoSize = TextAutoSize.StepBased(minFontSize = 18.sp, maxFontSize = 40.sp),
                    )
                }
                if (hasOperator(expr)) {
                    Text(
                        result?.let { "= " + formatRupiah(it) } ?: "Tidak bisa dibagi 0",
                        Modifier.padding(top = 2.dp),
                        style = appText(15f, 20f, Medium, if (result != null && result > 0) Ink.Muted else Ink.Red),
                    )
                }
            }
            Box(Modifier.fillMaxWidth().height(1.dp).background(Ink.white(0.08f)))
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 17.dp, vertical = 14.dp),
                Arrangement.spacedBy(12.dp),
                Alignment.CenterVertically,
            ) {
                Ico(R.drawable.ic_add_note)
                BasicTextField(
                    state = note,
                    modifier = Modifier.weight(1f).semantics { contentDescription = "Catatan" },
                    textStyle = appText(15f, 20f, Regular, Ink.White),
                    cursorBrush = SolidColor(Ink.White),
                    lineLimits = TextFieldLineLimits.SingleLine,
                    decorator = TextFieldDecorator { inner ->
                        Box(contentAlignment = Alignment.CenterStart) {
                            if (note.text.isEmpty()) {
                                Text("Ada catatan? (opsional)", style = appText(15f, 20f, Regular, Ink.Muted.copy(alpha = 0.5f)))
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
    listOf("0", "000", "<", "+"),
)

private val OperatorNames = mapOf("÷" to "bagi", "×" to "kali", "-" to "kurang", "+" to "tambah")

/** Keypad kalkulator (pengganti keyboard sistem untuk nominal): angka kaca, operator kaca lebih terang. */
@Composable
private fun AmountKeypad(onDigit: (String) -> Unit, onOperator: (Char) -> Unit, onBackspace: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        KeypadRows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { key ->
                    val operator = key in OperatorNames
                    LiquidButton(
                        onClick = {
                            when {
                                key == "<" -> onBackspace()
                                operator -> onOperator(key[0])
                                else -> onDigit(key)
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(54.dp)
                            .semantics {
                                contentDescription = when {
                                    key == "<" -> "Hapus angka"
                                    operator -> OperatorNames.getValue(key)
                                    else -> key
                                }
                            },
                        surfaceColor = if (operator) Ink.white(0.14f) else GlassDefaults.Surface,
                        contentPadding = 0.dp,
                    ) {
                        if (key == "<") {
                            Ico(R.drawable.ic_backspace)
                        } else {
                            Text(key, style = appText(if (operator) 26f else 24f, 30f, if (operator) Medium else SemiBold))
                        }
                    }
                }
            }
        }
    }
}

/** Pemilih tanggal ala baris dompet ShopeePay di GoPay: ikon, tanggal relatif + lengkap, panah bawah. */
@Composable
private fun DateSelector(date: LocalDate, today: LocalDate, modifier: Modifier = Modifier, onClick: () -> Unit) {
    GlassSurface(modifier.fillMaxWidth(), contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp), onClick = onClick) {
        Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(12.dp), Alignment.CenterVertically) {
            IconCircle(R.drawable.ic_add_calendar, 36.dp, Ink.white(0.1f))
            Text(
                buildAnnotatedString {
                    withStyle(SpanStyle(fontWeight = SemiBold, color = Ink.White)) { append(relativeDay(date, today)) }
                    withStyle(SpanStyle(fontWeight = Regular, color = Ink.Gray)) { append("  ·  " + date.formatId()) }
                },
                Modifier.weight(1f),
                style = appText(15f, 20f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Ico(R.drawable.ic_add_chevron, Modifier.rotate(90f))
        }
    }
}
