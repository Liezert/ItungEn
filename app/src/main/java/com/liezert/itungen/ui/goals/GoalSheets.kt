package com.liezert.itungen.ui.goals

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.flow.drop
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.liezert.glass.components.GlassSurface
import com.liezert.glass.components.LiquidButton
import com.liezert.itungen.R
import com.liezert.itungen.domain.model.Goal
import com.liezert.itungen.domain.model.formatRupiah
import com.liezert.itungen.domain.usecase.GoalError
import com.liezert.itungen.domain.usecase.TransferError
import com.liezert.itungen.domain.usecase.suggestedMonthly
import com.liezert.itungen.ui.components.AmountField
import com.liezert.itungen.domain.model.MAX_TITLE
import com.liezert.itungen.ui.components.GlassTextField
import com.liezert.itungen.ui.components.Ico
import com.liezert.itungen.ui.components.IconCircle
import com.liezert.itungen.ui.components.LiquidAlertDialog
import com.liezert.itungen.ui.components.LiquidDatePickerDialog
import com.liezert.itungen.ui.components.rememberIndonesianDatePickerState
import com.liezert.itungen.ui.theme.Bold
import com.liezert.itungen.ui.theme.Ink
import com.liezert.itungen.ui.theme.Medium
import com.liezert.itungen.ui.theme.Regular
import com.liezert.itungen.ui.theme.SemiBold
import com.liezert.itungen.ui.theme.appText
import com.liezert.itungen.ui.theme.formatId
import java.time.LocalDate
import androidx.compose.ui.text.style.TextOverflow

private const val DayMillis = 86_400_000L

private fun GoalError.message() = when (this) {
    GoalError.TITLE_REQUIRED -> "Isi nama target."
    GoalError.TARGET_REQUIRED -> "Isi nominal target lebih dari 0."
    GoalError.TARGET_TOO_LARGE -> "Nominal target terlalu besar."
    GoalError.DATE_IN_PAST -> "Tanggal target tidak boleh di masa lalu."
    GoalError.PLAN_ABOVE_TARGET -> "Setoran bulanan tidak boleh melebihi target dana."
}

private fun TransferError.message() = when (this) {
    TransferError.AMOUNT_REQUIRED -> "Isi nominal lebih dari 0."
    TransferError.INSUFFICIENT_BALANCE -> "Saldo utama tidak cukup."
}

/**
 * Isi sheet Buat Target (DESIGN_SPEC §6.6): nama, target, tanggal (opsional), setoran bulanan + nilai saran.
 * Ditaruh di dalam LiquidSheet oleh pemanggil.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateGoalSheet(
    state: GoalsUiState,
    onCreate: (title: String, target: Long?, targetDate: LocalDate?, plannedMonthly: Long?) -> Unit,
    /** Isian berubah: pesan dari percobaan sebelumnya dihapus (dulu bertahan sampai disimpan). */
    onEdited: () -> Unit = {},
) {
    val title = rememberTextFieldState()
    val target = rememberTextFieldState()
    val planned = rememberTextFieldState()
    var targetDay by rememberSaveable { mutableStateOf<Long?>(null) }
    ClearOnEdit(onEdited) { "${title.text}|${target.text}|${planned.text}|$targetDay" }
    var picking by rememberSaveable { mutableStateOf(false) }
    val targetDate = targetDay?.let(LocalDate::ofEpochDay)
    val suggestion = suggestedMonthly(target.text.toString().toLongOrNull() ?: 0, 0, targetDate, state.today)

    SheetBody {
        Text("Buat Target", style = appText(20f, 28f, Bold))
        GlassTextField(title, "Nama target, mis. Sepatu Lari", maxLength = MAX_TITLE)
        AmountField(target, "Target dana")
        OptionRow(
            label = targetDate?.formatId() ?: "Tanggal target (opsional)",
            trailing = if (targetDate != null) "Hapus" else null,
            onClick = { picking = true },
            onTrailing = { targetDay = null },
        )
        AmountField(planned, "Setoran bulanan (rencana)")
        if (suggestion != null && suggestion > 0) {
            // Nilai saran §5.1: bisa diterima atau diubah.
            SuggestionChip("Saran ${formatRupiah(suggestion)}/bulan · Pakai") {
                planned.setTextAndPlaceCursorAtEnd(suggestion.toString())
            }
        }
        FormMessage(state.form.goalError?.message() ?: if (state.form.failed) "Gagal menyimpan. Coba lagi." else null)
        SheetButton("Buat Target", enabled = !state.form.busy) {
            onCreate(
                title.text.toString(),
                target.text.toString().toLongOrNull(),
                targetDate,
                planned.text.toString().toLongOrNull(),
            )
        }
    }

    val picker = rememberIndonesianDatePickerState(targetDate ?: state.today.plusMonths(3)) { it >= state.today }
    LiquidDatePickerDialog(
        visible = picking,
        state = picker,
        onConfirm = {
            picker.selectedDateMillis?.let { targetDay = it / DayMillis }
            picking = false
        },
        onDismiss = { picking = false },
    )
}

