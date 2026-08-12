package com.bimacore.usahakecil.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "history_import_batches",
    indices = [Index(value = ["contentHash"], unique = true)],
)
data class HistoryImportBatchEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val schemaVersion: String,
    val contentHash: String,
    val sourceTitle: String,
    val timezone: String,
    val importedAt: Long,
    val recordCount: Int,
    val appliedCount: Int,
    val archivedCount: Int,
)

@Entity(
    tableName = "history_import_records",
    indices = [
        Index(value = ["batchId", "sourceRef"], unique = true),
        Index("fingerprint"),
        Index("targetType", "targetId"),
    ],
)
data class HistoryImportRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val batchId: Long,
    val sourceRef: String,
    val recordType: String,
    val status: String,
    val originalDate: String?,
    val originalTime: String?,
    val timePrecision: String,
    val eventAt: Long?,
    val fingerprint: String,
    val targetType: String?,
    val targetId: Long?,
    val rawJson: String,
    val rawText: String,
    val issues: String,
)
