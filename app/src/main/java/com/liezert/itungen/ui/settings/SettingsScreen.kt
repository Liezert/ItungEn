package com.liezert.itungen.ui.settings

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
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
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.liezert.glass.components.GlassCard
import com.liezert.glass.components.GlassSurface
import com.liezert.glass.components.LiquidButton
import com.liezert.glass.components.LiquidToggle
import com.liezert.itungen.ItungEnApp
import com.liezert.itungen.R
import com.liezert.itungen.data.backup.InvalidBackupException
import com.liezert.itungen.data.demo.seedDemoData
import com.liezert.itungen.data.sync.SyncStatus
import com.liezert.itungen.domain.model.formatRupiah
import com.liezert.itungen.domain.usecase.BudgetPeriod
import com.liezert.itungen.domain.usecase.BudgetPlan
import com.liezert.itungen.ui.account.AccountUiState
import com.liezert.itungen.ui.account.AccountViewModel
import com.liezert.itungen.ui.account.GoogleSignInButton
import com.liezert.itungen.ui.budget.dayName
import com.liezert.itungen.ui.components.GlassIconButton
import com.liezert.itungen.ui.components.Ico
import com.liezert.itungen.ui.components.IconCircle
import com.liezert.itungen.ui.components.LiquidAlertDialog
import com.liezert.itungen.ui.components.TopBar
import com.liezert.itungen.ui.components.bottomClearance
import com.liezert.itungen.ui.components.screenPad
import com.liezert.itungen.ui.lock.AppLock
import com.liezert.itungen.ui.theme.Bold
import com.liezert.itungen.ui.theme.Ink
import com.liezert.itungen.ui.theme.Medium
import com.liezert.itungen.ui.theme.Regular
import com.liezert.itungen.ui.theme.SemiBold
import com.liezert.itungen.ui.theme.appText
import com.liezert.itungen.work.ReminderWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val ClockFormat = DateTimeFormatter.ofPattern("HH.mm")

