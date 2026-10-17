package com.liezert.itungen.ui.components

import com.liezert.itungen.domain.model.RupiahPrefix
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.BasicSecureTextField
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.OutputTransformation
import androidx.compose.foundation.text.input.TextFieldDecorator
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.insert
import androidx.compose.foundation.text.input.maxLength
import androidx.compose.foundation.text.input.then
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.liezert.itungen.domain.model.MAX_AMOUNT
import com.liezert.itungen.ui.theme.Bold
import com.liezert.itungen.ui.theme.Ink
import com.liezert.itungen.ui.theme.Medium
import com.liezert.itungen.ui.theme.Regular
import com.liezert.itungen.ui.theme.SemiBold
import com.liezert.itungen.ui.theme.appText
import com.liezert.itungen.ui.design.ItungSurface
import com.liezert.itungen.ui.design.SurfaceRole

/** Hanya angka, maksimal 12 digit, tanpa nol di depan (nominal selalu > 0, DESIGN_SPEC §7.1). */
val AmountInput = InputTransformation.maxLength(MAX_AMOUNT.toString().length).then(
    InputTransformation {
        val text = asCharSequence()
        if (!text.all(Char::isDigit) || text.startsWith('0')) revertAllChanges()
    }
)

/** Tampilan "1500000" → "1.500.000"; isi aslinya tetap angka saja dan kursor terpetakan otomatis. */
val ThousandsSeparator = OutputTransformation {
    var i = length - 3
    while (i > 0) {
        insert(i, ".")
        i -= 3
    }
}

/** Kolom nominal rupiah ringkas (dipakai di sheet): label di atas, "Rp" + angka besar dengan pemisah ribuan. */
@Composable
fun AmountField(state: TextFieldState, label: String, modifier: Modifier = Modifier) {
    ItungSurface(modifier.fillMaxWidth(), contentPadding = PaddingValues(horizontal = 17.dp, vertical = 13.dp), role = SurfaceRole.FIELD) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(label, style = appText(13f, 18f, Medium, Ink.Gray))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(RupiahPrefix, style = appText(18f, 26f, SemiBold, Ink.Gray))
                val style = appText(24f, 32f, Bold)
                BasicTextField(
                    state = state,
                    modifier = Modifier.fillMaxWidth().semantics { contentDescription = label },
                    inputTransformation = AmountInput,
                    outputTransformation = ThousandsSeparator,
                    textStyle = style,
                    cursorBrush = SolidColor(Ink.White),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    lineLimits = TextFieldLineLimits.SingleLine,
                    decorator = TextFieldDecorator { inner ->
                        Box(contentAlignment = Alignment.CenterStart) {
                            if (state.text.isEmpty()) Text("0", style = style.copy(color = Ink.white(0.3f)))
                            inner()
                        }
                    },
                )
            }
        }
    }
}

/** Kolom teks satu baris bergaya kaca. [password] menyamarkan isinya. */
@Composable
fun ItungTextField(
    state: TextFieldState,
    placeholder: String,
    modifier: Modifier = Modifier,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    password: Boolean = false,
    maxLength: Int? = null,
) {
    val style = appText(16f, 24f, Regular, Ink.White)
    val decorator = TextFieldDecorator { inner ->
        Box(contentAlignment = Alignment.CenterStart) {
            if (state.text.isEmpty()) Text(placeholder, style = appText(16f, 24f, Regular, Ink.Muted.copy(alpha = 0.5f)))
            inner()
        }
    }
    ItungSurface(modifier.fillMaxWidth(), contentPadding = PaddingValues(17.dp), role = SurfaceRole.FIELD) {
        val field = Modifier.fillMaxWidth().semantics { contentDescription = placeholder }
        if (password) {
            BasicSecureTextField(
                state = state,
                modifier = field,
                textStyle = style,
                cursorBrush = SolidColor(Ink.White),
                keyboardOptions = keyboardOptions,
                decorator = decorator,
            )
        } else {
            BasicTextField(
                state = state,
                modifier = field,
                inputTransformation = maxLength?.let { InputTransformation.maxLength(it) },
                textStyle = style,
                cursorBrush = SolidColor(Ink.White),
                keyboardOptions = keyboardOptions,
                lineLimits = TextFieldLineLimits.SingleLine,
                decorator = decorator,
            )
        }
    }
}
