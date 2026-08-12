package com.bimacore.usahakecil.historyimport

import android.content.Context
import android.net.Uri
import com.bimacore.usahakecil.data.HistoricalImportRepository
import com.bimacore.usahakecil.domain.BusinessType
import java.io.ByteArrayOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class HistoryImportManager(
    private val context: Context,
    private val businessType: BusinessType,
    private val repository: HistoricalImportRepository,
    private val parser: HistoryImportParser = HistoryImportParser(),
) {
    suspend fun inspectText(text: String): HistoryImportDraft = withContext(Dispatchers.IO) {
        val bytes = text.encodeToByteArray()
        inspectBytes(bytes)
    }

    suspend fun inspectUri(uri: Uri): HistoryImportDraft = withContext(Dispatchers.IO) {
        val bytes = context.contentResolver.openInputStream(uri)?.use(::readWithLimit)
            ?: throw IllegalArgumentException("File impor tidak dapat dibuka")
        inspectBytes(bytes)
    }

    suspend fun commit(draft: HistoryImportDraft): HistoryImportResult = repository.commit(draft)

    private suspend fun inspectBytes(bytes: ByteArray): HistoryImportDraft {
        val contentHash = HistoryImportParser.sha256(bytes)
        require(!repository.isContentImported(contentHash)) { "File yang sama sudah pernah diimpor" }
        return parser.parse(
            bytes = bytes,
            expectedBusinessType = businessType,
            existingFingerprints = repository.existingFingerprints(),
        )
    }

    private fun readWithLimit(input: java.io.InputStream): ByteArray {
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(8_192)
        var total = 0
        while (true) {
            val read = input.read(buffer)
            if (read < 0) break
            total += read
            require(total <= HistoryImportLimits.MAX_BYTES) { "File impor melebihi batas 1 MB" }
            output.write(buffer, 0, read)
        }
        return output.toByteArray()
    }
}
