package com.bimacore.usahakecil.ui

import android.content.ClipboardManager
import android.content.ClipData
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.bimacore.usahakecil.historyimport.HistoryImportDraft
import com.bimacore.usahakecil.historyimport.HistoryImportRecordType
import com.bimacore.usahakecil.historyimport.HistoryImportReviewRow
import com.bimacore.usahakecil.historyimport.HistoryImportReviewStatus
import com.bimacore.usahakecil.historyimport.HistoryImportUiState
import com.bimacore.usahakecil.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryImportScreen(
    viewModel: OperationsViewModel,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val state by viewModel.historyImportState.collectAsState()
    val busy by viewModel.busy.collectAsState()
    var input by remember { mutableStateOf("") }
    var showConfirmation by remember { mutableStateOf(false) }
    val openFile = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        viewModel.finishHistoryImportFileSelection(uri)
    }
    val draft = (state as? HistoryImportUiState.Review)?.draft

    Scaffold(
        topBar = {
            CatatTokoOwnerHeader(
                pageTitle = "Import catatan lama",
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Kembali")
                    }
                },
            )
        },
        bottomBar = {
            if (draft != null && draft.readyCount > 0) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                ) {
                    Button(
                        onClick = { showConfirmation = true },
                        enabled = !busy,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 52.dp)
                            .testTag("history-import-confirm"),
                        shape = OwnerActionShape,
                    ) {
                        Text("Masukkan ${draft.readyCount} catatan siap")
                    }
                }
            }
        },
    ) { padding ->
        when (val current = state) {
            HistoryImportUiState.Empty -> HistoryImportInput(
                input = input,
                onInputChanged = { input = it },
                onPaste = {
                    val clipboard = context.getSystemService(ClipboardManager::class.java)
                    input = clipboard?.primaryClip
                        ?.takeIf { it.itemCount > 0 }
                        ?.getItemAt(0)
                        ?.coerceToText(context)
                        ?.toString()
                        .orEmpty()
                },
                onCopyPrompt = {
                    val prompt = context.resources.openRawResource(R.raw.catattoko_history_import_prompt_v1)
                        .bufferedReader()
                        .use { it.readText() }
                    context.getSystemService(ClipboardManager::class.java)?.setPrimaryClip(
                        ClipData.newPlainText("Prompt impor CatatToko", prompt),
                    )
                    Toast.makeText(context, "Prompt CatatToko disalin", Toast.LENGTH_SHORT).show()
                },
                onChooseFile = {
                    viewModel.beginHistoryImportFileSelection()
                    try {
                        openFile.launch("application/json")
                    } catch (error: Exception) {
                        viewModel.finishHistoryImportFileSelection(
                            null,
                            error.message ?: "Pemilih file JSON tidak dapat dibuka",
                        )
                    }
                },
                onInspect = { viewModel.inspectHistoryImportText(input) },
                enabled = !busy,
                modifier = Modifier.padding(padding),
            )
            HistoryImportUiState.Loading -> OwnerEmptyState(
                title = "Memeriksa file...",
                message = "CatatToko sedang mengecek format, tanggal, nominal, dan kemungkinan data ganda.",
                modifier = Modifier.padding(padding).padding(16.dp),
            )
            is HistoryImportUiState.Review -> HistoryImportReview(
                draft = current.draft,
                onApprove = viewModel::approveHistoryImportRow,
                modifier = Modifier.padding(padding),
            )
            is HistoryImportUiState.Success -> OwnerEmptyState(
                title = "Import selesai",
                message = buildString {
                    append("${current.result.appliedCount} catatan masuk ke histori. ")
                    append("${current.result.archivedCount} catatan disimpan untuk ditinjau dan tidak mengubah data aktif.")
                },
                actionLabel = "Selesai",
                onAction = {
                    viewModel.clearHistoryImport()
                    onBack()
                },
                modifier = Modifier.padding(padding).padding(16.dp).testTag("history-import-success"),
            )
            is HistoryImportUiState.Error -> OwnerEmptyState(
                title = "File belum bisa dipakai",
                message = current.message,
                actionLabel = "Pilih atau tempel ulang",
                onAction = viewModel::clearHistoryImport,
                modifier = Modifier.padding(padding).padding(16.dp).testTag("history-import-error"),
            )
        }
    }

    if (showConfirmation && draft != null) {
        AlertDialog(
            onDismissRequest = { showConfirmation = false },
            shape = OwnerCardShape,
            title = { Text("Masukkan ke histori?") },
            text = {
                Text(
                    "${draft.readyCount} catatan siap akan dimasukkan. " +
                        "${draft.unresolvedCount + draft.duplicateCount + draft.needsReviewCount} catatan lain tidak diterapkan. " +
                        "Stok aktif tidak diubah dari catatan lama.",
                )
            },
            confirmButton = {
                Button(onClick = {
                    showConfirmation = false
                    viewModel.confirmHistoryImport()
                }) { Text("Ya, masukkan") }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmation = false }) { Text("Batal") }
            },
        )
    }
}