/** Pengaturan: akun & sinkron (otomatis), anggaran, pengingat, tampilan, data (cadangan), keluar. */
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onEmailSignIn: () -> Unit,
    onOpenBudget: () -> Unit,
    viewModel: AccountViewModel = viewModel(factory = AccountViewModel.Factory),
) {
    val account by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val container = (context.applicationContext as ItungEnApp).container
    val plan by container.budgetPlan.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var confirmSignOut by rememberSaveable { mutableStateOf(false) }
    var reminder by remember { mutableStateOf(container.reminderEnabled) }
    var appLock by remember { mutableStateOf(container.appLock) }
    var dataMessage by remember { mutableStateOf<String?>(null) }
    var lockMessage by remember { mutableStateOf<String?>(null) }
    val transactions by remember { container.transactionRepository.observeTransactions() }.collectAsStateWithLifecycle(null)
    // Izin bisa dicabut dari pengaturan HP kapan saja: dibaca ulang setiap layar kembali tampil.
    var notificationsAllowed by remember { mutableStateOf(true) }
    LifecycleResumeEffect(Unit) {
        notificationsAllowed = NotificationManagerCompat.from(context).areNotificationsEnabled()
        onPauseOrDispose { }
    }

    fun setReminder(on: Boolean) {
        reminder = on
        container.reminderEnabled = on
        if (on) ReminderWorker.schedule(context) else ReminderWorker.cancel(context)
    }
    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        setReminder(granted)
        notificationsAllowed = NotificationManagerCompat.from(context).areNotificationsEnabled()
        // Ditolak permanen: dialog izin tidak muncul lagi, jadi buka pengaturan notifikasi app.
        if (!granted) openNotificationSettings(context)
    }
    fun toggleReminder(on: Boolean) {
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        when {
            on && !granted -> notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            on && !notificationsAllowed -> {
                setReminder(true)
                openNotificationSettings(context) // izin ada, tetapi notifikasi app dimatikan
            }
            else -> setReminder(on)
        }
    }
    fun toggleAppLock(on: Boolean) {
        if (on && !AppLock.available(context)) {
            lockMessage = "Atur kunci layar HP (sidik jari atau PIN) dulu agar ItungEn bisa dikunci."
            return
        }
        // Mematikan juga perlu verifikasi, agar orang lain yang memegang HP tidak bisa membuka kunci dari sini.
        AppLock.authenticate(context, if (on) "Aktifkan kunci ItungEn" else "Matikan kunci ItungEn") { ok ->
            if (!ok) return@authenticate
            appLock = on
            container.appLock = on
            (context as? Activity)?.setRecentsScreenshotEnabled(!on)
        }
    }
    fun setDark(dark: Boolean) {
        Ink.dark = dark
        container.darkTheme = dark
    }

    // Cadangan: berkas dipilih pengguna lewat pemilih dokumen sistem (tanpa izin penyimpanan).
    fun runData(action: suspend () -> String) {
        scope.launch {
            dataMessage = try {
                withContext(Dispatchers.IO) { action() }
            } catch (_: InvalidBackupException) {
                "Berkas ini bukan cadangan ItungEn."
            } catch (_: Exception) {
                "Gagal membaca/menulis berkas. Coba lagi."
            }
        }
    }
    val exportJson = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) runData {
            context.contentResolver.openOutputStream(uri)!!.use { container.backup.exportJson(it) }
            "Cadangan tersimpan. Simpan berkasnya di tempat aman (mis. Google Drive)."
        }
    }
    val exportCsv = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        if (uri != null) runData {
            val transactions = container.transactionRepository.observeTransactions().first()
            context.contentResolver.openOutputStream(uri)!!.use { container.backup.exportCsv(transactions, it) }
            "CSV tersimpan (${transactions.size} transaksi)."
        }
    }
    val importJson = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) runData {
            val result = context.contentResolver.openInputStream(uri)!!.use { container.backup.importJson(it) }
            val rows = if (result.added == 0) "Tidak ada catatan baru: semua isi cadangan sudah ada di HP ini." else "${result.added} catatan dipulihkan."
            if (result.planRestored) "$rows Anggaran juga dipulihkan." else rows
        }
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        TopBar(
            leading = { GlassIconButton(R.drawable.ic_back, onBack, "Kembali") },
            trailing = { Spacer(Modifier.size(48.dp)) },
        )
        Column(Modifier.screenPad(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Pengaturan", Modifier.padding(bottom = 8.dp), style = appText(20f, 28f, Bold, tracking = -0.5f))

            SectionLabel("AKUN & SINKRONISASI")
            AccountCard(
                account,
                onSignIn = { viewModel.signIn(context) },
                onEmailSignIn = onEmailSignIn,
                onRetry = viewModel::retrySync,
            )

            SectionLabel("ANGGARAN")
            val currentPlan = plan
            NavRow(
                R.drawable.ic_wallet,
                "Jatah harian",
                if (currentPlan == null) "Belum diatur: isi berapa uang masuk dan kapan" else currentPlan.summary(),
                onOpenBudget,
            )

            SectionLabel("PENGINGAT")
            val reminderBlocked = reminder && !notificationsAllowed
            ToggleRow(
                "Pengingat malam (20.00)",
                if (reminderBlocked) "Izin notifikasi dicabut, jadi pengingat tidak tampil. Aktifkan lagi untuk mengizinkan."
                else "Muncul hanya bila hari itu belum ada catatan. Bisa langsung mencatat dari notifikasinya.",
                checked = reminder && notificationsAllowed,
                onChange = ::toggleReminder,
                subtitleColor = if (reminderBlocked) Ink.Orange else Ink.Gray,
            )
            if (reminder && notificationsAllowed) {
                Text(
                    "Di beberapa HP (mis. Realme, Oppo, Xiaomi), penghemat baterai bisa menunda pengingat. " +
                        "Bila tidak muncul, izinkan ItungEn berjalan di latar belakang di pengaturan baterai.",
                    Modifier.padding(horizontal = 4.dp),
                    style = appText(12f, 16f, Regular, Ink.Gray),
                )
            }

            SectionLabel("PRIVASI")
            ToggleRow(
                "Kunci aplikasi",
                "Sidik jari, wajah, atau PIN HP saat membuka ItungEn",
                checked = appLock,
                onChange = ::toggleAppLock,
            )
            lockMessage?.let { Text(it, Modifier.padding(horizontal = 4.dp), style = appText(12f, 16f, Regular, Ink.Orange)) }
            ToggleRow(
                "Sembunyikan saldo",
                "Nominal di Beranda dan Profil diganti titik; ketuk ikon mata untuk melihat",
                checked = container.hideBalance,
                onChange = container::setBalanceHidden,
            )

            SectionLabel("TAMPILAN")
            ToggleRow("Mode gelap", "Latar hitam, nyaman di malam hari", checked = Ink.dark, onChange = ::setDark)

            SectionLabel("DATA")
            GlassCard(Modifier.fillMaxWidth(), padding = 17.dp, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Cadangan JSON bisa dipulihkan di HP mana pun; CSV untuk dibuka di spreadsheet.",
                    style = appText(13f, 18f, Regular, Ink.Gray),
                )
                val today = LocalDate.now()
                LiquidButton(onClick = { exportJson.launch("itungen-cadangan-$today.json") }, modifier = Modifier.fillMaxWidth()) {
                    Text("Ekspor cadangan (JSON)", style = appText(15f, 20f, SemiBold))
                }
                LiquidButton(onClick = { exportCsv.launch("itungen-transaksi-$today.csv") }, modifier = Modifier.fillMaxWidth()) {
                    Text("Ekspor transaksi (CSV)", style = appText(15f, 20f, SemiBold))
                }
                LiquidButton(onClick = { importJson.launch(arrayOf("application/json", "application/octet-stream")) }, modifier = Modifier.fillMaxWidth()) {
                    Text("Pulihkan dari cadangan", style = appText(15f, 20f, SemiBold))
                }
                if (transactions?.isEmpty() == true) {
                    LiquidButton(onClick = { runData { seedDemoData(container); "Data contoh ditambahkan. Lihat Beranda." } }, modifier = Modifier.fillMaxWidth()) {
                        Text("Isi data contoh", style = appText(15f, 20f, SemiBold))
                    }
                }
                dataMessage?.let { Text(it, style = appText(13f, 18f, Medium, Ink.Muted)) }
            }

            if (account.user != null) {
                Spacer(Modifier.height(8.dp))
                LiquidButton(
                    onClick = { confirmSignOut = true },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    enabled = !account.busy,
                ) { Text("Keluar dari akun", style = appText(16f, 22f, SemiBold, Ink.Red)) }
                if (account.pending > 0) {
                    Text(
                        "${account.pending} perubahan belum terunggah. Keluar baru bisa setelah semuanya terunggah, " +
                            "agar tidak ada catatan yang hilang.",
                        Modifier.padding(horizontal = 4.dp),
                        style = appText(12f, 16f, Regular, Ink.Orange),
                    )
                }
            }
            account.message?.let { Text(it, style = appText(13f, 18f, Medium, Ink.Red)) }
        }
        Spacer(Modifier.height(bottomClearance()))
    }

    LiquidAlertDialog(
        visible = confirmSignOut,
        title = "Keluar dari akun?",
        text = if (account.pending > 0) {
            "${account.pending} perubahan belum terunggah. ItungEn akan mencoba mengunggahnya dulu; " +
                "bila gagal, kamu tetap masuk agar tidak ada yang hilang."
        } else {
            "Catatan di HP ini akan dihapus. Semuanya tetap aman di akunmu dan kembali saat kamu masuk lagi."
        },
        confirmLabel = "Keluar",
        onConfirm = {
            confirmSignOut = false
            viewModel.signOut()
        },
        onDismiss = { confirmSignOut = false },
        confirmColor = Ink.Red,
    )
}

