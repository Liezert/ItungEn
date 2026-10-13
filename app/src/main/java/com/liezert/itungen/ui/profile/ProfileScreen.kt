package com.liezert.itungen.ui.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.liezert.glass.components.GlassCard
import com.liezert.glass.components.GlassSurface
import com.liezert.glass.components.LiquidButton
import com.liezert.itungen.R
import com.liezert.itungen.data.sync.SyncStatus
import com.liezert.itungen.domain.model.formatRupiahShort
import com.liezert.itungen.ui.account.AccountUiState
import com.liezert.itungen.ui.account.AccountViewModel
import com.liezert.itungen.ui.account.GoogleSignInButton
import com.liezert.itungen.ui.components.GlassIconButton
import com.liezert.itungen.ui.components.IconCircle
import com.liezert.itungen.ui.components.LiquidAlertDialog
import com.liezert.itungen.ui.components.TopBar
import com.liezert.itungen.ui.components.bottomClearance
import com.liezert.itungen.ui.components.screenPad
import com.liezert.itungen.ui.theme.Bold
import com.liezert.itungen.ui.theme.Ink
import com.liezert.itungen.ui.theme.Medium
import com.liezert.itungen.ui.theme.Regular
import com.liezert.itungen.ui.theme.SemiBold
import com.liezert.itungen.ui.theme.appText
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val JoinedFormat = DateTimeFormatter.ofPattern("MMM yyyy", Locale.forLanguageTag("id-ID"))

@Composable
fun ProfileScreen(
    onHistory: () -> Unit,
    onEmailSignIn: () -> Unit,
    viewModel: ProfileViewModel = viewModel(factory = ProfileViewModel.Factory),
    accountViewModel: AccountViewModel = viewModel(factory = AccountViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val account by accountViewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var confirmSignOut by rememberSaveable { mutableStateOf(false) }

    ProfileContent(
        state = state,
        account = account,
        onHistory = onHistory,
        onSignIn = { accountViewModel.signIn(context) },
        onEmailSignIn = onEmailSignIn,
        onSync = accountViewModel::syncNow,
        onSignOut = { confirmSignOut = true },
    )

    LiquidAlertDialog(
        visible = confirmSignOut,
        title = "Keluar dari akun?",
        text = "Catatan di HP ini akan dihapus. Semuanya tetap aman di akunmu dan kembali saat kamu masuk lagi.",
        confirmLabel = "Keluar",
        onConfirm = {
            confirmSignOut = false
            accountViewModel.signOut()
        },
        onDismiss = { confirmSignOut = false },
        confirmColor = Ink.Red,
    )
}

@Composable
fun ProfileContent(
    state: ProfileUiState,
    account: AccountUiState,
    onHistory: () -> Unit,
    onSignIn: () -> Unit,
    onEmailSignIn: () -> Unit,
    onSync: () -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Kartu menu (Notifikasi, Ekspor/Impor, Ubah Profil, Kategori Kustom) muncul saat fiturnya dibuat (DESIGN_SPEC §6.6).
    val user = account.user
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        TopBar(
            leading = { Spacer(Modifier.size(48.dp)) }, // hamburger ditunda sampai punya isi (DESIGN_SPEC §11.2)
            trailing = { GlassIconButton(R.drawable.ic_history, onHistory, "Semua transaksi") },
        )
        Column(Modifier.screenPad(), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            GlassSurface(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(21.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        IconCircle(R.drawable.ic_profile_avatar, 56.dp, Ink.AvatarBg, border = Ink.white(0.1f))
                        Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                            Text(user?.name ?: if (user != null) "Pengguna ItungEn" else "Tamu", style = appText(16f, 20f, SemiBold))
                            Text(user?.email ?: "Data tersimpan di perangkat ini", style = appText(13f, 19.5f, Regular, Ink.Gray))
                        }
                    }
                    // Kolom berbobot, bukan lebar tetap 94,66 dp dari Figma: layar 360 dp lebih sempit dari acuan 390 dp.
                    Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(8.dp)) {
                        Stat("TOTAL SALDO", formatRupiahShort(state.balance), bordered = false, Modifier.weight(1f))
                        Stat("TARGET AKTIF", state.activeGoals.toString(), bordered = true, Modifier.weight(1.15f))
                        Stat("BERGABUNG", state.joined?.format(JoinedFormat) ?: "-", bordered = true, Modifier.weight(1f))
                    }
                }
            }
            AccountCard(account, onSignIn, onEmailSignIn, onSync, onSignOut)
        }
        Spacer(Modifier.height(bottomClearance()))
    }
}