/** Isi sheet Tambah Dana (§4.2): dari dompet utama ke dompet goal; pembatalan target ada di sini juga. */
@Composable
fun AddFundsSheet(
    goal: Goal,
    state: GoalsUiState,
    onAddFunds: (amount: Long?) -> Unit,
    onCancelGoal: () -> Unit,
    onEdited: () -> Unit = {},
) {
    val amount = rememberTextFieldState()
    ClearOnEdit(onEdited) { amount.text.toString() }
    var confirmCancel by rememberSaveable { mutableStateOf(false) }

    SheetBody {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            IconCircle(R.drawable.ic_goal_shoe_large, 48.dp, Ink.Accent.copy(alpha = 0.2f))
            Column {
                Text("Tambah Dana", style = appText(20f, 28f, Bold))
                Text("${goal.name} · ${goal.percent}% tercapai", style = appText(13f, 18f, Regular, Ink.Gray))
            }
        }
        Text(
            "Saldo utama: ${formatRupiah(state.mainBalance)}",
            style = appText(14f, 20f, Medium, if (state.mainBalance > 0) Ink.Muted else Ink.Red),
        )
        AmountField(amount, "Nominal dari saldo utama")
        // Saran hanya nominal yang bisa langsung dipakai: tidak melebihi sisa target maupun saldo utama.
        val plan = minOf(goal.plannedMonthly, goal.remaining)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (plan in 1..state.mainBalance && plan != goal.remaining) {
                SuggestionChip("Rencana ${formatRupiah(plan)}") { amount.setTextAndPlaceCursorAtEnd(plan.toString()) }
            }
            if (goal.remaining in 1..state.mainBalance) {
                SuggestionChip("Sisa ${formatRupiah(goal.remaining)}") { amount.setTextAndPlaceCursorAtEnd(goal.remaining.toString()) }
            }
        }
        FormMessage(state.form.transferError?.message() ?: if (state.form.failed) "Gagal menyimpan. Coba lagi." else null)
        SheetButton("Tambah Dana", enabled = !state.form.busy) { onAddFunds(amount.text.toString().toLongOrNull()) }
        LiquidButton(
            onClick = { confirmCancel = true },
            modifier = Modifier.align(Alignment.CenterHorizontally),
        ) { Text("Batalkan target", style = appText(14f, 20f, SemiBold, Ink.Red)) }
    }

    LiquidAlertDialog(
        visible = confirmCancel,
        title = "Batalkan \"${goal.name}\"?",
        text = if (goal.saved > 0) "Dana ${formatRupiah(goal.saved)} dikembalikan ke saldo utama, lalu target diarsipkan."
        else "Target diarsipkan.",
        confirmLabel = "Batalkan",
        onConfirm = {
            confirmCancel = false
            onCancelGoal()
        },
        onDismiss = { confirmCancel = false },
        dismissLabel = "Kembali",
        confirmColor = Ink.Red,
    )
}

/** Panggil [onEdited] setiap [value] (gabungan isian form) berubah setelah sheet terbuka. */
@Composable
private fun ClearOnEdit(onEdited: () -> Unit, value: () -> String) {
    val latest by rememberUpdatedState(onEdited)
    LaunchedEffect(Unit) { snapshotFlow(value).drop(1).collect { latest() } }
}

@Composable
private fun SheetBody(content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(start = 20.dp, end = 20.dp, bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        content = content,
    )
}

@Composable
private fun OptionRow(label: String, trailing: String?, onClick: () -> Unit, onTrailing: () -> Unit) {
    GlassSurface(Modifier.fillMaxWidth(), contentPadding = PaddingValues(13.dp), onClick = onClick) {
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                IconCircle(R.drawable.ic_add_calendar, 32.dp, Ink.white(0.1f))
                Text(label, style = appText(16f, 24f, Regular))
            }
            if (trailing != null) {
                Text(
                    trailing,
                    Modifier.clip(CircleShape).clickable(role = Role.Button, onClick = onTrailing).padding(8.dp),
                    style = appText(14f, 20f, SemiBold, Ink.AccentLight),
                )
            } else {
                Ico(R.drawable.ic_add_chevron)
            }
        }
    }
}

@Composable
private fun SuggestionChip(label: String, onClick: () -> Unit) {
    LiquidButton(
        onClick = onClick,
        modifier = Modifier.height(36.dp),
        surfaceColor = Ink.Accent.copy(alpha = 0.2f),
        contentPadding = 14.dp,
    ) { Text(label, style = appText(13f, 18f, SemiBold, Ink.AccentLight), maxLines = 1, overflow = TextOverflow.Ellipsis) }
}

@Composable
private fun FormMessage(message: String?) {
    if (message != null) Text(message, style = appText(14f, 20f, Medium, Ink.Red))
}

@Composable
private fun SheetButton(label: String, enabled: Boolean, onClick: () -> Unit) {
    LiquidButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(52.dp),
        tint = Ink.Accent.copy(alpha = if (enabled) 1f else 0.5f),
        enabled = enabled,
    ) { Text(label, style = appText(16f, 24f, Bold, Color.White)) }
}