@Composable
private fun SectionLabel(text: String) {
    Text(text, Modifier.padding(start = 4.dp, top = 8.dp), style = appText(12f, 16f, Bold, Ink.Subtle, tracking = 1.2f))
}

@Composable
private fun NavRow(@DrawableRes icon: Int, title: String, subtitle: String, onClick: () -> Unit) {
    GlassSurface(Modifier.fillMaxWidth(), contentPadding = PaddingValues(16.dp), onClick = onClick) {
        Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(12.dp), Alignment.CenterVertically) {
            IconCircle(icon, 32.dp, Ink.white(0.1f))
            Column(Modifier.weight(1f)) {
                Text(title, style = appText(15f, 22f, Medium))
                Text(subtitle, style = appText(12f, 16f, Regular, Ink.Gray))
            }
            Ico(R.drawable.ic_chevron_right)
        }
    }
}

private fun openNotificationSettings(context: Context) {
    context.startActivity(
        Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS)
            .putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, context.packageName),
    )
}

private val LongDate = DateTimeFormatter.ofPattern("d MMMM", Locale.forLanguageTag("id-ID"))

private fun BudgetPlan.summary(): String = when (period) {
    BudgetPeriod.MONTHLY -> "${formatRupiah(income)}/bulan · uang masuk tanggal $cycleStartDay"
    BudgetPeriod.WEEKLY -> "${formatRupiah(income)}/minggu · setiap ${dayName(cycleStartDay)}"
    BudgetPeriod.UNTIL_DATE -> "${formatRupiah(income)} sampai ${nextIncomeOn?.format(LongDate) ?: "kiriman berikutnya"}"
}

