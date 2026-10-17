package com.liezert.itungen.ui.goals

import androidx.compose.ui.res.stringResource
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.liezert.itungen.R
import com.liezert.itungen.domain.model.AmountKind
import com.liezert.itungen.domain.model.Goal
import com.liezert.itungen.domain.model.GoalDeposit
import com.liezert.itungen.domain.model.formatRupiah
import com.liezert.itungen.domain.usecase.GoalError
import com.liezert.itungen.domain.usecase.TransferError
import com.liezert.itungen.domain.usecase.suggestedMonthly
import com.liezert.itungen.ui.components.AmountField
import com.liezert.itungen.ui.components.AmountText
import com.liezert.itungen.domain.model.MAX_TITLE
import com.liezert.itungen.ui.components.ItungTextField
import com.liezert.itungen.ui.components.Ico
import com.liezert.itungen.ui.components.IconCircle
import com.liezert.itungen.ui.components.ItungAlertDialog
import com.liezert.itungen.ui.components.ItungDatePickerDialog
import com.liezert.itungen.ui.components.rememberAppDatePickerState
import com.liezert.itungen.ui.theme.Bold
import com.liezert.itungen.ui.theme.Ink
import com.liezert.itungen.ui.theme.Medium
import com.liezert.itungen.ui.theme.Regular
import com.liezert.itungen.ui.theme.SemiBold
import com.liezert.itungen.ui.theme.appText
import com.liezert.itungen.ui.theme.formatDay
import java.time.LocalDate
import androidx.compose.ui.text.style.TextOverflow
import com.liezert.itungen.ui.design.ItungSurface
import com.liezert.itungen.ui.design.ItungButton
import com.liezert.itungen.ui.design.itungImageFrame
import androidx.compose.foundation.layout.aspectRatio

private const val DayMillis = 86_400_000L

@Composable
private fun GoalError.message() = stringResource(
    when (this) {
        GoalError.TITLE_REQUIRED -> R.string.goal_error_title
        GoalError.TARGET_REQUIRED -> R.string.goal_error_target
        GoalError.TARGET_TOO_LARGE -> R.string.goal_error_too_large
        GoalError.DATE_IN_PAST -> R.string.goal_error_past_date
        GoalError.PLAN_ABOVE_TARGET -> R.string.goal_error_plan_above
    }
)

@Composable
private fun TransferError.message() = stringResource(
    when (this) {
        TransferError.AMOUNT_REQUIRED -> R.string.transfer_error_amount
        TransferError.INSUFFICIENT_BALANCE -> R.string.transfer_error_balance
    }
)

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
    onPickImage: (Uri) -> Unit = {},
    onRemoveImage: () -> Unit = {},
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
        Text(stringResource(R.string.create_goal), style = appText(20f, 28f, Bold))
        ItungTextField(title, stringResource(R.string.goal_name_hint), maxLength = MAX_TITLE)
        ProductImagePicker(state.form.draftImage, title.text.toString(), onPickImage, onRemoveImage)
        AmountField(target, stringResource(R.string.goal_target_amount))
        OptionRow(
            label = targetDate?.formatDay() ?: stringResource(R.string.goal_target_date),
            trailing = if (targetDate != null) stringResource(R.string.action_remove) else null,
            onClick = { picking = true },
            onTrailing = { targetDay = null },
        )
        AmountField(planned, stringResource(R.string.goal_planned_monthly))
        if (suggestion != null && suggestion > 0) {
            // Nilai saran §5.1: bisa diterima atau diubah.
            SuggestionChip(stringResource(R.string.goal_suggestion, formatRupiah(suggestion))) {
                planned.setTextAndPlaceCursorAtEnd(suggestion.toString())
            }
        }
        FormMessage(state.form.goalError?.message() ?: formMessage(state.form))
        SheetButton(stringResource(R.string.create_goal), enabled = !state.form.busy) {
            onCreate(
                title.text.toString(),
                target.text.toString().toLongOrNull(),
                targetDate,
                planned.text.toString().toLongOrNull(),
            )
        }
    }

    val picker = rememberAppDatePickerState(targetDate ?: state.today.plusMonths(3)) { it >= state.today }
    ItungDatePickerDialog(
        visible = picking,
        state = picker,
        onConfirm = {
            picker.selectedDateMillis?.let { targetDay = it / DayMillis }
            picking = false
        },
        onDismiss = { picking = false },
    )
}

