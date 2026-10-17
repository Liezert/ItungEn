package com.liezert.itungen.ui.settings

import com.liezert.itungen.ui.components.displayName
import com.liezert.itungen.domain.model.TransactionType
import com.liezert.itungen.data.backup.CsvLabels
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.Image
import com.liezert.itungen.ui.components.formatIn
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import com.liezert.itungen.work.QuickIncome
import com.liezert.itungen.ui.design.ActiveTheme
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.liezert.itungen.ItungEnApp
import com.liezert.itungen.R
import com.liezert.itungen.data.backup.BackupTooLargeException
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
import com.liezert.itungen.ui.components.ChoiceSheet
import com.liezert.itungen.ui.components.ItungIconButton
import com.liezert.itungen.ui.components.Ico
import com.liezert.itungen.ui.components.IconCircle
import com.liezert.itungen.ui.components.ItungAlertDialog
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
import androidx.compose.ui.text.style.TextOverflow
import com.liezert.itungen.ui.design.ItungSwitch
import com.liezert.itungen.ui.design.ItungSurface
import com.liezert.itungen.ui.design.ItungCard
import com.liezert.itungen.ui.design.ItungButton

private val ClockFormat = DateTimeFormatter.ofPattern("HH.mm")

/** Sore sampai malam: waktu yang masuk akal untuk "sudah catat hari ini?". */
private val ReminderHours = (16..23).toList()

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
    // Teks untuk pesan di callback: LocalResources ikut berganti saat bahasa app berganti (lint LocalContextGetResourceValueCall).
    val resources = LocalResources.current
    val container = (context.applicationContext as ItungEnApp).container
    val plan by container.budgetPlan.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var confirmSignOut by rememberSaveable { mutableStateOf(false) }
    var reminder by remember { mutableStateOf(container.reminderEnabled) }
    var reminderHour by remember { mutableIntStateOf(container.reminderHour) }
    var pickingHour by rememberSaveable { mutableStateOf(false) }
    var appLock by remember { mutableStateOf(container.appLock) }
    var dataMessage by remember { mutableStateOf<String?>(null) }
    var dataBusy by remember { mutableStateOf(false) }
    var language by remember { mutableStateOf(AppLanguages.current(context)) }
    var lockMessage by remember { mutableStateOf<String?>(null) }
    var reminderMessage by remember { mutableStateOf<String?>(null) }
    var quickIncome by remember { mutableStateOf(container.quickIncomeEnabled) }
    var quickIncomeMessage by remember { mutableStateOf<String?>(null) }
    val hasTransactions by remember { container.transactionRepository.observeHasTransactions() }.collectAsStateWithLifecycle(true)
    // Izin bisa dicabut dari pengaturan HP kapan saja: dibaca ulang setiap layar kembali tampil.
    var notificationsAllowed by remember { mutableStateOf(true) }
    LifecycleResumeEffect(Unit) {
        notificationsAllowed = NotificationManagerCompat.from(context).areNotificationsEnabled()
        onPauseOrDispose { }
    }

    fun setReminder(on: Boolean) {
        reminder = on
        container.reminderEnabled = on
        if (on) ReminderWorker.schedule(context, reminderHour) else ReminderWorker.cancel(context)
    }
    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        setReminder(granted)
        notificationsAllowed = NotificationManagerCompat.from(context).areNotificationsEnabled()
        // Penolakan dihormati: tidak langsung melempar ke pengaturan sistem (lihat notificationAsk).
        reminderMessage = if (granted) null else resources.getString(R.string.reminder_denied)
    }
    /** Minta izin notifikasi lewat [launcher]; setelah ditolak permanen, buka pengaturan notifikasi app. */
    fun askNotifications(launcher: ManagedActivityResultLauncher<String, Boolean>) {
        val rationale = (context as? Activity)
            ?.let { ActivityCompat.shouldShowRequestPermissionRationale(it, Manifest.permission.POST_NOTIFICATIONS) } == true
        when (notificationAsk(container.notificationAsked, rationale)) {
            NotificationAsk.REQUEST -> {
                container.notificationAsked = true
                launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
            // Dialog tidak akan muncul lagi: satu-satunya jalan adalah pengaturan notifikasi app.
            NotificationAsk.OPEN_SETTINGS -> openNotificationSettings(context)
        }
    }
    fun notificationGranted() =
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    fun toggleReminder(on: Boolean) {
        val granted = notificationGranted()
        reminderMessage = null
        when {
            on && !granted -> askNotifications(notificationPermission)
            on && !notificationsAllowed -> {
                setReminder(true)
                openNotificationSettings(context) // izin ada, tetapi notifikasi app dimatikan
            }
            else -> setReminder(on)
        }
    }
    fun setQuickIncome(on: Boolean) {
        quickIncome = on
        container.quickIncomeEnabled = on
        if (on) scope.launch { QuickIncome.show(context.applicationContext) } else QuickIncome.cancel(context)
    }
    val quickIncomePermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        setQuickIncome(granted)
        notificationsAllowed = NotificationManagerCompat.from(context).areNotificationsEnabled()
        quickIncomeMessage = if (granted) null else resources.getString(R.string.quick_income_denied)
    }
    fun toggleQuickIncome(on: Boolean) {
        quickIncomeMessage = null
        when {
            on && !notificationGranted() -> askNotifications(quickIncomePermission)
            on && !notificationsAllowed -> {
                setQuickIncome(true)
                openNotificationSettings(context) // izin ada, tetapi notifikasi app dimatikan
            }
            else -> setQuickIncome(on)
        }
    }
    fun toggleAppLock(on: Boolean) {
        if (on && !AppLock.available(context)) {
            lockMessage = resources.getString(R.string.lock_unavailable)
            return
        }
        // Mematikan juga perlu verifikasi, agar orang lain yang memegang HP tidak bisa membuka kunci dari sini.
        AppLock.authenticate(context, resources.getString(if (on) R.string.lock_enable_prompt else R.string.lock_disable_prompt)) { ok ->
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

    // Cadangan: berkas dipilih pengguna lewat pemilih dokumen sistem (tanpa izin penyimpanan). [busy] tampil selama
    // berjalan dan tombol data dinonaktifkan, agar ekspor/impor (bisa beberapa detik bila banyak gambar) tidak dobel.
    fun runData(busy: String, action: suspend () -> String) {
        if (dataBusy) return
        dataBusy = true
        dataMessage = busy
        scope.launch {
            dataMessage = try {
                withContext(Dispatchers.IO) { action() }
            } catch (_: InvalidBackupException) {
                resources.getString(R.string.backup_invalid)
            } catch (_: BackupTooLargeException) {
                resources.getString(R.string.backup_too_large)
            } catch (_: Exception) {
                resources.getString(R.string.backup_io_failed)
            } finally {
                dataBusy = false
            }
        }
    }
    val exportBackup = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
        if (uri != null) runData(resources.getString(R.string.backup_exporting)) {
            context.contentResolver.openOutputStream(uri)!!.use { container.backup.export(it) }
            resources.getString(R.string.backup_exported)
        }
    }
    val exportCsv = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        if (uri != null) runData(resources.getString(R.string.csv_exporting)) {
            val transactions = container.transactionRepository.allTransactions()
            val labels = CsvLabels(
                header = listOf(R.string.csv_date, R.string.csv_type, R.string.csv_category, R.string.csv_note, R.string.csv_amount).map(resources::getString),
                type = { type ->
                    resources.getString(
                        when (type) {
                            TransactionType.INCOME -> R.string.type_income
                            TransactionType.EXPENSE -> R.string.type_expense
                            TransactionType.TRANSFER_OUT -> R.string.csv_to_goal
                            TransactionType.TRANSFER_IN -> R.string.csv_from_goal
                        }
                    )
                },
                category = { tx -> tx.category?.displayName(context).orEmpty() },
            )
            context.contentResolver.openOutputStream(uri)!!.use { container.backup.exportCsv(transactions, it, labels) }
            resources.getQuantityString(R.plurals.csv_exported, transactions.size, transactions.size)
        }
    }
    val importBackup = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) runData(resources.getString(R.string.backup_importing)) {
            val result = context.contentResolver.openInputStream(uri)!!.use { container.backup.import(it) }
            val res = resources
            listOfNotNull(
                if (result.added == 0) res.getString(R.string.backup_nothing_new) else res.getQuantityString(R.plurals.backup_restored_rows, result.added, result.added),
                result.images.takeIf { it > 0 }?.let { res.getQuantityString(R.plurals.backup_restored_images, it, it) },
                res.getString(R.string.backup_restored_plan).takeIf { result.planRestored },
            ).joinToString(" ")
        }
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        TopBar(
            leading = { ItungIconButton(R.drawable.ic_back, onBack, stringResource(R.string.action_back)) },
            trailing = { Spacer(Modifier.size(48.dp)) },
        )
        Column(Modifier.screenPad(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.settings), Modifier.padding(bottom = 8.dp), style = appText(20f, 28f, Bold, tracking = -0.5f))

            SectionLabel(stringResource(R.string.section_account))
            AccountCard(
                account,
                onSignIn = { viewModel.signIn(context) },
                onEmailSignIn = onEmailSignIn,
                onRetry = viewModel::retrySync,
            )

            SectionLabel(stringResource(R.string.section_budget))
            val currentPlan = plan
            NavRow(
                R.drawable.ic_wallet,
                stringResource(R.string.budget_plan),
                if (currentPlan == null) stringResource(R.string.budget_plan_unset) else currentPlan.summary(),
                onOpenBudget,
            )

            SectionLabel(stringResource(R.string.section_reminders))
            val reminderBlocked = reminder && !notificationsAllowed
            ToggleRow(
                stringResource(R.string.daily_reminder),
                stringResource(if (reminderBlocked) R.string.daily_reminder_blocked else R.string.daily_reminder_body),
                checked = reminder && notificationsAllowed,
                onChange = ::toggleReminder,
                subtitleColor = if (reminderBlocked) Ink.Orange else Ink.Gray,
            )
            reminderMessage?.let { Text(it, Modifier.padding(horizontal = 4.dp), style = appText(12f, 16f, Regular, Ink.Orange)) }
            ToggleRow(
                stringResource(R.string.quick_income_setting),
                stringResource(R.string.quick_income_setting_body),
                checked = quickIncome && notificationsAllowed,
                onChange = ::toggleQuickIncome,
            )
            quickIncomeMessage?.let { Text(it, Modifier.padding(horizontal = 4.dp), style = appText(12f, 16f, Regular, Ink.Orange)) }
            if (reminder && notificationsAllowed) {
                NavRow(R.drawable.ic_history, stringResource(R.string.reminder_time), stringResource(R.string.reminder_time_daily, reminderHour)) { pickingHour = true }
                Text(
                    stringResource(R.string.reminder_battery_hint),
                    Modifier.padding(horizontal = 4.dp),
                    style = appText(12f, 16f, Regular, Ink.Gray),
                )
            }

            SectionLabel(stringResource(R.string.section_privacy))
            ToggleRow(
                stringResource(R.string.app_lock),
                stringResource(R.string.app_lock_body),
                checked = appLock,
                onChange = ::toggleAppLock,
            )
            lockMessage?.let { Text(it, Modifier.padding(horizontal = 4.dp), style = appText(12f, 16f, Regular, Ink.Orange)) }
            ToggleRow(
                stringResource(R.string.balance_hide),
                stringResource(R.string.balance_hide_body),
                checked = container.hideBalance,
                onChange = container::setBalanceHidden,
            )

            SectionLabel(stringResource(R.string.section_display))
            ThemePicker(ActiveTheme.spec, onSelect = container::setTheme)
            ToggleRow(stringResource(R.string.dark_mode), stringResource(R.string.dark_mode_body), checked = Ink.dark, onChange = ::setDark)
            // Mode hemat hanya mengatur efek kaca; tema lain tidak punya efek yang bisa dihemat.
            if (ActiveTheme.spec.capturesScreen) ToggleRow(
                stringResource(R.string.economy_mode),
                stringResource(if (container.powerSaveActive && !container.economyMode) R.string.economy_mode_auto else R.string.economy_mode_body),
                checked = container.glassEconomy,
                onChange = container::setEconomy,
            )

            SectionLabel(stringResource(R.string.section_language))
            LanguagePicker(language, onSelect = {
                language = it
                AppLanguages.set(context, it)
            })

            SectionLabel(stringResource(R.string.section_data))
            ItungCard(Modifier.fillMaxWidth(), padding = 17.dp, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    stringResource(R.string.data_body),
                    style = appText(13f, 18f, Regular, Ink.Gray),
                )
                val today = LocalDate.now()
                ItungButton(onClick = { exportBackup.launch(resources.getString(R.string.backup_file_name, today.toString())) }, modifier = Modifier.fillMaxWidth(), enabled = !dataBusy) {
                    Text(stringResource(R.string.backup_export), style = appText(15f, 20f, SemiBold))
                }
                ItungButton(onClick = { exportCsv.launch(resources.getString(R.string.csv_file_name, today.toString())) }, modifier = Modifier.fillMaxWidth(), enabled = !dataBusy) {
                    Text(stringResource(R.string.csv_export), style = appText(15f, 20f, SemiBold))
                }
                ItungButton(
                    onClick = { importBackup.launch(arrayOf("application/zip", "application/json", "application/octet-stream")) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !dataBusy,
                ) {
                    Text(stringResource(R.string.backup_import), style = appText(15f, 20f, SemiBold))
                }
                if (!hasTransactions) {
                    ItungButton(
                        onClick = { runData(resources.getString(R.string.demo_adding)) { seedDemoData(container); resources.getString(R.string.demo_added) } },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !dataBusy,
                    ) {
                        Text(stringResource(R.string.demo_add), style = appText(15f, 20f, SemiBold))
                    }
                }
                // Dibacakan TalkBack saat berubah (mulai → selesai/galat).
                dataMessage?.let { Text(it, Modifier.semantics { liveRegion = LiveRegionMode.Polite }, style = appText(13f, 18f, Medium, Ink.Muted)) }
            }

            if (account.user != null) {
                Spacer(Modifier.height(8.dp))
                ItungButton(
                    onClick = { confirmSignOut = true },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    enabled = !account.busy,
                ) { Text(stringResource(R.string.sign_out), style = appText(16f, 22f, SemiBold, Ink.Red)) }
                if (account.pending > 0) {
                    Text(
                        pluralStringResource(R.plurals.sign_out_pending, account.pending, account.pending),
                        Modifier.padding(horizontal = 4.dp),
                        style = appText(12f, 16f, Regular, Ink.Orange),
                    )
                }
            }
            account.message?.let { Text(stringResource(it), style = appText(13f, 18f, Medium, Ink.Red)) }
        }
        Spacer(Modifier.height(bottomClearance()))
    }

    ChoiceSheet(stringResource(R.string.reminder_time), ReminderHours.map { it to "%02d.00".format(it) }, 4, reminderHour, pickingHour, { hour ->
        pickingHour = false
        reminderHour = hour
        container.reminderHour = hour
        ReminderWorker.schedule(context, hour)
    }) { pickingHour = false }

    ItungAlertDialog(
        visible = confirmSignOut,
        title = stringResource(R.string.sign_out_confirm_title),
        text = if (account.pending > 0) pluralStringResource(R.plurals.sign_out_confirm_pending, account.pending, account.pending)
        else stringResource(R.string.sign_out_confirm_body),
        confirmLabel = stringResource(R.string.sign_out_action),
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
    ItungSurface(Modifier.fillMaxWidth(), contentPadding = PaddingValues(16.dp), onClick = onClick) {
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

@Composable
private fun BudgetPlan.summary(): String = when (period) {
    BudgetPeriod.MONTHLY -> stringResource(R.string.plan_summary_monthly, formatRupiah(income), cycleStartDay)
    BudgetPeriod.WEEKLY -> stringResource(R.string.plan_summary_weekly, formatRupiah(income), dayName(cycleStartDay))
    BudgetPeriod.UNTIL_DATE -> stringResource(R.string.plan_summary_until, formatRupiah(income), nextIncomeOn?.formatIn("d MMMM") ?: stringResource(R.string.next_transfer))
}

/** Ikuti sistem / Bahasa Indonesia / English; nama bahasa ditulis dalam bahasanya sendiri (tidak diterjemahkan). */
@Composable
private fun LanguagePicker(selected: AppLanguage, onSelect: (AppLanguage) -> Unit) {
    ItungCard(Modifier.fillMaxWidth().selectableGroup(), padding = 6.dp) {
        AppLanguage.entries.forEach { language ->
            val on = language == selected
            Row(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .selectable(on, role = Role.RadioButton) { onSelect(language) }
                    .padding(horizontal = 11.dp, vertical = 12.dp),
                Arrangement.spacedBy(12.dp),
                Alignment.CenterVertically,
            ) {
                Text(
                    stringResource(
                        when (language) {
                            AppLanguage.SYSTEM -> R.string.language_system
                            AppLanguage.INDONESIAN -> R.string.language_indonesian
                            AppLanguage.ENGLISH -> R.string.language_english
                        }
                    ),
                    Modifier.weight(1f),
                    style = appText(16f, 22f, if (on) SemiBold else Regular),
                )
                if (on) Image(painterResource(R.drawable.ic_check), null, Modifier.size(20.dp), colorFilter = ColorFilter.tint(Ink.Blue))
            }
        }
    }
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
    ItungCard(Modifier.fillMaxWidth(), padding = 17.dp) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(Modifier.weight(1f).toggleable(value = checked, role = Role.Switch, onValueChange = onChange)) {
                Text(title, style = appText(16f, 22f, SemiBold))
                Text(subtitle, style = appText(13f, 18f, Regular, subtitleColor))
            }
            ItungSwitch(checked, onChange, modifier = Modifier.clearAndSetSemantics {})
        }
    }
}