/** Label = target sentuh + semantik switch; LiquidToggle punya gestur seret sendiri. */
@Composable
private fun ToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
    subtitleColor: Color = Ink.Gray,
) {
    GlassCard(Modifier.fillMaxWidth(), padding = 17.dp) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(Modifier.weight(1f).toggleable(value = checked, role = Role.Switch, onValueChange = onChange)) {
                Text(title, style = appText(16f, 22f, SemiBold))
                Text(subtitle, style = appText(13f, 18f, Regular, subtitleColor))
            }
            LiquidToggle({ checked }, onChange, modifier = Modifier.clearAndSetSemantics {})
        }
    }
}

private fun AccountUiState.syncLabel(): String {
    val s = sync
    return when {
        user == null -> "Catatan hanya tersimpan di HP ini. Masuk agar tersimpan di akun dan tersinkron otomatis di semua HP."
        s == SyncStatus.Syncing -> "Menyinkronkan…"
        s is SyncStatus.Failed -> "Gagal sinkron: ${s.reason}" + if (pending > 0) " $pending perubahan menunggu." else ""
        pending > 0 -> "$pending perubahan menunggu diunggah"
        s is SyncStatus.Synced -> "Tersinkron otomatis · terakhir " + s.at.atZone(ZoneId.systemDefault()).format(ClockFormat)
        else -> "Tersinkron otomatis setiap ada perubahan"
    }
}

@Composable
private fun AccountCard(account: AccountUiState, onSignIn: () -> Unit, onEmailSignIn: () -> Unit, onRetry: () -> Unit) {
    val user = account.user
    val failed = account.sync is SyncStatus.Failed
    GlassCard(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
            IconCircle(R.drawable.ic_cloud_sync, 40.dp, Ink.Cyan.copy(alpha = 0.15f))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    if (user == null) "Simpan ke akun" else user.email ?: user.name ?: "Akun ItungEn",
                    style = appText(16f, 22f, SemiBold),
                    maxLines = 1,
                )
                Text(account.syncLabel(), style = appText(13f, 18f, Regular, if (failed) Ink.Orange else Ink.Gray))
            }
        }
        if (user == null) {
            GoogleSignInButton(busy = account.busy, onClick = onSignIn)
            LiquidButton(onClick = onEmailSignIn, modifier = Modifier.fillMaxWidth(), enabled = !account.busy) {
                Text("Masuk dengan email", style = appText(15f, 20f, SemiBold))
            }
        } else if (failed) {
            LiquidButton(onClick = onRetry, modifier = Modifier.fillMaxWidth(), enabled = account.sync != SyncStatus.Syncing && !account.busy) {
                Text("Coba lagi", style = appText(15f, 20f, SemiBold, Ink.Blue))
            }
        }
    }
}