@Composable
private fun HistoryImportInput(
    input: String,
    onInputChanged: (String) -> Unit,
    onPaste: () -> Unit,
    onCopyPrompt: () -> Unit,
    onChooseFile: () -> Unit,
    onInspect: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize().padding(16.dp).testTag("history-import-input-list"),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Masukkan catatan lama",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    "Pakai Gemini, ChatGPT, atau AI lain untuk membaca foto. CatatToko cuma menerima hasil JSON—tanpa API key.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        item {
            OutlinedButton(
                onClick = onCopyPrompt,
                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp).testTag("history-import-copy-prompt"),
                shape = OwnerActionShape,
            ) { Text("1. Salin prompt untuk AI") }
        }
        item {
            Text(
                "2. Unggah foto dan tempel prompt di AI pilihanmu.\n3. Kembali ke sini, lalu tempel JSON atau pilih file.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        item {
            OutlinedTextField(
                value = input,
                onValueChange = onInputChanged,
                label = { Text("Tempel JSON CatatToko") },
                placeholder = { Text("{ \"schemaVersion\": \"catattoko.history-import.v1\", ... }") },
                minLines = 7,
                maxLines = 12,
                modifier = Modifier.fillMaxWidth().testTag("history-import-input"),
            )
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(
                    onClick = onPaste,
                    enabled = enabled,
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("history-import-paste"),
                    shape = OwnerActionShape,
                ) { Text("Tempel") }
                OutlinedButton(
                    onClick = onChooseFile,
                    enabled = enabled,
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("history-import-file"),
                    shape = OwnerActionShape,
                ) { Text("Pilih file") }
            }
        }
        item {
            Button(
                onClick = onInspect,
                enabled = input.isNotBlank() && enabled,
                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp).testTag("history-import-inspect"),
                shape = OwnerActionShape,
            ) { Text("Periksa data") }
        }
        item {
            Text(
                "Tidak ada API key di aplikasi. Data hanya diproses dari JSON yang kamu masukkan.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun HistoryImportReview(
    draft: HistoryImportDraft,
    onApprove: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            OwnerHeroCard(
                eyebrow = "Hasil pemeriksaan",
                value = "${draft.rows.size} catatan ditemukan",
                supportingText = "Hijau siap masuk. Kuning perlu keputusanmu. Merah tidak diterapkan. Data ganda dilewati.",
            )
        }
        item {
            OwnerBentoSurface {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    HistoryImportCount("Siap masuk", draft.readyCount)
                    HistoryImportCount("Perlu dicek", draft.needsReviewCount)
                    HistoryImportCount("Tidak dapat diterapkan", draft.unresolvedCount)
                    HistoryImportCount("Data ganda", draft.duplicateCount)
                }
            }
        }
        if (draft.warnings.isNotEmpty()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Catatan pemeriksaan", fontWeight = FontWeight.Bold)
                    draft.warnings.forEach { warning ->
                        Text("• $warning", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
        items(draft.rows, key = { it.index }) { row ->
            HistoryImportRecordCard(row, onApprove)
        }
        item { Spacer(Modifier.width(1.dp)) }
    }
}

@Composable
private fun HistoryImportCount(label: String, value: Int) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value.toString(), fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun HistoryImportRecordCard(
    row: HistoryImportReviewRow,
    onApprove: (Int) -> Unit,
) {
    val statusColor = when (row.status) {
        HistoryImportReviewStatus.READY -> MaterialTheme.colorScheme.primary
        HistoryImportReviewStatus.NEEDS_REVIEW -> MaterialTheme.colorScheme.tertiary
        HistoryImportReviewStatus.UNRESOLVED -> MaterialTheme.colorScheme.error
        HistoryImportReviewStatus.DUPLICATE -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    OwnerBentoSurface(modifier = Modifier.fillMaxWidth().testTag("history-import-row-${row.index}")) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(historyTypeLabel(row.recordType), fontWeight = FontWeight.Bold)
                Text(
                    historyStatusLabel(row.status),
                    color = statusColor,
                    fontWeight = FontWeight.Bold,
                )
            }
            Text(
                listOfNotNull(row.record.date, row.record.time, row.record.partyName)
                    .joinToString(" • ")
                    .ifBlank { row.record.sourceRef },
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            row.record.amount?.let { Text(formatRupiah(it), fontWeight = FontWeight.SemiBold) }
            if (row.record.rawText.isNotBlank()) {
                Text(
                    row.record.rawText,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            if (row.issues.isNotEmpty()) {
                HorizontalDivider(color = statusColor.copy(alpha = 0.35f))
                row.issues.forEach { issue ->
                    Text("• $issue", color = statusColor, style = MaterialTheme.typography.bodySmall)
                }
            }
            if (row.canApprove) {
                OutlinedButton(
                    onClick = { onApprove(row.index) },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                    shape = OwnerActionShape,
                ) { Text("Saya sudah cek, masukkan") }
            }
        }
    }
}

private fun historyStatusLabel(status: HistoryImportReviewStatus): String = when (status) {
    HistoryImportReviewStatus.READY -> "Siap masuk"
    HistoryImportReviewStatus.NEEDS_REVIEW -> "Perlu dicek"
    HistoryImportReviewStatus.UNRESOLVED -> "Tidak diterapkan"
    HistoryImportReviewStatus.DUPLICATE -> "Data ganda"
}

private fun historyTypeLabel(type: HistoryImportRecordType): String = when (type) {
    HistoryImportRecordType.SALE -> "Penjualan"
    HistoryImportRecordType.PURCHASE -> "Pembelian"
    HistoryImportRecordType.CASH_IN -> "Kas masuk"
    HistoryImportRecordType.CASH_OUT -> "Kas keluar"
    HistoryImportRecordType.EXPENSE -> "Pengeluaran"
    HistoryImportRecordType.RECEIVABLE -> "Piutang"
    HistoryImportRecordType.PAYABLE -> "Utang"
    HistoryImportRecordType.STOCK_ADJUSTMENT -> "Penyesuaian stok lama"
    HistoryImportRecordType.UNRESOLVED -> "Belum dikenali"
}