/** Isi sheet Tambah Dana (§4.2), dibuka dari detail goal: dari dompet utama ke dompet goal. */
@Composable
fun AddFundsSheet(
    goal: Goal,
    state: GoalsUiState,
    onAddFunds: (amount: Long?) -> Unit,
    onEdited: () -> Unit = {},
) {
    val amount = rememberTextFieldState()
    ClearOnEdit(onEdited) { amount.text.toString() }

    SheetBody {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            GoalImage(goal.imagePath, null, Modifier.size(64.dp, 48.dp).itungImageFrame()) { Ico(R.drawable.ic_goal_shoe) }
            Column {
                Text(stringResource(R.string.add_funds), style = appText(20f, 28f, Bold))
                Text(stringResource(R.string.goal_name_percent, goal.name, goal.percent), style = appText(13f, 18f, Regular, Ink.Gray), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        Text(
            stringResource(R.string.main_balance_is, formatRupiah(state.mainBalance)),
            style = appText(14f, 20f, Medium, if (state.mainBalance > 0) Ink.Muted else Ink.Red),
        )
        AmountField(amount, stringResource(R.string.amount_from_main))
        // Saran hanya nominal yang bisa langsung dipakai: tidak melebihi sisa target maupun saldo utama.
        val plan = minOf(goal.plannedMonthly, goal.remaining)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (plan in 1..state.mainBalance && plan != goal.remaining) {
                SuggestionChip(stringResource(R.string.chip_plan, formatRupiah(plan))) { amount.setTextAndPlaceCursorAtEnd(plan.toString()) }
            }
            if (goal.remaining in 1..state.mainBalance) {
                SuggestionChip(stringResource(R.string.chip_remaining, formatRupiah(goal.remaining))) { amount.setTextAndPlaceCursorAtEnd(goal.remaining.toString()) }
            }
        }
        FormMessage(state.form.transferError?.message() ?: formMessage(state.form))
        SheetButton(stringResource(R.string.add_funds), enabled = !state.form.busy) { onAddFunds(amount.text.toString().toLongOrNull()) }
    }
}

/** Konfirmasi Hapus target (dari kartu atau sheet): dananya kembali ke saldo utama, jadi tidak ada uang yang hilang. */
@Composable
fun DeleteGoalDialog(goal: Goal, visible: Boolean, onConfirm: () -> Unit, onDismiss: () -> Unit) = ItungAlertDialog(
    visible = visible,
    title = stringResource(R.string.delete_goal_title, goal.name),
    // Sejak Fase 1 hapus goal juga menghapus riwayat transfernya (uangnya kembali utuh ke saldo utama).
    text = if (goal.saved > 0) stringResource(R.string.delete_goal_body_funds, formatRupiah(goal.saved))
    else stringResource(R.string.delete_goal_body),
    confirmLabel = stringResource(R.string.action_delete),
    onConfirm = onConfirm,
    onDismiss = onDismiss,
    dismissLabel = stringResource(R.string.action_back),
    confirmColor = Ink.Red,
)

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
private fun OptionRow(
    label: String,
    trailing: String?,
    onClick: () -> Unit,
    onTrailing: () -> Unit,
    @DrawableRes icon: Int = R.drawable.ic_add_calendar,
) {
    ItungSurface(Modifier.fillMaxWidth(), contentPadding = PaddingValues(13.dp), onClick = onClick) {
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                IconCircle(icon, 32.dp, Ink.white(0.1f))
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
    ItungButton(
        onClick = onClick,
        modifier = Modifier.height(36.dp),
        surfaceColor = Ink.Accent.copy(alpha = 0.2f),
        contentPadding = 14.dp,
    ) { Text(label, style = appText(13f, 18f, SemiBold, Ink.AccentLight), maxLines = 1, overflow = TextOverflow.Ellipsis) }
}

@Composable
private fun formMessage(form: GoalFormState) = when {
    form.imageFailed -> stringResource(R.string.image_failed)
    form.failed -> stringResource(R.string.save_failed)
    else -> null
}

/**
 * Satu gambar produk per target: dipilih lewat Photo Picker sistem (hanya gambar, tanpa izin penyimpanan).
 * Tanpa gambar: satu baris "Tambah gambar produk"; dengan gambar: pratinjau lebar + Ganti / Hapus.
 */
@Composable
private fun ProductImagePicker(imagePath: String?, name: String, onPick: (Uri) -> Unit, onRemove: () -> Unit) {
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri -> uri?.let(onPick) }
    val launch = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
    if (imagePath == null) {
        OptionRow(label = stringResource(R.string.add_product_image), trailing = null, onClick = launch, onTrailing = {}, icon = R.drawable.ic_image)
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        GoalImage(imagePath, if (name.isBlank()) stringResource(R.string.product_image) else stringResource(R.string.goal_image_of, name), Modifier.fillMaxWidth().aspectRatio(16f / 9f).itungImageFrame()) {
            Ico(R.drawable.ic_goal_shoe_large)
        }
        Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(8.dp)) {
            SuggestionChip(stringResource(R.string.change_image), launch)
            ItungButton(onClick = onRemove, modifier = Modifier.height(36.dp), contentPadding = 14.dp) {
                Text(stringResource(R.string.remove_image), style = appText(13f, 18f, SemiBold, Ink.Red), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun FormMessage(message: String?) {
    if (message != null) Text(message, style = appText(14f, 20f, Medium, Ink.Red))
}

@Composable
private fun SheetButton(label: String, enabled: Boolean, onClick: () -> Unit) {
    ItungButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(52.dp),
        tint = Ink.Accent.copy(alpha = if (enabled) 1f else 0.5f),
        enabled = enabled,
    ) { Text(label, style = appText(16f, 24f, Bold, Color.White)) }
}
