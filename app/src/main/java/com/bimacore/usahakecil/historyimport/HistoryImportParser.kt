package com.bimacore.usahakecil.historyimport

import com.bimacore.usahakecil.domain.BusinessType
import com.bimacore.usahakecil.domain.MoneyMath
import com.bimacore.usahakecil.domain.PaymentMethod
import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.decodeFromJsonElement

class HistoryImportParser {
    private val json = Json {
        ignoreUnknownKeys = false
        isLenient = false
        explicitNulls = true
        coerceInputValues = false
    }

    fun parse(
        bytes: ByteArray,
        expectedBusinessType: BusinessType,
        existingFingerprints: Set<String> = emptySet(),
    ): HistoryImportDraft {
        require(bytes.isNotEmpty()) { "File impor kosong" }
        require(bytes.size <= HistoryImportLimits.MAX_BYTES) {
            "File impor melebihi batas 1 MB"
        }
        val decoder = StandardCharsets.UTF_8.newDecoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT)
        val text = runCatching { decoder.decode(ByteBuffer.wrap(bytes)).toString() }
            .getOrElse { throw IllegalArgumentException("File impor bukan UTF-8 yang valid") }
        return parse(text, expectedBusinessType, existingFingerprints, sha256(bytes))
    }

    fun parse(
        text: String,
        expectedBusinessType: BusinessType,
        existingFingerprints: Set<String> = emptySet(),
        contentHash: String = sha256(text.encodeToByteArray()),
    ): HistoryImportDraft {
        require(text.encodeToByteArray().size <= HistoryImportLimits.MAX_BYTES) {
            "Data impor melebihi batas 1 MB"
        }
        val element = runCatching { json.parseToJsonElement(text) }
            .getOrElse { throw IllegalArgumentException("Format JSON tidak bisa dibaca") }
        validateStructure(element)
        val payload = runCatching { json.decodeFromJsonElement<HistoryImportPayload>(element) }
            .getOrElse { throw IllegalArgumentException("Struktur JSON CatatToko tidak lengkap") }
        require(payload.schemaVersion == HISTORY_IMPORT_SCHEMA_V1) {
            "Format belum didukung. Gunakan $HISTORY_IMPORT_SCHEMA_V1"
        }
        require(payload.source.pageCount in 0..HistoryImportLimits.MAX_PAGES) {
            "Jumlah halaman sumber tidak valid"
        }
        require(payload.source.timezone == "Asia/Jakarta") {
            "Zona waktu yang didukung saat ini hanya Asia/Jakarta"
        }
        payload.source.businessType?.let { value ->
            require(value == expectedBusinessType.name) {
                "Data $value tidak dapat dimasukkan ke APK ${expectedBusinessType.name}"
            }
        }
        payload.source.title.validateNullableLength("Judul sumber", HistoryImportLimits.MAX_SHORT_TEXT)
        require(payload.records.isNotEmpty()) { "Tidak ada catatan untuk diperiksa" }
        require(payload.records.size <= HistoryImportLimits.MAX_RECORDS) {
            "Satu impor maksimal ${HistoryImportLimits.MAX_RECORDS} catatan"
        }
        val sourceRefs = payload.records.map(HistoryImportRecord::sourceRef)
        require(sourceRefs.size == sourceRefs.distinct().size) {
            "sourceRef ganda ditemukan dalam satu file"
        }

        val rows = payload.records.mapIndexed { index, record ->
            validateRecord(index, record, payload.source, existingFingerprints)
        }
        val computedReady = rows.count { it.status == HistoryImportReviewStatus.READY }
        val computedNeedsReview = rows.size - computedReady
        val datedRows = payload.records.mapNotNull(HistoryImportRecord::date).sorted()
        val computedFrom = datedRows.firstOrNull()
        val computedTo = datedRows.lastOrNull()
        val warnings = buildList {
            if (payload.summary.recordCount != rows.size) {
                add("Jumlah record ringkasan tidak cocok; CatatToko menghitung ulang.")
            }
            if (payload.summary.readyCount != computedReady ||
                payload.summary.needsReviewCount != computedNeedsReview
            ) {
                add("Status ringkasan AI tidak cocok; CatatToko memakai hasil pemeriksaan sendiri.")
            }
            if (payload.summary.dateFrom != computedFrom || payload.summary.dateTo != computedTo) {
                add("Rentang tanggal ringkasan tidak cocok; CatatToko menghitung ulang.")
            }
            addAll(payload.summary.warnings.filter(String::isNotBlank))
        }
        return HistoryImportDraft(
            payload = payload,
            contentHash = contentHash,
            rows = rows,
            warnings = warnings.distinct(),
        )
    }

    private fun validateStructure(element: JsonElement) {
        val root = element.requireObject("Akar JSON")
        root.requireExactKeys(setOf("schemaVersion", "source", "records", "summary"), "Akar JSON")
        root.required("schemaVersion").requireString("schemaVersion")

        val source = root.required("source").requireObject("source")
        source.requireExactKeys(setOf("title", "pageCount", "businessType", "timezone"), "source")
        source.required("title").requireNullableString("source.title")
        source.required("pageCount").requireInteger("source.pageCount")
        source.required("businessType").requireNullableString("source.businessType")
        source.required("timezone").requireString("source.timezone")

        val records = root.required("records") as? JsonArray
            ?: throw IllegalArgumentException("records wajib berupa daftar")
        records.forEachIndexed { index, item ->
            val record = item.requireObject("records[$index]")
            record.requireExactKeys(RECORD_KEYS, "records[$index]")
            setOf("sourceRef", "type", "rawText").forEach { key ->
                record.required(key).requireString("records[$index].$key")
            }
            setOf("date", "time", "partyName", "category", "paymentMethod", "note").forEach { key ->
                record.required(key).requireNullableString("records[$index].$key")
            }
            setOf("amount", "amountPaid", "stockDelta").forEach { key ->
                record.required(key).requireNullableInteger("records[$index].$key")
            }
            val items = record.required("items") as? JsonArray
                ?: throw IllegalArgumentException("records[$index].items wajib berupa daftar")
            items.forEachIndexed { itemIndex, value ->
                val line = value.requireObject("records[$index].items[$itemIndex]")
                line.requireExactKeys(ITEM_KEYS, "records[$index].items[$itemIndex]")
                setOf("productName", "variantName", "unitLabel").forEach { key ->
                    line.required(key).requireNullableString("records[$index].items[$itemIndex].$key")
                }
                setOf("quantity", "unitPrice", "subtotal").forEach { key ->
                    line.required(key).requireNullableInteger("records[$index].items[$itemIndex].$key")
                }
                line.required("uncertainFields").requireStringArray(
                    "records[$index].items[$itemIndex].uncertainFields",
                )
            }
            record.required("uncertainFields").requireStringArray("records[$index].uncertainFields")
            record.required("warnings").requireStringArray("records[$index].warnings")
        }

        val summary = root.required("summary").requireObject("summary")
        summary.requireExactKeys(
            setOf("recordCount", "readyCount", "needsReviewCount", "dateFrom", "dateTo", "warnings"),
            "summary",
        )
        setOf("recordCount", "readyCount", "needsReviewCount").forEach { key ->
            summary.required(key).requireInteger("summary.$key")
        }
        summary.required("dateFrom").requireNullableString("summary.dateFrom")
        summary.required("dateTo").requireNullableString("summary.dateTo")
        summary.required("warnings").requireStringArray("summary.warnings")
    }

    private fun validateRecord(
        index: Int,
        record: HistoryImportRecord,
        source: HistoryImportSource,
        existingFingerprints: Set<String>,
    ): HistoryImportReviewRow {
        require(record.sourceRef.isNotBlank() && record.sourceRef.length <= HistoryImportLimits.MAX_SOURCE_REF) {
            "sourceRef pada baris ${index + 1} tidak valid"
        }
        require(record.items.size <= HistoryImportLimits.MAX_ITEMS_PER_RECORD) {
            "Baris ${record.sourceRef} memiliki terlalu banyak item"
        }
        require(record.uncertainFields.size <= HistoryImportLimits.MAX_LIST_ITEMS &&
            record.warnings.size <= HistoryImportLimits.MAX_LIST_ITEMS
        ) { "Terlalu banyak penanda pemeriksaan pada ${record.sourceRef}" }
        record.partyName.validateNullableLength("Nama pihak", HistoryImportLimits.MAX_SHORT_TEXT)
        record.category.validateNullableLength("Kategori", HistoryImportLimits.MAX_SHORT_TEXT)
        record.note.validateNullableLength("Catatan", HistoryImportLimits.MAX_NOTE)
        require(record.rawText.length <= HistoryImportLimits.MAX_RAW_TEXT) { "Teks sumber terlalu panjang" }
        record.warnings.forEach { require(it.length <= HistoryImportLimits.MAX_WARNING) { "Peringatan terlalu panjang" } }
        require(record.uncertainFields.all { it in RECORD_UNCERTAIN_FIELDS }) {
            "Nama field ragu pada ${record.sourceRef} tidak dikenal"
        }

        val type = runCatching { HistoryImportRecordType.valueOf(record.type) }
            .getOrElse { throw IllegalArgumentException("Jenis ${record.type} tidak didukung") }
        val reviewIssues = mutableListOf<String>()
        reviewIssues += record.warnings.filter(String::isNotBlank)
        if (record.uncertainFields.isNotEmpty()) {
            reviewIssues += "Ada ${record.uncertainFields.size} bagian yang ditandai ragu oleh AI"
        }
        val issues = mutableListOf<String>()
        val eventAt = parseEventAt(record.date, record.time, issues)
        val paymentMethod = record.paymentMethod?.let { method ->
            runCatching { PaymentMethod.valueOf(method) }.getOrElse {
                issues += "Metode pembayaran tidak dikenal"
                null
            }
        }
        validateMoney(record.amount, "Total", issues)
        validateMoney(record.amountPaid, "Jumlah dibayar", issues, allowZero = true)
        if (record.amount != null && record.amountPaid != null && record.amountPaid > record.amount) {
            issues += "Jumlah dibayar melebihi total"
        }
        record.items.forEachIndexed { itemIndex, item ->
            validateItem(itemIndex, item, issues, reviewIssues)
        }

        when (type) {
            HistoryImportRecordType.SALE -> validateSale(record, paymentMethod, issues)
            HistoryImportRecordType.PURCHASE -> validatePurchase(record, paymentMethod, issues)
            HistoryImportRecordType.CASH_IN,
            HistoryImportRecordType.CASH_OUT,
            HistoryImportRecordType.EXPENSE,
            -> validateCash(record, paymentMethod, issues)
            HistoryImportRecordType.RECEIVABLE,
            HistoryImportRecordType.PAYABLE,
            -> validateDebt(record, paymentMethod, issues)
            HistoryImportRecordType.STOCK_ADJUSTMENT -> {
                issues += "Penyesuaian stok disimpan sebagai arsip dan tidak mengubah stok saat ini"
            }
            HistoryImportRecordType.UNRESOLVED -> {
                issues += "Jenis catatan belum dapat ditentukan"
            }
        }
        if (type !in setOf(HistoryImportRecordType.UNRESOLVED, HistoryImportRecordType.STOCK_ADJUSTMENT) && eventAt == null) {
            issues += "Tanggal transaksi wajib diperiksa"
        }

        val fingerprint = sha256(
            json.encodeToString(
                FingerprintPayload(source.title, source.businessType, source.timezone, record),
            ).encodeToByteArray(),
        )
        val allIssues = (reviewIssues + issues).distinct()
        val status = when {
            fingerprint in existingFingerprints -> HistoryImportReviewStatus.DUPLICATE
            type == HistoryImportRecordType.UNRESOLVED || type == HistoryImportRecordType.STOCK_ADJUSTMENT -> {
                HistoryImportReviewStatus.UNRESOLVED
            }
            issues.isNotEmpty() -> HistoryImportReviewStatus.UNRESOLVED
            reviewIssues.isNotEmpty() -> HistoryImportReviewStatus.NEEDS_REVIEW
            else -> HistoryImportReviewStatus.READY
        }
        return HistoryImportReviewRow(
            index = index,
            record = record,
            recordType = type,
            status = status,
            issues = allIssues,
            fingerprint = fingerprint,
            eventAt = eventAt,
            timePrecision = if (record.time == null) "DATE_ONLY" else "EXACT",
            canApprove = status == HistoryImportReviewStatus.NEEDS_REVIEW,
        )
    }

    private fun validateItem(
        index: Int,
        item: HistoryImportItem,
        issues: MutableList<String>,
        reviewIssues: MutableList<String>,
    ) {
        require(item.uncertainFields.size <= HistoryImportLimits.MAX_LIST_ITEMS) {
            "Terlalu banyak field ragu pada item ${index + 1}"
        }
        require(item.uncertainFields.all { it in ITEM_UNCERTAIN_FIELDS }) {
            "Nama field ragu pada item ${index + 1} tidak dikenal"
        }
        item.productName.validateNullableLength("Nama produk", HistoryImportLimits.MAX_SHORT_TEXT)
        item.variantName.validateNullableLength("Nama varian", HistoryImportLimits.MAX_SHORT_TEXT)
        item.unitLabel.validateNullableLength("Satuan", HistoryImportLimits.MAX_SHORT_TEXT)
        if (item.uncertainFields.isNotEmpty()) {
            reviewIssues += "Item ${index + 1} mempunyai bagian yang ditandai ragu oleh AI"
        }
        if (item.quantity != null && item.quantity !in 1..MoneyMath.MAX_QUANTITY) {
            issues += "Jumlah item ${index + 1} tidak valid"
        }
        validateMoney(item.unitPrice, "Harga item ${index + 1}", issues, allowZero = true)
        validateMoney(item.subtotal, "Subtotal item ${index + 1}", issues, allowZero = true)
        if (item.quantity != null && item.unitPrice != null && item.subtotal != null) {
            val calculated = runCatching { MoneyMath.multiply(item.unitPrice, item.quantity) }.getOrNull()
            if (calculated != item.subtotal) issues += "Subtotal item ${index + 1} tidak cocok"
        }
    }

    private fun validateSale(
        record: HistoryImportRecord,
        method: PaymentMethod?,
        issues: MutableList<String>,
    ) {
        if (record.items.isEmpty()) issues += "Penjualan tidak memiliki rincian barang"
        if (record.amount == null || record.amount <= 0) issues += "Total penjualan belum valid"
        if (method == null) issues += "Metode pembayaran penjualan belum jelas"
        if (method == PaymentMethod.CREDIT && record.partyName.isNullOrBlank()) {
            issues += "Penjualan piutang membutuhkan nama pelanggan"
        }
        if (method == PaymentMethod.CREDIT && (record.amountPaid ?: 0L) > 0) {
            issues += "Metode pembayaran awal piutang belum dijelaskan oleh format v1"
        }
        if (
            method != null &&
            method != PaymentMethod.CREDIT &&
            record.amountPaid != null &&
            record.amountPaid != record.amount
        ) {
            issues += "Jumlah dibayar harus sama dengan total untuk penjualan non-piutang"
        }
        validateCompleteItems(record, issues)
    }

    private fun validatePurchase(
        record: HistoryImportRecord,
        method: PaymentMethod?,
        issues: MutableList<String>,
    ) {
        if (record.partyName.isNullOrBlank()) issues += "Pembelian membutuhkan nama pemasok"
        if (record.items.isEmpty()) issues += "Pembelian tidak memiliki rincian barang"
        if (record.amount == null || record.amount <= 0) issues += "Total pembelian belum valid"
        if ((record.amountPaid ?: 0L) > 0 && method == null) issues += "Metode pembayaran pembelian belum jelas"
        validateCompleteItems(record, issues)
    }

    private fun validateCompleteItems(record: HistoryImportRecord, issues: MutableList<String>) {
        record.items.forEachIndexed { index, item ->
            if (item.productName.isNullOrBlank() || item.quantity == null || item.unitPrice == null || item.subtotal == null) {
                issues += "Rincian item ${index + 1} belum lengkap"
            }
        }
        val subtotals = record.items.mapNotNull(HistoryImportItem::subtotal)
        if (subtotals.size == record.items.size && record.amount != null) {
            val total = runCatching { subtotals.fold(0L, Math::addExact) }.getOrNull()
            if (total != record.amount) issues += "Jumlah subtotal tidak sama dengan total"
        }
    }

    private fun validateCash(
        record: HistoryImportRecord,
        method: PaymentMethod?,
        issues: MutableList<String>,
    ) {
        if (record.amount == null || record.amount <= 0) issues += "Nominal kas belum valid"
        if (record.category.isNullOrBlank()) issues += "Kategori kas wajib diisi"
        if (method == null || method == PaymentMethod.CREDIT) issues += "Metode kas belum valid"
        if (record.items.isNotEmpty()) issues += "Catatan kas tidak boleh mempunyai item barang"
    }

    private fun validateDebt(
        record: HistoryImportRecord,
        method: PaymentMethod?,
        issues: MutableList<String>,
    ) {
        if (record.partyName.isNullOrBlank()) issues += "Nama pihak utang/piutang wajib diisi"
        if (record.amount == null || record.amount <= 0) issues += "Nilai utang/piutang belum valid"
        if ((record.amountPaid ?: 0L) > 0 && (method == null || method == PaymentMethod.CREDIT)) {
            issues += "Metode pembayaran utang/piutang belum valid"
        }
        if (record.items.isNotEmpty()) issues += "Catatan utang/piutang tidak boleh mempunyai item"
    }

    private fun parseEventAt(date: String?, time: String?, issues: MutableList<String>): Long? {
        if (date == null) {
            if (time != null) issues += "Waktu ada tetapi tanggal kosong"
            return null
        }
        if (!DATE_REGEX.matches(date)) {
            issues += "Format tanggal harus YYYY-MM-DD"
            return null
        }
        if (time != null && !TIME_REGEX.matches(time)) {
            issues += "Format waktu harus HH:mm"
            return null
        }
        val value = "$date ${time ?: "12:00"}"
        val format = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).apply {
            isLenient = false
            this.timeZone = TimeZone.getTimeZone("Asia/Jakarta")
        }
        return runCatching { requireNotNull(format.parse(value)).time }.getOrElse {
            issues += "Tanggal atau waktu tidak valid"
            null
        }
    }

    private fun validateMoney(
        value: Long?,
        label: String,
        issues: MutableList<String>,
        allowZero: Boolean = false,
    ) {
        if (value == null) return
        val range = if (allowZero) 0L..MoneyMath.MAX_MONEY else 1L..MoneyMath.MAX_MONEY
        if (value !in range) issues += "$label di luar batas"
    }

    @kotlinx.serialization.Serializable
    private data class FingerprintPayload(
        val title: String?,
        val businessType: String?,
        val timezone: String,
        val record: HistoryImportRecord,
    )

    companion object {
        private val DATE_REGEX = Regex("\\d{4}-\\d{2}-\\d{2}")
        private val TIME_REGEX = Regex("(?:[01]\\d|2[0-3]):[0-5]\\d")
        private val INTEGER_REGEX = Regex("-?(?:0|[1-9]\\d*)")
        private val RECORD_KEYS = setOf(
            "sourceRef", "type", "date", "time", "partyName", "category", "paymentMethod",
            "amount", "amountPaid", "items", "stockDelta", "note", "rawText", "uncertainFields", "warnings",
        )
        private val ITEM_KEYS = setOf(
            "productName", "variantName", "quantity", "unitLabel", "unitPrice", "subtotal", "uncertainFields",
        )
        private val RECORD_UNCERTAIN_FIELDS = RECORD_KEYS - setOf("sourceRef", "uncertainFields", "warnings")
        private val ITEM_UNCERTAIN_FIELDS = ITEM_KEYS - "uncertainFields"

        fun sha256(bytes: ByteArray): String = MessageDigest.getInstance("SHA-256")
            .digest(bytes)
            .joinToString("") { "%02x".format(it) }

        private fun String?.validateNullableLength(label: String, max: Int) {
            require(this == null || length <= max) { "$label terlalu panjang" }
        }

        private fun JsonElement.requireObject(label: String): JsonObject = this as? JsonObject
            ?: throw IllegalArgumentException("$label wajib berupa objek")

        private fun JsonObject.required(key: String): JsonElement = this[key]
            ?: throw IllegalArgumentException("Field $key tidak ditemukan")

        private fun JsonObject.requireExactKeys(expected: Set<String>, label: String) {
            val unknown = keys - expected
            val missing = expected - keys
            require(unknown.isEmpty()) { "$label mempunyai field tidak dikenal: ${unknown.first()}" }
            require(missing.isEmpty()) { "$label kehilangan field: ${missing.first()}" }
        }

        private fun JsonElement.requireString(label: String) {
            val primitive = this as? JsonPrimitive
            require(primitive != null && primitive.isString) { "$label wajib berupa teks" }
        }

        private fun JsonElement.requireNullableString(label: String) {
            if (this.toString() == "null") return
            requireString(label)
        }

        private fun JsonElement.requireInteger(label: String) {
            val primitive = this as? JsonPrimitive
            require(primitive != null && !primitive.isString && INTEGER_REGEX.matches(primitive.content)) {
                "$label wajib berupa bilangan bulat"
            }
        }

        private fun JsonElement.requireNullableInteger(label: String) {
            if (this.toString() == "null") return
            requireInteger(label)
        }

        private fun JsonElement.requireStringArray(label: String) {
            val array = this as? JsonArray ?: throw IllegalArgumentException("$label wajib berupa daftar")
            require(array.size <= HistoryImportLimits.MAX_LIST_ITEMS) { "$label terlalu banyak" }
            array.forEach { it.requireString(label) }
        }
    }
}