private val ClockFormat = DateTimeFormatter.ofPattern("HH.mm")

private fun AccountUiState.syncLabel(): String = when {
    user == null -> "Catatan hanya tersimpan di HP ini. Masuk agar tersimpan di akun dan tersinkron di semua HP."
    sync == SyncStatus.Syncing -> "Menyinkronkan…"
    sync == SyncStatus.Failed -> "Gagal sinkron, dicoba lagi otomatis" + if (pending > 0) " · $pending perubahan menunggu" else ""
    pending > 0 -> "$pending perubahan menunggu diunggah"
    sync is SyncStatus.Synced -> "Tersinkron · " + sync.at.atZone(ZoneId.systemDefault()).format(ClockFormat)
    else -> "Tersinkron otomatis setiap ada perubahan"
}

/** Masuk dengan Google / email (tamu) atau status sinkron + Sinkronkan / Keluar (sudah masuk). */
@Composable
private fun AccountCard(
    account: AccountUiState,
    onSignIn: () -> Unit,
    onEmailSignIn: () -> Unit,
    onSync: () -> Unit,
    onSignOut: () -> Unit,
) {
    GlassCard(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
            IconCircle(R.drawable.ic_cloud_sync, 40.dp, Ink.Cyan.copy(alpha = 0.15f))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(if (account.user == null) "Simpan ke akun" else "Sinkronisasi", style = appText(16f, 22f, SemiBold))
                Text(
                    account.syncLabel(),
                    style = appText(13f, 18f, Regular, if (account.sync == SyncStatus.Failed) Ink.Orange else Ink.Gray),
                )
            }
        }
        if (account.user == null) {
            GoogleSignInButton(busy = account.busy, onClick = onSignIn)
            LiquidButton(onClick = onEmailSignIn, modifier = Modifier.fillMaxWidth(), enabled = !account.busy) {
                Text("Masuk dengan email", style = appText(15f, 20f, SemiBold))
            }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                LiquidButton(
                    onClick = onSync,
                    modifier = Modifier.weight(1f),
                    enabled = !account.busy && account.sync != SyncStatus.Syncing,
                ) { Text("Sinkronkan", style = appText(15f, 20f, SemiBold)) }
                LiquidButton(onClick = onSignOut, modifier = Modifier.weight(1f), enabled = !account.busy) {
                    Text("Keluar", style = appText(15f, 20f, SemiBold, Ink.Red))
                }
            }
        }
        account.message?.let { Text(it, style = appText(13f, 18f, Medium, Ink.Red)) }
    }
}

@Composable
private fun Stat(label: String, value: String, bordered: Boolean, modifier: Modifier = Modifier) {
    Column(
        modifier.then(
            if (bordered) Modifier.drawBehind {
                drawLine(Ink.white(0.1f), Offset(0.5.dp.toPx(), 0f), Offset(0.5.dp.toPx(), size.height), strokeWidth = 1.dp.toPx())
            }.padding(start = 10.dp) else Modifier
        ),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(label, style = appText(10f, 16f, Medium, Ink.Gray, tracking = 0.1f), maxLines = 1)
        Text(value, style = appText(14f, 21f, Bold), maxLines = 1)
    }
}