@Composable
private fun AccountUiState.syncLabel(): String {
    val s = sync
    return when {
        user == null -> stringResource(R.string.sync_local_only)
        s == SyncStatus.Syncing -> stringResource(R.string.sync_syncing)
        s is SyncStatus.Failed -> listOfNotNull(
            stringResource(R.string.sync_failed, stringResource(s.reason, s.code.orEmpty())),
            pending.takeIf { it > 0 }?.let { pluralStringResource(R.plurals.sync_pending_short, it, it) },
        ).joinToString(" ")
        pending > 0 -> pluralStringResource(R.plurals.sync_pending, pending, pending)
        s is SyncStatus.Synced -> stringResource(R.string.sync_synced_at, s.at.atZone(ZoneId.systemDefault()).format(ClockFormat))
        else -> stringResource(R.string.sync_auto)
    }
}

@Composable
private fun AccountCard(account: AccountUiState, onSignIn: () -> Unit, onEmailSignIn: () -> Unit, onRetry: () -> Unit) {
    val user = account.user
    val failed = account.sync is SyncStatus.Failed
    ItungCard(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
            IconCircle(R.drawable.ic_cloud_sync, 40.dp, Ink.Cyan.copy(alpha = 0.15f))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    if (user == null) stringResource(R.string.save_to_account) else user.email ?: user.name ?: stringResource(R.string.itungen_account),
                    style = appText(16f, 22f, SemiBold),
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
                Text(account.syncLabel(), style = appText(13f, 18f, Regular, if (failed) Ink.Orange else Ink.Gray))
            }
        }
        if (user == null) {
            GoogleSignInButton(busy = account.busy, onClick = onSignIn)
            ItungButton(onClick = onEmailSignIn, modifier = Modifier.fillMaxWidth(), enabled = !account.busy) {
                Text(stringResource(R.string.sign_in_email), style = appText(15f, 20f, SemiBold))
            }
        } else if (failed) {
            ItungButton(onClick = onRetry, modifier = Modifier.fillMaxWidth(), enabled = account.sync != SyncStatus.Syncing && !account.busy) {
                Text(stringResource(R.string.action_retry), style = appText(15f, 20f, SemiBold, Ink.Blue))
            }
        }
    }
}
