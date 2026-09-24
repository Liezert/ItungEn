package com.liezert.itungen.ui.transactions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.liezert.glass.components.GlassCard
import com.liezert.glass.tokens.GlassColors
import com.liezert.itungen.domain.model.Transaction
import com.liezert.itungen.domain.model.TransactionType
import com.liezert.itungen.domain.model.formatRupiah
import com.liezert.itungen.ui.theme.formatId

@Composable
fun TransactionsScreen(viewModel: TransactionsViewModel = viewModel(factory = TransactionsViewModel.Factory)) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LazyColumn(
        Modifier.fillMaxSize().statusBarsPadding(),
        // Ruang bawah agar item terakhir tidak tertutup navbar dan tombol tambah.
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 190.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            GlassCard(Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                Text("Total Saldo", color = GlassColors.TextSecondary, fontSize = 12.sp)
                Text(
                    formatRupiah(state.balance),
                    // Saldo negatif boleh, tapi ditandai jelas (§7.1).
                    color = if (state.balance < 0) GlassColors.AccentRed else GlassColors.TextPrimary,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
        if (!state.loading && state.days.isEmpty()) {
            item { Text("Belum ada transaksi. Tekan + untuk mencatat.", color = GlassColors.TextSecondary) }
        }
        state.days.forEach { (date, transactions) ->
            item(key = "day-$date") {
                Text(date.formatId(), color = GlassColors.TextSecondary, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
            }
            items(transactions, key = { it.id }) { TransactionRow(it) }
        }
    }
}

@Composable
private fun TransactionRow(transaction: Transaction) {
    val incoming = transaction.type == TransactionType.INCOME || transaction.type == TransactionType.TRANSFER_IN
    val categoryName = transaction.category?.name ?: "Transfer"
    GlassCard(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    transaction.note ?: categoryName,
                    color = GlassColors.TextPrimary,
                    fontSize = 16.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (transaction.note != null) {
                    Text(categoryName, color = GlassColors.TextSecondary, fontSize = 12.sp)
                }
            }
            Text(
                (if (incoming) "+" else "-") + formatRupiah(transaction.amount),
                color = when (transaction.type) {
                    TransactionType.INCOME -> GlassColors.AccentGreen
                    TransactionType.EXPENSE -> GlassColors.AccentRed
                    else -> GlassColors.TextSecondary
                },
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}
