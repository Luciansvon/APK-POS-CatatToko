package com.bimacore.usahakecil.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import com.bimacore.usahakecil.data.ShiftEntity
import com.bimacore.usahakecil.data.ShiftStatus
import com.bimacore.usahakecil.data.ShiftSummary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ShiftSection(
    shifts: List<ShiftEntity>,
    summary: ShiftSummary?,
    isLoading: Boolean,
    onOpenRequest: () -> Unit,
    onCloseRequest: () -> Unit,
) {
    SectionTitle("Shift kasir")
    when {
        isLoading -> Text("Memuat posisi kas...")
        summary != null -> {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        "Shift aktif - ${summary.shift.cashierName}",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        "Buka ${formatShiftDate(summary.shift.openedAt)} | Modal awal ${formatRupiah(summary.shift.openingCash)}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    ShiftMetric("Penjualan tunai", summary.cashSales)
                    ShiftMetric("Pemasukan lain", summary.otherCashIn)
                    ShiftMetric("Pengeluaran", summary.cashOut)
                    ShiftMetric("Kas seharusnya", summary.expectedCash)
                    Button(onClick = onCloseRequest, modifier = Modifier.fillMaxWidth()) {
                        Text("Tutup Shift")
                    }
                }
            }
        }
        else -> {
            InfoCard(
                "Belum ada shift aktif",
                "Buka shift untuk mulai mencatat modal awal dan selisih kas.",
            )
            Button(onClick = onOpenRequest, modifier = Modifier.fillMaxWidth()) {
                Text("Buka Shift")
            }
        }
    }

    val history = shifts.filter { it.status == ShiftStatus.CLOSED.name }.take(10)
    if (history.isNotEmpty()) {
        SectionTitle("Riwayat shift")
        history.forEach { shift ->
            val difference = shift.cashDifference ?: 0L
            InfoCard(
                "${shift.cashierName} | ${formatShiftDate(shift.openedAt)}",
                "Penjualan ${formatRupiah(shift.totalSales)} | Selisih ${formatSignedRupiah(difference)}",
            )
        }
    }
}

@Composable
private fun ShiftMetric(label: String, value: Long) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(label, modifier = Modifier.weight(1f))
        Text(formatRupiah(value), style = MaterialTheme.typography.titleSmall)
    }
}

@Composable
fun ShiftOpenDialog(
    onDismiss: () -> Unit,
    onSave: (cashierName: String, openingCash: Long, note: String) -> Unit,
) {
    var cashierName by remember { mutableStateOf("Kasir utama") }
    var openingCash by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Buka shift") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = cashierName,
                    onValueChange = { cashierName = it },
                    label = { Text("Nama kasir") },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = openingCash,
                    onValueChange = { if (it.all(Char::isDigit)) openingCash = it },
                    label = { Text("Modal awal") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Catatan (opsional)") },
                )
            }
        },
        confirmButton = {
            Button(onClick = { onSave(cashierName, openingCash.toLongOrNull() ?: 0L, note) }) {
                Text("Buka Shift")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } },
    )
}

@Composable
fun ShiftCloseDialog(
    onDismiss: () -> Unit,
    onSave: (closingCash: Long, note: String) -> Unit,
) {
    var closingCash by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Tutup shift") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = closingCash,
                    onValueChange = { if (it.all(Char::isDigit)) closingCash = it },
                    label = { Text("Uang tunai fisik") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Catatan/alasan selisih") },
                )
            }
        },
        confirmButton = {
            Button(onClick = { onSave(closingCash.toLongOrNull() ?: 0L, note) }) {
                Text("Tutup Shift")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } },
    )
}

private fun formatShiftDate(timestamp: Long): String =
    SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(timestamp))

private fun formatSignedRupiah(value: Long): String =
    if (value >= 0) "+${formatRupiah(value)}" else "-${formatRupiah(-value)}"
