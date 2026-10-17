package com.liezert.itungen.ui.account

import androidx.annotation.DrawableRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.liezert.itungen.R
import com.liezert.itungen.ui.components.ItungTextField
import com.liezert.itungen.ui.components.Ico
import com.liezert.itungen.ui.components.IconCircle
import com.liezert.itungen.ui.theme.Bold
import com.liezert.itungen.ui.theme.Ink
import com.liezert.itungen.ui.theme.Medium
import com.liezert.itungen.ui.theme.Regular
import com.liezert.itungen.ui.theme.SemiBold
import com.liezert.itungen.ui.theme.appText
import com.liezert.itungen.ui.design.ItungCard
import com.liezert.itungen.ui.design.ItungButton

/**
 * Layar masuk: Google, email + kata sandi, atau lanjut sebagai tamu (bisa masuk nanti dari Profil).
 * [startWithEmail] membuka langsung formulir email (dari tombol di Profil).
 */
@Composable
fun LoginScreen(
    onDone: () -> Unit,
    startWithEmail: Boolean = false,
    viewModel: AccountViewModel = viewModel(factory = AccountViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    LaunchedEffect(state.user) { if (state.user != null) onDone() }

    var emailMode by rememberSaveable { mutableStateOf(startWithEmail) }
    var creating by rememberSaveable { mutableStateOf(false) }
    val email = rememberTextFieldState()
    val password = rememberTextFieldState()

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .padding(horizontal = 24.dp),
    ) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.Center) {
            Text("ItungEn", style = appText(44f, 52f, Bold, tracking = -1f))
            Text(
                "Catat uang masuk dan keluar,\nkejar target tabunganmu.",
                Modifier.padding(top = 8.dp),
                style = appText(17f, 24f, Regular, Ink.Muted),
            )
            if (emailMode) {
                Column(Modifier.padding(top = 32.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(if (creating) "Buat akun baru" else "Masuk dengan email", style = appText(20f, 28f, SemiBold))
                    ItungTextField(
                        email,
                        "Email",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                    )
                    ItungTextField(
                        password,
                        "Kata sandi",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                        password = true,
                    )
                }
            } else {
                ItungCard(Modifier.fillMaxWidth().padding(top = 32.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                    Feature(R.drawable.ic_wallet, "Catat cepat", "Keypad kalkulator, langsung tersimpan.")
                    Feature(R.drawable.ic_target, "Target tabungan", "Pantau progres setiap goal.")
                    Feature(R.drawable.ic_cloud_sync, "Aman di akunmu", "Masuk agar catatan tersimpan dan tersinkron di semua HP.")
                }
            }
        }
        Spacer(Modifier.height(16.dp))

        state.message?.let {
            Text(it, Modifier.fillMaxWidth().padding(bottom = 12.dp), style = appText(14f, 20f, Medium, Ink.Red), textAlign = TextAlign.Center)
        }
        if (emailMode) {
            ItungButton(
                onClick = { viewModel.signInWithEmail(email.text.toString(), password.text.toString(), creating) },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                enabled = !state.busy,
            ) { Text(if (state.busy) "Menghubungkan…" else if (creating) "Daftar" else "Masuk", style = appText(16f, 22f, SemiBold)) }
            LinkText(if (creating) "Sudah punya akun? Masuk" else "Belum punya akun? Daftar") {
                creating = !creating
                viewModel.clearMessage()
            }
            LinkText("Kembali") {
                emailMode = false
                creating = false
                viewModel.clearMessage()
            }
        } else {
            GoogleSignInButton(busy = state.busy) { viewModel.signIn(context) }
            Spacer(Modifier.height(12.dp))
            ItungButton(
                onClick = { emailMode = true },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                enabled = !state.busy,
            ) { Text("Masuk dengan email", style = appText(16f, 22f, SemiBold)) }
            Spacer(Modifier.height(12.dp))
            ItungButton(
                onClick = {
                    viewModel.continueAsGuest()
                    onDone()
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                enabled = !state.busy,
            ) { Text("Lanjutkan sebagai tamu", style = appText(16f, 22f, SemiBold)) }
            Text(
                "Sebagai tamu, catatan hanya tersimpan di HP ini. Kamu bisa masuk kapan saja dari Profil.",
                Modifier.fillMaxWidth().padding(top = 12.dp),
                style = appText(13f, 18f, Regular, Ink.Gray),
                textAlign = TextAlign.Center,
            )
            LinkText("Coba dengan data contoh") {
                viewModel.continueWithDemo()
                onDone()
            }
        }
    }
}

@Composable
private fun LinkText(text: String, onClick: () -> Unit) {
    Text(
        text,
        Modifier.fillMaxWidth().clickable(role = Role.Button, onClick = onClick).padding(vertical = 14.dp),
        style = appText(15f, 20f, SemiBold, Ink.Muted),
        textAlign = TextAlign.Center,
    )
}

/** Tombol kaca berpermukaan putih + logo G, gaya tombol "Sign in with Google" versi terang. */
@Composable
fun GoogleSignInButton(busy: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    ItungButton(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().height(56.dp),
        surfaceColor = Color.White,
        enabled = !busy,
    ) {
        if (!busy) Ico(R.drawable.ic_google)
        // Teks gelap tetap di atas tombol putih (Ink.Black adalah warna latar: di tema terang ikut terang dan hilang).
        Text(if (busy) "Menghubungkan…" else "Masuk dengan Google", style = appText(16f, 22f, SemiBold, Color.Black))
    }
}

@Composable
private fun Feature(@DrawableRes icon: Int, title: String, body: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
        IconCircle(icon, 40.dp, Ink.white(0.08f))
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = appText(15f, 20f, SemiBold))
            Text(body, style = appText(13f, 18f, Regular, Ink.Gray))
        }
    }
}
