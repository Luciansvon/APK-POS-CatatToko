package com.bimacore.usahakecil.data

import androidx.room.withTransaction
import com.bimacore.usahakecil.domain.BusinessType
import com.bimacore.usahakecil.domain.LedgerRules
import com.bimacore.usahakecil.domain.PaymentMethod
import com.bimacore.usahakecil.historyimport.HistoryImportDraft
import com.bimacore.usahakecil.historyimport.HistoryImportParser
import com.bimacore.usahakecil.historyimport.HistoryImportRecord
import com.bimacore.usahakecil.historyimport.HistoryImportRecordType
import com.bimacore.usahakecil.historyimport.HistoryImportResult
import com.bimacore.usahakecil.historyimport.HistoryImportReviewRow
import com.bimacore.usahakecil.historyimport.HistoryImportReviewStatus
import com.bimacore.usahakecil.security.ReportSession
import java.util.Locale
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class HistoricalImportRepository(
    private val database: PosDatabase,
    private val businessType: BusinessType,
    private val ownerSession: ReportSession,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    private val importDao = database.historyImportDao()
    private val catalogDao = database.catalogDao()
    private val operationsDao = database.operationsDao()
    private val json = Json { explicitNulls = true }

    suspend fun existingFingerprints(): Set<String> = importDao.getFingerprints().toSet()

    suspend fun isContentImported(contentHash: String): Boolean =
        importDao.getBatchByContentHash(contentHash) != null

    suspend fun commit(draft: HistoryImportDraft): HistoryImportResult {
        ownerSession.requireOwner()
        require(draft.payload.source.businessType == null ||
            draft.payload.source.businessType == businessType.name
        ) { "Jenis usaha pada file tidak sesuai dengan APK" }
        require(draft.rows.any { it.status == HistoryImportReviewStatus.READY }) {
            "Belum ada catatan siap dimasukkan"
        }
        validateDraft(draft)
        return database.withTransaction {
            require(importDao.getBatchByContentHash(draft.contentHash) == null) {
                "File yang sama sudah pernah diimpor"
            }
            val appliedCount = draft.readyCount
            val archivedCount = draft.rows.size - appliedCount
            val batchId = importDao.insertBatch(
                HistoryImportBatchEntity(
                    schemaVersion = draft.payload.schemaVersion,
                    contentHash = draft.contentHash,
                    sourceTitle = draft.payload.source.title.orEmpty(),
                    timezone = draft.payload.source.timezone,
                    importedAt = clock(),
                    recordCount = draft.rows.size,
                    appliedCount = appliedCount,
                    archivedCount = archivedCount,
                ),
            )
            val products = catalogDao.getActiveProducts()
            val variants = catalogDao.getActiveVariants()
            val productByName = products.associateBy { it.name.normalized() }
            val variantByProductAndName = variants.associateBy { it.productId to it.label.normalized() }
            var applied = 0
            draft.rows.forEach { row ->
                val target = if (row.status == HistoryImportReviewStatus.READY) {
                    applyRecord(
                        batchId = batchId,
                        row = row,
                        productByName = productByName,
                        variantByProductAndName = variantByProductAndName,
                    ).also { applied++ }
                } else {
                    null
                }
                importDao.insertRecord(
                    HistoryImportRecordEntity(
                        batchId = batchId,
                        sourceRef = row.record.sourceRef,
                        recordType = row.recordType.name,
                        status = if (target == null) row.status.name else "IMPORTED",
                        originalDate = row.record.date,
                        originalTime = row.record.time,
                        timePrecision = row.timePrecision,
                        eventAt = row.eventAt,
                        fingerprint = row.fingerprint,
                        targetType = target?.first,
                        targetId = target?.second,
                        rawJson = json.encodeToString(row.record),
                        rawText = row.record.rawText,
                        issues = row.issues.joinToString(" | "),
                    ),
                )
            }
            check(applied == appliedCount) { "Jumlah catatan tersimpan tidak sesuai" }
            HistoryImportResult(
                batchId = batchId,
                appliedCount = applied,
                archivedCount = archivedCount,
                duplicateCount = draft.duplicateCount,
            )
        }
    }

    private suspend fun validateDraft(draft: HistoryImportDraft) {
        val canonical = HistoryImportParser().parse(
            text = json.encodeToString(draft.payload),
            expectedBusinessType = businessType,
            existingFingerprints = importDao.getFingerprints().toSet(),
            contentHash = draft.contentHash,
        )
        require(canonical.rows.size == draft.rows.size) {
            "Jumlah catatan berubah setelah diperiksa"
        }
        canonical.rows.zip(draft.rows).forEach { (expected, provided) ->
            require(
                provided.index == expected.index &&
                    provided.record == expected.record &&
                    provided.recordType == expected.recordType &&
                    provided.fingerprint == expected.fingerprint &&
                    provided.eventAt == expected.eventAt &&
                    provided.timePrecision == expected.timePrecision,
            ) { "Catatan ${provided.record.sourceRef} berubah setelah diperiksa" }
            if (provided.status == HistoryImportReviewStatus.READY) {
                require(
                    expected.status == HistoryImportReviewStatus.READY ||
                        expected.status == HistoryImportReviewStatus.NEEDS_REVIEW,
                ) { "Catatan ${provided.record.sourceRef} belum aman untuk dimasukkan" }
            }
        }
    }

    private suspend fun applyRecord(
        batchId: Long,
        row: HistoryImportReviewRow,
        productByName: Map<String, ProductEntity>,
        variantByProductAndName: Map<Pair<Long, String>, ProductVariantEntity>,
    ): Pair<String, Long> {
        val eventAt = requireNotNull(row.eventAt) { "Tanggal ${row.record.sourceRef} belum valid" }
        return when (row.recordType) {
            HistoryImportRecordType.SALE -> "SALE" to insertSale(
                batchId,
                row,
                eventAt,
                productByName,
                variantByProductAndName,
            )
            HistoryImportRecordType.PURCHASE -> "PURCHASE" to insertPurchase(
                batchId,
                row,
                eventAt,
                productByName,
                variantByProductAndName,
            )
            HistoryImportRecordType.CASH_IN,
            HistoryImportRecordType.CASH_OUT,
            HistoryImportRecordType.EXPENSE,
            -> "CASH_ENTRY" to insertCash(row, eventAt)
            HistoryImportRecordType.RECEIVABLE,
            HistoryImportRecordType.PAYABLE,
            -> "DEBT" to insertDebt(row, eventAt)
            HistoryImportRecordType.STOCK_ADJUSTMENT,
            HistoryImportRecordType.UNRESOLVED,
            -> error("Catatan ${row.record.sourceRef} belum aman untuk disimpan")
        }
    }

    private suspend fun insertSale(
        batchId: Long,
        row: HistoryImportReviewRow,
        eventAt: Long,
        productByName: Map<String, ProductEntity>,
        variantByProductAndName: Map<Pair<Long, String>, ProductVariantEntity>,
    ): Long {
        val record = row.record
        val total = requireNotNull(record.amount)
        val method = PaymentMethod.valueOf(requireNotNull(record.paymentMethod))
        val received = when (method) {
            PaymentMethod.CREDIT -> record.amountPaid ?: 0L
            else -> total
        }
        val customerId = record.partyName?.let { syntheticId("customer", it) }
        val profileName = database.profileDao().getProfile()?.businessName.orEmpty().ifBlank { "CatatToko" }
        val receipt = "IMP-S-$batchId-${row.index + 1}"
        val saleId = database.saleDao().insertSale(
            SaleEntity(
                receiptNumber = receipt,
                businessName = profileName,
                createdAt = eventAt,
                paymentMethod = method.name,
                total = total,
                amountReceived = received,
                changeAmount = 0,
                customerId = customerId,
                settlementStatus = when {
                    received >= total -> "PAID"
                    received > 0 -> "PARTIAL"
                    else -> "UNPAID"
                },
                orderStatus = "COMPLETED",
                note = importNote(record),
                updatedAt = eventAt,
                shiftId = null,
            ),
        )
        database.saleDao().insertItems(
            record.items.map { item ->
                val productName = requireNotNull(item.productName).trim()
                val product = productByName[productName.normalized()]
                val productId = product?.id ?: syntheticId("product", productName)
                val variant = item.variantName?.let { variantName ->
                    variantByProductAndName[productId to variantName.normalized()]
                }
                SaleItemEntity(
                    saleId = saleId,
                    productId = productId,
                    variantId = variant?.id ?: item.variantName?.let { syntheticId("variant:$productId", it) },
                    productName = productName,
                    variantName = item.variantName?.trim(),
                    categoryName = record.category?.trim().orEmpty().ifBlank { "Catatan lama" },
                    unitPrice = requireNotNull(item.unitPrice),
                    quantity = requireNotNull(item.quantity),
                    subtotal = requireNotNull(item.subtotal),
                    baseQuantity = item.quantity,
                    unitLabel = item.unitLabel?.trim().orEmpty().ifBlank { product?.unitLabel ?: "satuan" },
                    note = importNote(record),
                )
            },
        )
        if (method != PaymentMethod.CREDIT) {
            operationsDao.insertCashEntry(
                historicalCashEntry(
                    type = "SALE_IN",
                    amount = total,
                    category = "Penjualan",
                    note = "Penjualan $receipt • Impor catatan lama",
                    paymentMethod = method.name,
                    referenceType = "SALE",
                    referenceId = saleId,
                    eventAt = eventAt,
                ),
            )
        }
        if (method == PaymentMethod.CREDIT && received < total) {
            insertDebtForSource(
                kind = DebtKind.RECEIVABLE,
                partyName = requireNotNull(record.partyName),
                originalAmount = total,
                paidAmount = received,
                paymentMethod = null,
                sourceType = "SALE",
                sourceId = saleId,
                note = "Piutang $receipt • Impor catatan lama",
                eventAt = eventAt,
            )
        }
        return saleId
    }

    private suspend fun insertPurchase(
        batchId: Long,
        row: HistoryImportReviewRow,
        eventAt: Long,
        productByName: Map<String, ProductEntity>,
        variantByProductAndName: Map<Pair<Long, String>, ProductVariantEntity>,
    ): Long {
        val record = row.record
        val total = requireNotNull(record.amount)
        val paid = record.amountPaid ?: 0L
        val supplierName = requireNotNull(record.partyName).trim()
        val supplierId = syntheticId("supplier", supplierName)
        val invoice = "IMP-P-$batchId-${row.index + 1}"
        val purchaseId = operationsDao.insertPurchase(
            PurchaseEntity(
                supplierId = supplierId,
                supplierName = supplierName,
                invoiceNumber = invoice,
                total = total,
                amountPaid = paid,
                settlementStatus = LedgerRules.status(total, listOf(paid).filter { it > 0 }).name,
                note = importNote(record),
                createdAt = eventAt,
                updatedAt = eventAt,
            ),
        )
        operationsDao.insertPurchaseItems(
            record.items.map { item ->
                val productName = requireNotNull(item.productName).trim()
                val product = productByName[productName.normalized()]
                val productId = product?.id ?: syntheticId("product", productName)
                val variant = item.variantName?.let { variantName ->
                    variantByProductAndName[productId to variantName.normalized()]
                }
                PurchaseItemEntity(
                    purchaseId = purchaseId,
                    productId = productId,
                    variantId = variant?.id ?: item.variantName?.let { syntheticId("variant:$productId", it) },
                    productName = productName,
                    variantName = item.variantName?.trim(),
                    unitLabel = item.unitLabel?.trim().orEmpty().ifBlank { product?.unitLabel ?: "satuan" },
                    factorToBase = 1,
                    quantity = requireNotNull(item.quantity),
                    baseQuantity = item.quantity,
                    unitCost = requireNotNull(item.unitPrice),
                    subtotal = requireNotNull(item.subtotal),
                )
            },
        )
        if (paid > 0) {
            val method = requireNotNull(record.paymentMethod)
            operationsDao.insertCashEntry(
                historicalCashEntry(
                    type = "PURCHASE_OUT",
                    amount = paid,
                    category = "Pembelian",
                    note = "Pembayaran $invoice • Impor catatan lama",
                    paymentMethod = method,
                    referenceType = "PURCHASE",
                    referenceId = purchaseId,
                    eventAt = eventAt,
                ),
            )
        }
        if (paid < total) {
            insertDebtForSource(
                kind = DebtKind.PAYABLE,
                partyName = supplierName,
                originalAmount = total,
                paidAmount = paid,
                paymentMethod = record.paymentMethod,
                sourceType = "PURCHASE",
                sourceId = purchaseId,
                note = "Utang $invoice • Impor catatan lama",
                eventAt = eventAt,
            )
        }
        return purchaseId
    }

    private suspend fun insertCash(row: HistoryImportReviewRow, eventAt: Long): Long {
        val record = row.record
        return operationsDao.insertCashEntry(
            historicalCashEntry(
                type = row.recordType.name,
                amount = requireNotNull(record.amount),
                category = requireNotNull(record.category).trim(),
                note = importNote(record),
                paymentMethod = requireNotNull(record.paymentMethod),
                referenceType = "HISTORY_IMPORT",
                referenceId = null,
                eventAt = eventAt,
            ),
        )
    }

    private suspend fun insertDebt(row: HistoryImportReviewRow, eventAt: Long): Long {
        val record = row.record
        return insertDebtForSource(
            kind = if (row.recordType == HistoryImportRecordType.PAYABLE) DebtKind.PAYABLE else DebtKind.RECEIVABLE,
            partyName = requireNotNull(record.partyName),
            originalAmount = requireNotNull(record.amount),
            paidAmount = record.amountPaid ?: 0L,
            paymentMethod = record.paymentMethod,
            sourceType = "HISTORY_IMPORT",
            sourceId = syntheticId("debt-source", record.sourceRef),
            note = importNote(record),
            eventAt = eventAt,
            recordCashPayment = true,
        )
    }

    private suspend fun insertDebtForSource(
        kind: DebtKind,
        partyName: String,
        originalAmount: Long,
        paidAmount: Long,
        paymentMethod: String?,
        sourceType: String,
        sourceId: Long,
        note: String,
        eventAt: Long,
        recordCashPayment: Boolean = false,
    ): Long {
        val debtId = operationsDao.insertDebt(
            DebtEntity(
                kind = kind.name,
                partyId = syntheticId(kind.name, partyName),
                partyName = partyName.trim(),
                sourceType = sourceType,
                sourceId = sourceId,
                originalAmount = originalAmount,
                paidAmount = paidAmount,
                settlementStatus = LedgerRules.status(
                    originalAmount,
                    listOf(paidAmount).filter { it > 0 },
                ).name,
                note = note,
                createdAt = eventAt,
                updatedAt = eventAt,
            ),
        )
        if (paidAmount > 0) {
            operationsDao.insertDebtPayment(
                DebtPaymentEntity(
                    debtId = debtId,
                    amount = paidAmount,
                    paymentMethod = requireNotNull(paymentMethod),
                    note = "Pembayaran historis • Impor catatan lama",
                    paidAt = eventAt,
                ),
            )
            if (recordCashPayment) {
                operationsDao.insertCashEntry(
                    historicalCashEntry(
                        type = if (kind == DebtKind.PAYABLE) "PAYABLE_OUT" else "RECEIVABLE_IN",
                        amount = paidAmount,
                        category = if (kind == DebtKind.PAYABLE) "Bayar utang" else "Terima piutang",
                        note = "Pembayaran historis • Impor catatan lama",
                        paymentMethod = requireNotNull(paymentMethod),
                        referenceType = "DEBT",
                        referenceId = debtId,
                        eventAt = eventAt,
                    ),
                )
            }
        }
        return debtId
    }

    private fun historicalCashEntry(
        type: String,
        amount: Long,
        category: String,
        note: String,
        paymentMethod: String,
        referenceType: String?,
        referenceId: Long?,
        eventAt: Long,
    ) = CashEntryEntity(
        type = type,
        amount = amount,
        category = category,
        note = note,
        paymentMethod = paymentMethod,
        referenceType = referenceType,
        referenceId = referenceId,
        createdAt = eventAt,
        shiftId = null,
    )

    private fun importNote(record: HistoryImportRecord): String = buildString {
        append("Impor catatan lama • ")
        append(record.sourceRef)
        record.note?.trim()?.takeIf(String::isNotBlank)?.let { append(" • ").append(it) }
    }.take(1_000)

    private fun syntheticId(namespace: String, value: String): Long {
        val digest = HistoryImportParser.sha256("$namespace|${value.normalized()}".encodeToByteArray())
        return -digest.take(15).toLong(16) - 1L
    }

    private fun String.normalized(): String = trim().lowercase(Locale.ROOT).replace(Regex("\\s+"), " ")
}
