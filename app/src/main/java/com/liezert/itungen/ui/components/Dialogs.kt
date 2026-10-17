package com.liezert.itungen.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
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
import java.util.Locale
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.liezert.glass.components.LiquidButton
import com.liezert.glass.components.LiquidDialog
import com.liezert.itungen.ui.theme.Ink
import com.liezert.itungen.ui.theme.Medium
import com.liezert.itungen.ui.theme.Regular
import com.liezert.itungen.ui.theme.appText

private val Indonesian = Locale.forLanguageTag("id-ID")

/** Aksen dialog tema gelap dari repo Kyant0. */
private val DialogAccent = Color(0xFF0091FF)

/** Tombol "Batal" di dialog repo: containerColor α0.2 (gelap #121212, terang #FAFAFA). */
private val CancelSurface: Color @Composable get() = Color(if (Ink.dark) 0xFF121212 else 0xFFFAFAFA).copy(0.2f)

/** Alert kaca, tata letak dari DialogContent repo Kyant0: judul, isi, lalu dua tombol kapsul sama lebar. */
@Composable
fun LiquidAlertDialog(
    visible: Boolean,
    title: String,
    text: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    dismissLabel: String = "Batal",
    confirmColor: Color = DialogAccent,
) {
    LiquidDialog(visible, onDismiss) {
        Text(title, Modifier.padding(28.dp, 24.dp, 28.dp, 12.dp), style = appText(24f, 30f, Medium))
        Text(
            text,
            Modifier
                // "plus lighter" di tema gelap seperti repo; di tema terang biasa (Plus membuat teks hitam hilang).
                .then(if (Ink.dark) Modifier.graphicsLayer(blendMode = BlendMode.Plus) else Modifier)
                .padding(24.dp, 12.dp, 24.dp, 12.dp),
            style = appText(15f, 21f, Regular, Ink.White.copy(0.68f)),
        )
        DialogButtons {
            LiquidButton(onDismiss, modifier = Modifier.weight(1f), surfaceColor = CancelSurface) {
                Text(dismissLabel, style = appText(16f, 22f))
            }
            LiquidButton(onConfirm, modifier = Modifier.weight(1f), tint = confirmColor) {
                Text(confirmLabel, style = appText(16f, 22f, color = Color.White))
            }
        }
    }
}

private const val DayMillis = 86_400_000L

/**
 * State pemilih tanggal berbahasa Indonesia. Nama bulan dan hari diambil dari locale state saat state dibuat, bukan
 * saat dialog digambar, jadi locale harus diberikan di sini; konfigurasi di [LiquidDatePickerDialog] hanya
 * menerjemahkan teks tombol dan judul. [selectable] = tanggal yang boleh dipilih.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun rememberIndonesianDatePickerState(initialSelectedDate: LocalDate, selectable: (LocalDate) -> Boolean): DatePickerState {
    val isSelectable by rememberUpdatedState(selectable)
    return remember {
        DatePickerState(
            locale = Indonesian,
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
fun LiquidDatePickerDialog(
    visible: Boolean,
    state: DatePickerState,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    LiquidDialog(visible, onDismiss, Modifier.padding(horizontal = 8.dp, vertical = 16.dp)) {
        // Bahasa Indonesia apa pun bahasa HP-nya (nama bulan/hari dan teks "Pilih tanggal"), sama seperti seluruh app.
        val configuration = LocalConfiguration.current
        val context = LocalContext.current
        val indonesian = remember(configuration) { Configuration(configuration).apply { setLocale(Indonesian) } }
        val indonesianContext = remember(context, indonesian) { context.createConfigurationContext(indonesian) }
        CompositionLocalProvider(LocalConfiguration provides indonesian, LocalContext provides indonesianContext) {
            DatePicker(
                state,
                Modifier.padding(top = 8.dp),
                colors = DatePickerDefaults.colors(containerColor = Color.Transparent),
                showModeToggle = false,
            )
        }
        DialogButtons {
            LiquidButton(onDismiss, modifier = Modifier.weight(1f), surfaceColor = CancelSurface) {
                Text("Batal", style = appText(16f, 22f))
            }
            LiquidButton(onConfirm, modifier = Modifier.weight(1f), tint = DialogAccent) {
                Text("OK", style = appText(16f, 22f, color = Color.White))
            }
        }
    }
}

@Composable
private fun DialogButtons(content: @Composable RowScope.() -> Unit) {
    Row(
        Modifier
            .padding(24.dp, 12.dp, 24.dp, 24.dp)
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}
