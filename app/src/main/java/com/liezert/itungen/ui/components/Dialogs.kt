package com.liezert.itungen.ui.components

import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.BoxWithConstraints
import com.liezert.itungen.R
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SelectableDates
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import java.time.LocalDate
import androidx.compose.material3.Text
import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.liezert.itungen.ui.theme.Ink
import com.liezert.itungen.ui.theme.Medium
import com.liezert.itungen.ui.theme.Regular
import com.liezert.itungen.ui.theme.appText
import com.liezert.itungen.ui.design.ItungDialog
import com.liezert.itungen.ui.design.ItungButton

/** Aksen dialog tema gelap dari repo Kyant0. */
private val DialogAccent = Color(0xFF0091FF)

/** Tombol "Batal" di dialog repo: containerColor α0.2 (gelap #121212, terang #FAFAFA). */
private val CancelSurface: Color @Composable get() = Color(if (Ink.dark) 0xFF121212 else 0xFFFAFAFA).copy(0.2f)

/** Alert kaca, tata letak dari DialogContent repo Kyant0: judul, isi, lalu dua tombol kapsul sama lebar. */
@Composable
fun ItungAlertDialog(
    visible: Boolean,
    title: String,
    text: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    dismissLabel: String = stringResource(R.string.action_cancel),
    confirmColor: Color = DialogAccent,
) {
    ItungDialog(visible, onDismiss) {
        Text(title, Modifier.padding(28.dp, 24.dp, 28.dp, 12.dp), style = appText(24f, 30f, Medium))
        Text(
            text,
            Modifier
                // "plus lighter" di tema gelap seperti repo; di tema terang biasa (Plus membuat teks hitam hilang).
                .then(if (Ink.dark) Modifier.graphicsLayer(blendMode = BlendMode.Plus) else Modifier)
                .padding(24.dp, 12.dp, 24.dp, 12.dp),
            style = appText(15f, 21f, Regular, Ink.White.copy(0.68f)),
        )
        DialogButtons(dismissLabel, onDismiss, confirmLabel, onConfirm, confirmColor)
    }
}

private const val DayMillis = 86_400_000L

/**
 * State pemilih tanggal dalam bahasa app. Nama bulan dan hari diambil dari locale state saat state dibuat, bukan
 * saat dialog digambar, jadi locale harus diberikan di sini; konfigurasi di [ItungDatePickerDialog] hanya
 * menerjemahkan teks tombol dan judul. [selectable] = tanggal yang boleh dipilih.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun rememberAppDatePickerState(initialSelectedDate: LocalDate, selectable: (LocalDate) -> Boolean): DatePickerState {
    val isSelectable by rememberUpdatedState(selectable)
    val locale = dateLocale()
    return remember(locale) {
        DatePickerState(
            locale = locale,
            initialSelectedDateMillis = initialSelectedDate.toEpochDay() * DayMillis,
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long) = isSelectable(LocalDate.ofEpochDay(utcTimeMillis / DayMillis))
            },
        )
    }
}

/** Pemilih tanggal Material di dalam kaca dialog (latar picker transparan agar kacanya terlihat). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItungDatePickerDialog(
    visible: Boolean,
    state: DatePickerState,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    ItungDialog(visible, onDismiss, Modifier.padding(horizontal = 8.dp, vertical = 16.dp)) {
        // Bahasa app, apa pun bahasa HP-nya (nama bulan/hari dan teks "Pilih tanggal"), sama seperti seluruh app.
        val configuration = LocalConfiguration.current
        val context = LocalContext.current
        val locale = dateLocale()
        val app = remember(configuration, locale) { Configuration(configuration).apply { setLocale(locale) } }
        val appContext = remember(context, app) { context.createConfigurationContext(app) }
        CompositionLocalProvider(LocalConfiguration provides app, LocalContext provides appContext) {
            DatePicker(
                state,
                Modifier.padding(top = 8.dp),
                colors = DatePickerDefaults.colors(containerColor = Color.Transparent),
                showModeToggle = false,
            )
        }
        DialogButtons(stringResource(R.string.action_cancel), onDismiss, stringResource(R.string.action_ok), onConfirm)
    }
}

/**
 * Batal + konfirmasi, sama lebar. Bila label terpanjang tidak muat di setengah lebar (huruf besar, "Delete" di bahasa
 * Inggris), keduanya bertumpuk selebar dialog; dulu labelnya dipecah di tengah kata ("Kemba / li", "Delet / e").
 */
@Composable
private fun DialogButtons(
    dismissLabel: String,
    onDismiss: () -> Unit,
    confirmLabel: String,
    onConfirm: () -> Unit,
    confirmColor: Color = DialogAccent,
) {
    val style = appText(16f, 22f)
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    BoxWithConstraints(Modifier.padding(24.dp, 12.dp, 24.dp, 24.dp).fillMaxWidth()) {
        val widest = with(density) { listOf(dismissLabel, confirmLabel).maxOf { measurer.measure(it, style).size.width }.toDp() }
        val stacked = widest > (maxWidth - ButtonGap) / 2 - LabelInset
        val dismiss: @Composable (Modifier) -> Unit = {
            ItungButton(onDismiss, it, surfaceColor = CancelSurface) { Text(dismissLabel, style = style) }
        }
        val confirm: @Composable (Modifier) -> Unit = {
            ItungButton(onConfirm, it, tint = confirmColor) { Text(confirmLabel, style = style.copy(color = Color.White)) }
        }
        if (stacked) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                dismiss(Modifier.fillMaxWidth())
                confirm(Modifier.fillMaxWidth())
            }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(ButtonGap)) {
                dismiss(Modifier.weight(1f))
                confirm(Modifier.weight(1f))
            }
        }
    }
}

private val ButtonGap = 16.dp

/** Padding kiri-kanan tombol tema (16 dp per sisi) + sedikit kelonggaran untuk bingkai tema. */
private val LabelInset = 36.dp
