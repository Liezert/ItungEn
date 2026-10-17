package com.liezert.itungen.ui.profile

import com.liezert.itungen.ui.components.formatIn
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.text.BasicText
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.liezert.itungen.R
import com.liezert.itungen.ui.components.HiddenAmount
import com.liezert.itungen.ItungEnApp
import androidx.compose.ui.platform.LocalContext
import com.liezert.itungen.domain.model.formatRupiah
import com.liezert.itungen.ui.account.AccountUiState
import com.liezert.itungen.ui.account.AccountViewModel
import com.liezert.itungen.ui.components.Ico
import com.liezert.itungen.ui.components.IconCircle
import com.liezert.itungen.ui.components.TopBar
import com.liezert.itungen.ui.components.bottomClearance
import com.liezert.itungen.ui.components.screenPad
import com.liezert.itungen.ui.theme.Bold
import com.liezert.itungen.ui.theme.Ink
import com.liezert.itungen.ui.theme.Medium
import com.liezert.itungen.ui.theme.Regular
import com.liezert.itungen.ui.theme.SemiBold
import com.liezert.itungen.ui.theme.appText
import java.time.format.DateTimeFormatter
import java.util.Locale
import androidx.compose.ui.text.style.TextOverflow
import com.liezert.itungen.ui.design.ItungSurface


@Composable
fun ProfileScreen(
    onOpenSettings: () -> Unit,
    viewModel: ProfileViewModel = viewModel(factory = ProfileViewModel.Factory),
    accountViewModel: AccountViewModel = viewModel(factory = AccountViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val account by accountViewModel.uiState.collectAsStateWithLifecycle()
    ProfileContent(state, account, onOpenSettings)
}

@Composable
fun ProfileContent(
    state: ProfileUiState,
    account: AccountUiState,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val user = account.user
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        TopBar(
            leading = { Spacer(Modifier.size(48.dp)) },
            trailing = { Spacer(Modifier.size(48.dp)) },
        )
        Column(Modifier.screenPad(), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            ItungSurface(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(21.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        IconCircle(R.drawable.ic_profile_avatar, 56.dp, Ink.AvatarBg, border = Ink.white(0.1f))
                        Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                            Text(user?.name ?: stringResource(if (user != null) R.string.itungen_user else R.string.guest), style = appText(16f, 20f, SemiBold))
                            Text(
                                user?.email ?: stringResource(R.string.data_on_device),
                                style = appText(13f, 19.5f, Regular, Ink.Gray),
                            )
                        }
                    }
                    // Kolom berbobot, bukan lebar tetap 94,66 dp dari Figma: layar 360 dp lebih sempit dari acuan 390 dp.
                    Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(8.dp)) {
                        val hidden = (LocalContext.current.applicationContext as ItungEnApp).container.hideBalance
                        // Dompet utama saja (dana di target tampil di Goals), sama dengan Total Saldo di Beranda.
                        Stat(stringResource(R.string.stat_main_balance), if (hidden) HiddenAmount else formatRupiah(state.balance), bordered = false, Modifier.weight(1f))
                        Stat(stringResource(R.string.stat_active_goals), state.activeGoals.toString(), bordered = true, Modifier.weight(1.15f))
                        Stat(stringResource(R.string.stat_joined), state.joined?.formatIn("MMM yyyy") ?: "-", bordered = true, Modifier.weight(1f))
                    }
                }
            }
            // Akun, sinkron (otomatis), dan tema ada di Pengaturan.
            ItungSurface(Modifier.fillMaxWidth(), contentPadding = PaddingValues(16.dp), onClick = onOpenSettings) {
                Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(12.dp), Alignment.CenterVertically) {
                    IconCircle(R.drawable.ic_settings, 32.dp, Ink.white(0.1f))
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.settings), style = appText(15f, 22f, Medium))
                        Text(
                            stringResource(if (user == null) R.string.settings_hint_guest else R.string.settings_hint_user),
                            style = appText(12f, 16f, Regular, Ink.Gray),
                        )
                    }
                    Ico(R.drawable.ic_chevron_right)
                }
            }
        }
        Spacer(Modifier.height(bottomClearance()))
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
        Text(label, style = appText(10f, 16f, Medium, Ink.Gray, tracking = 0.1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        // Angka lengkap seperti di Beranda; mengecil sendiri bila kolomnya sempit.
        BasicText(
            value,
            style = appText(14f, 21f, Bold),
            maxLines = 1,
            autoSize = TextAutoSize.StepBased(minFontSize = 10.sp, maxFontSize = 14.sp),
        )
    }
}
