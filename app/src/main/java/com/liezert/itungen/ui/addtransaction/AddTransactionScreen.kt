package com.liezert.itungen.ui.addtransaction

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.OutputTransformation
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.insert
import androidx.compose.foundation.text.input.maxLength
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.then
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.liezert.glass.tokens.GlassColors
import com.liezert.itungen.domain.model.CategoryType
import com.liezert.itungen.domain.model.MAX_AMOUNT
import com.liezert.itungen.domain.model.TransactionError
import com.liezert.itungen.ui.theme.formatId
import java.time.LocalDate

private const val DAY_MS = 86_400_000L

private val DigitsOnly = InputTransformation {
    if (!asCharSequence().all(Char::isDigit)) revertAllChanges()
}

/** Tampilan "1500000" → "1.500.000"; isi aslinya tetap angka saja. */
private val ThousandsSeparator = OutputTransformation {
    var i = length - 3
    while (i > 0) {
        insert(i, ".")
        i -= 3
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionScreen(
    onClose: () -> Unit,
    viewModel: AddTransactionViewModel = viewModel(factory = AddTransactionViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val amount = rememberTextFieldState()
    val note = rememberTextFieldState()
    var pickingDate by rememberSaveable { mutableStateOf(false) }

    val close = {
        viewModel.reset()
        onClose()
    }
    BackHandler(onBack = close)
    LaunchedEffect(state.saved) { if (state.saved) close() }

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Tambah Transaksi", color = GlassColors.TextPrimary, fontSize = 20.sp, modifier = Modifier.weight(1f))
            IconButton(onClick = close, modifier = Modifier.semantics { contentDescription = "Tutup" }) {
                Text("✕", color = GlassColors.TextPrimary, fontSize = 20.sp)
            }
        }

        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            listOf(CategoryType.EXPENSE to "Pengeluaran", CategoryType.INCOME to "Pemasukan").forEachIndexed { index, (type, label) ->
                SegmentedButton(
                    selected = state.type == type,
                    onClick = { viewModel.selectType(type) },
                    shape = SegmentedButtonDefaults.itemShape(index, 2),
                ) { Text(label) }
            }
        }

        OutlinedTextField(
            state = amount,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Nominal") },
            prefix = { Text("Rp ") },
            inputTransformation = InputTransformation.maxLength(MAX_AMOUNT.toString().length).then(DigitsOnly),
            outputTransformation = ThousandsSeparator,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            lineLimits = TextFieldLineLimits.SingleLine,
            isError = state.error == TransactionError.AMOUNT_REQUIRED || state.error == TransactionError.AMOUNT_TOO_LARGE,
        )

        Text("Kategori", color = GlassColors.TextSecondary, fontSize = 12.sp)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            state.categories.forEach { category ->
                FilterChip(
                    selected = category.id == state.categoryId,
                    onClick = { viewModel.selectCategory(category.id) },
                    label = { Text(category.name) },
                )
            }
        }

        OutlinedTextField(
            state = note,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Nama (opsional)") },
            lineLimits = TextFieldLineLimits.SingleLine,
        )

        OutlinedButton(onClick = { pickingDate = true }, modifier = Modifier.fillMaxWidth()) {
            Text("Tanggal: ${state.date.formatId()}")
        }

        val message = state.error?.message() ?: if (state.saveFailed) "Gagal menyimpan. Coba lagi." else null
        if (message != null) Text(message, color = GlassColors.AccentRed)

        Spacer(Modifier.height(8.dp))
        Button(
            onClick = { viewModel.save(amount.text.toString(), note.text.toString()) },
            enabled = !state.saving,
            modifier = Modifier.fillMaxWidth().height(52.dp),
        ) { Text("Simpan") }
    }

    if (pickingDate) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = state.date.toEpochDay() * DAY_MS,
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long) = utcTimeMillis / DAY_MS <= state.today.toEpochDay()
            },
        )
        DatePickerDialog(
            onDismissRequest = { pickingDate = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { viewModel.selectDate(LocalDate.ofEpochDay(it / DAY_MS)) }
                    pickingDate = false
                }) { Text("Pilih") }
            },
            dismissButton = { TextButton(onClick = { pickingDate = false }) { Text("Batal") } },
        ) { DatePicker(pickerState) }
    }
}

private fun TransactionError.message() = when (this) {
    TransactionError.AMOUNT_REQUIRED -> "Isi nominal lebih dari 0."
    TransactionError.AMOUNT_TOO_LARGE -> "Nominal terlalu besar."
    TransactionError.CATEGORY_REQUIRED -> "Pilih kategori."
    TransactionError.DATE_IN_FUTURE -> "Tanggal tidak boleh di masa depan."
}
