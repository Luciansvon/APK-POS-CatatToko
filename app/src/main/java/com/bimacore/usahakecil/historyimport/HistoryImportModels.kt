package com.bimacore.usahakecil.historyimport

import kotlinx.serialization.Serializable

const val HISTORY_IMPORT_SCHEMA_V1 = "catattoko.history-import.v1"

@Serializable
data class HistoryImportPayload(
    val schemaVersion: String,
    val source: HistoryImportSource,
    val records: List<HistoryImportRecord>,
    val summary: HistoryImportSummary,
)

@Serializable
data class HistoryImportSource(
    val title: String?,
    val pageCount: Int,
    val businessType: String?,
    val timezone: String,
)

@Serializable
data class HistoryImportRecord(
    val sourceRef: String,
    val type: String,
    val date: String?,
    val time: String?,
    val partyName: String?,
    val category: String?,
    val paymentMethod: String?,
    val amount: Long?,
    val amountPaid: Long?,
    val items: List<HistoryImportItem>,
    val stockDelta: Int?,
    val note: String?,
    val rawText: String,
    val uncertainFields: List<String>,
    val warnings: List<String>,
)

@Serializable
data class HistoryImportItem(
    val productName: String?,
    val variantName: String?,
    val quantity: Int?,
    val unitLabel: String?,
    val unitPrice: Long?,
    val subtotal: Long?,
    val uncertainFields: List<String>,
)

@Serializable
data class HistoryImportSummary(
    val recordCount: Int,
    val readyCount: Int,
    val needsReviewCount: Int,
    val dateFrom: String?,
    val dateTo: String?,
    val warnings: List<String>,
)

enum class HistoryImportRecordType {
    SALE,
    PURCHASE,
    CASH_IN,
    CASH_OUT,
    EXPENSE,
    RECEIVABLE,
    PAYABLE,
    STOCK_ADJUSTMENT,
    UNRESOLVED,
}

enum class HistoryImportReviewStatus {
    READY,
    NEEDS_REVIEW,
    UNRESOLVED,
    DUPLICATE,
}

data class HistoryImportReviewRow(
    val index: Int,
    val record: HistoryImportRecord,
    val recordType: HistoryImportRecordType,
    val status: HistoryImportReviewStatus,
    val issues: List<String>,
    val fingerprint: String,
    val eventAt: Long?,
    val timePrecision: String,
    val canApprove: Boolean,
)

data class HistoryImportDraft(
    val payload: HistoryImportPayload,
    val contentHash: String,
    val rows: List<HistoryImportReviewRow>,
    val warnings: List<String>,
) {
    val readyCount: Int get() = rows.count { it.status == HistoryImportReviewStatus.READY }
    val needsReviewCount: Int get() = rows.count { it.status == HistoryImportReviewStatus.NEEDS_REVIEW }
    val unresolvedCount: Int get() = rows.count { it.status == HistoryImportReviewStatus.UNRESOLVED }
    val duplicateCount: Int get() = rows.count { it.status == HistoryImportReviewStatus.DUPLICATE }
}

data class HistoryImportResult(
    val batchId: Long,
    val appliedCount: Int,
    val archivedCount: Int,
    val duplicateCount: Int,
)

sealed interface HistoryImportUiState {
    data object Empty : HistoryImportUiState
    data object Loading : HistoryImportUiState
    data class Review(val draft: HistoryImportDraft) : HistoryImportUiState
    data class Success(val result: HistoryImportResult) : HistoryImportUiState
    data class Error(val message: String) : HistoryImportUiState
}

object HistoryImportLimits {
    const val MAX_BYTES = 1024 * 1024
    const val MAX_RECORDS = 200
    const val MAX_ITEMS_PER_RECORD = 50
    const val MAX_PAGES = 1_000
    const val MAX_SOURCE_REF = 128
    const val MAX_SHORT_TEXT = 160
    const val MAX_NOTE = 1_000
    const val MAX_RAW_TEXT = 4_000
    const val MAX_LIST_ITEMS = 20
    const val MAX_WARNING = 500
}
