package com.bimacore.usahakecil.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface HistoryImportDao {
    @Query("SELECT * FROM history_import_batches WHERE contentHash = :contentHash LIMIT 1")
    suspend fun getBatchByContentHash(contentHash: String): HistoryImportBatchEntity?

    @Query("SELECT fingerprint FROM history_import_records")
    suspend fun getFingerprints(): List<String>

    @Insert
    suspend fun insertBatch(batch: HistoryImportBatchEntity): Long

    @Insert
    suspend fun insertRecord(record: HistoryImportRecordEntity): Long

    @Query("SELECT COUNT(*) FROM history_import_batches")
    suspend fun batchCount(): Int

    @Query("SELECT COUNT(*) FROM history_import_records WHERE batchId = :batchId AND status = :status")
    suspend fun recordCount(batchId: Long, status: String): Int
}
