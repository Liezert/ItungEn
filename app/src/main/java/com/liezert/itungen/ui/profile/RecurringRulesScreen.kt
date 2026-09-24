package com.liezert.itungen.ui.profile

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.liezert.glass.components.GlassCard
import com.liezert.glass.tokens.GlassColors
import com.liezert.itungen.R
import com.liezert.itungen.domain.model.RecurringRule
import com.liezert.itungen.domain.model.TransactionType
import com.liezert.itungen.domain.model.formatRupiah

@Composable
fun RecurringRulesScreen(
    onBack: () -> Unit,
    viewModel: RecurringRulesViewModel = viewModel(factory = RecurringRulesViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var confirmDeleteId by rememberSaveable { mutableStateOf<Long?>(null) }
    BackHandler(onBack = onBack)

    LazyColumn(
        Modifier.fillMaxSize().statusBarsPadding(),
        // Ruang bawah agar item terakhir tidak tertutup navbar dan tombol tambah.
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 190.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Image(
                        painterResource(R.drawable.ic_back),
                        contentDescription = "Kembali",
                        colorFilter = ColorFilter.tint(GlassColors.TextPrimary),
                    )
                }
                Text("Transaksi Berulang", color = GlassColors.TextPrimary, fontSize = 20.sp)
            }
        }
        if (state.actionFailed) {
            item { Text("Gagal menyimpan perubahan. Coba lagi.", color = GlassColors.AccentRed) }
        }
        if (!state.loading && state.rules.isEmpty()) {
            item {
                Text(
                    "Belum ada transaksi berulang. Nyalakan \"Ulangi tiap bulan\" saat mencatat gaji atau tagihan tetap.",
                    color = GlassColors.TextSecondary,
                )
            }
        }
        items(state.rules, key = { it.id }) { rule ->
            RuleCard(
                rule = rule,
                onActiveChange = { viewModel.setActive(rule.id, it) },
                onDelete = { confirmDeleteId = rule.id },
            )
        }
    }

    val deleting = state.rules.firstOrNull { it.id == confirmDeleteId }
    if (deleting != null) {
        AlertDialog(
            onDismissRequest = { confirmDeleteId = null },
            title = { Text("Hapus \"${deleting.title()}\"?") },
            text = { Text("Transaksi yang sudah tercatat tetap ada. Pencatatan otomatis berikutnya berhenti.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.delete(deleting.id)
                    confirmDeleteId = null
                }) { Text("Hapus", color = GlassColors.AccentRed) }
            },
            dismissButton = { TextButton(onClick = { confirmDeleteId = null }) { Text("Batal") } },
        )
    }
}

private fun RecurringRule.title() = note ?: category?.name ?: "Transaksi berulang"

@Composable
private fun RuleCard(rule: RecurringRule, onActiveChange: (Boolean) -> Unit, onDelete: () -> Unit) {
    val income = rule.type == TransactionType.INCOME
    GlassCard(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Seluruh area teks + switch satu target sentuh, dibaca TalkBack sebagai switch berlabel.
            Row(
                Modifier
                    .weight(1f)
                    .toggleable(value = rule.isActive, role = Role.Switch, onValueChange = onActiveChange),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f).alpha(if (rule.isActive) 1f else 0.5f)) {
                    Text(rule.title(), color = GlassColors.TextPrimary, fontSize = 16.sp)
                    Text(
                        "Setiap tanggal ${rule.dayOfMonth}" + (rule.category?.let { " · ${it.name}" } ?: ""),
                        color = GlassColors.TextSecondary,
                        fontSize = 12.sp,
                    )
                    Text(
                        (if (income) "+" else "-") + formatRupiah(rule.amount),
                        color = if (income) GlassColors.AccentGreen else GlassColors.AccentRed,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                Switch(checked = rule.isActive, onCheckedChange = null)
            }
            IconButton(onClick = onDelete) {
                Image(
                    painterResource(R.drawable.ic_delete),
                    contentDescription = "Hapus ${rule.title()}",
                    modifier = Modifier.size(22.dp),
                    colorFilter = ColorFilter.tint(GlassColors.TextSecondary),
                )
            }
        }
    }
}
