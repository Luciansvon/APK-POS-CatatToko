package com.bimacore.usahakecil.data

import androidx.room.withTransaction
import com.bimacore.usahakecil.domain.InventoryRules
import com.bimacore.usahakecil.domain.LedgerLine
import com.bimacore.usahakecil.domain.LedgerRules
import com.bimacore.usahakecil.domain.MoneyMath
import com.bimacore.usahakecil.security.ReportSession
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.Flow

enum class PartyKind {
    SUPPLIER,
    CUSTOMER,
}

enum class DebtKind {
    PAYABLE,
    RECEIVABLE,
}

enum class ManualCashType {
    CASH_IN,
    CASH_OUT,
    EXPENSE,
}

data class PurchaseLineDraft(
    val productId: Long,
    val variantId: Long? = null,
    val unitLabel: String,
    val factorToBase: Int = 1,
    val quantity: Int,
    val unitCost: Long,
)

data class PurchaseDraft(
    val supplierId: Long,
    val invoiceNumber: String = "",
    val amountPaid: Long,
    val note: String = "",
    val lines: List<PurchaseLineDraft>,
)

enum class ShiftStatus {
    OPEN,
    CLOSED,
}

data class ShiftSummary(
    val shift: ShiftEntity,
    val totalSales: Long,
    val cashSales: Long,
    val nonCashSales: Long,
    val otherCashIn: Long,
    val cashOut: Long,
    val refundAmount: Long,
    val expectedCash: Long,
    val physicalCash: Long? = shift.closingCash,
    val cashDifference: Long? = shift.cashDifference,
)

class OperationsRepository(
    private val database: PosDatabase,
    private val clock: () -> Long = System::currentTimeMillis,
    private val ownerSession: ReportSession? = null,
) {
    private val operationsDao = database.operationsDao()
    private val catalogDao = database.catalogDao()
    private val shiftDao = database.shiftDao()

    val suppliers: Flow<List<PartyEntity>> = operationsDao.observeParties(PartyKind.SUPPLIER.name)
    val customers: Flow<List<PartyEntity>> = operationsDao.observeParties(PartyKind.CUSTOMER.name)
    val purchases: Flow<List<PurchaseEntity>> = operationsDao.observePurchases()
    val cashEntries: Flow<List<CashEntryEntity>> = operationsDao.observeCashEntries()
    val debts: Flow<List<DebtEntity>> = operationsDao.observeDebts()
    val shifts: Flow<List<ShiftEntity>> = ownerSession?.let { session ->
        combine(shiftDao.observeShifts(), session.unlocked) { shifts, unlocked ->
            if (unlocked) shifts else emptyList()
        }
    } ?: shiftDao.observeShifts()
    val openShift: Flow<ShiftEntity?> = shiftDao.observeOpenShift()

    suspend fun saveParty(
        id: Long?,
        kind: PartyKind,
        name: String,
        phone: String,
        address: String,
    ): Long {
        require(name.isNotBlank()) { "Nama wajib diisi" }
        val now = clock()
        return if (id == null) {
            operationsDao.insertParty(
                PartyEntity(
                    kind = kind.name,
                    name = name.trim(),
                    phone = phone.trim(),
                    address = address.trim(),
                    createdAt = now,
                    updatedAt = now,
                ),
            )
        } else {
            val current = requireNotNull(operationsDao.getParty(id)) { "Data pihak tidak tersedia" }
            operationsDao.updateParty(
                current.copy(
                    kind = kind.name,
                    name = name.trim(),
                    phone = phone.trim(),
                    address = address.trim(),
                    updatedAt = now,
                ),
            )
            id
        }
    }

    suspend fun setPartyActive(id: Long, active: Boolean) {
        val current = requireNotNull(operationsDao.getParty(id)) { "Data pihak tidak tersedia" }
        operationsDao.updateParty(current.copy(isActive = active, updatedAt = clock()))
    }

    suspend fun recordPurchase(draft: PurchaseDraft): Long = database.withTransaction {
        require(draft.lines.isNotEmpty()) { "Item pembelian masih kosong" }
        val supplier = requireNotNull(operationsDao.getParty(draft.supplierId)) {
            "Pemasok tidak tersedia"
        }
        require(supplier.kind == PartyKind.SUPPLIER.name && supplier.isActive) {
            "Pemasok tidak aktif"
        }
        val resolved = draft.lines.map { line ->
            require(line.quantity > 0) { "Jumlah pembelian harus lebih dari nol" }
            require(line.unitLabel.isNotBlank()) { "Satuan pembelian wajib diisi" }
            require(line.unitCost in 0..MoneyMath.MAX_MONEY) { "Harga beli tidak valid" }
            val product = requireNotNull(catalogDao.getProduct(line.productId)) {
                "Produk pembelian tidak tersedia"
            }
            val variant = line.variantId?.let {
                requireNotNull(catalogDao.getVariant(it)) { "Varian pembelian tidak tersedia" }
            }
            require(variant == null || variant.productId == product.id) { "Varian tidak sesuai produk" }
            require(!product.hasVariants || variant != null) {
                "Produk bervarian wajib memilih varian untuk pembelian"
            }
            val baseQuantity = InventoryRules.toBaseQuantity(line.quantity, line.factorToBase)
            val subtotal = MoneyMath.multiply(line.unitCost, line.quantity)
            ResolvedPurchaseLine(line, product, variant, baseQuantity, subtotal)
        }
        val total = LedgerRules.total(
            resolved.map { LedgerLine(it.line.unitCost, it.line.quantity) },
        )
        require(draft.amountPaid in 0..total) { "Pembayaran pembelian melebihi total" }

        val now = clock()
        val status = LedgerRules.status(total, listOf(draft.amountPaid).filter { it > 0 }).name
        val invoice = draft.invoiceNumber.trim().ifBlank { "BL-$now" }
        val purchaseId = operationsDao.insertPurchase(
            PurchaseEntity(
                supplierId = supplier.id,
                supplierName = supplier.name,
                invoiceNumber = invoice,
                total = total,
                amountPaid = draft.amountPaid,
                settlementStatus = status,
                note = draft.note.trim(),
                createdAt = now,
                updatedAt = now,
            ),
        )
        operationsDao.insertPurchaseItems(
            resolved.map {
                PurchaseItemEntity(
                    purchaseId = purchaseId,
                    productId = it.product.id,
                    variantId = it.variant?.id,
                    productName = it.product.name,
                    variantName = it.variant?.label,
                    unitLabel = it.line.unitLabel.trim(),
                    factorToBase = it.line.factorToBase,
                    quantity = it.line.quantity,
                    baseQuantity = it.baseQuantity,
                    unitCost = it.line.unitCost,
                    subtotal = it.subtotal,
                )
            },
        )
        val aggregatedVariantQty = resolved
            .filter { it.variant != null }
            .groupBy { it.variant!!.id }
        aggregatedVariantQty.forEach { (variantId, lines) ->
            val totalBaseQty = lines.fold(0) { acc, line ->
                Math.addExact(acc, line.baseQuantity)
            }
            val currentVariant = requireNotNull(catalogDao.getVariant(variantId)) {
                "Varian pembelian tidak tersedia"
            }
            val next = InventoryRules.adjustStock(
                currentVariant.stock,
                totalBaseQty,
                "Pembelian $invoice",
            )
            catalogDao.updateVariant(currentVariant.copy(stock = next, updatedAt = now))
        }

        val aggregatedProductQty = resolved
            .filter { it.variant == null }
            .groupBy { it.product.id }
        aggregatedProductQty.forEach { (productId, lines) ->
            val totalBaseQty = lines.fold(0) { acc, line ->
                Math.addExact(acc, line.baseQuantity)
            }
            val currentProduct = requireNotNull(catalogDao.getProduct(productId)) {
                "Produk pembelian tidak tersedia"
            }
            val next = InventoryRules.adjustStock(
                currentProduct.stock,
                totalBaseQty,
                "Pembelian $invoice",
            )
            catalogDao.updateProduct(currentProduct.copy(stock = next, updatedAt = now))
        }
        database.stockDao().insertMovements(
            resolved.map {
                StockMovementEntity(
                    productId = it.product.id,
                    variantId = it.variant?.id,
                    saleId = 0,
                    type = "PURCHASE",
                    quantityDelta = it.line.quantity,
                    reason = "Pembelian $invoice",
                    createdAt = now,
                    referenceType = "PURCHASE",
                    referenceId = purchaseId,
                    unitLabel = it.line.unitLabel.trim(),
                    baseQuantityDelta = it.baseQuantity,
                )
            },
        )
        if (draft.amountPaid > 0) {
            operationsDao.insertCashEntry(
                CashEntryEntity(
                    type = "PURCHASE_OUT",
                    amount = draft.amountPaid,
                    category = "Pembelian",
                    note = "Pembayaran pembelian $invoice",
                    paymentMethod = "CASH",
                    referenceType = "PURCHASE",
                    referenceId = purchaseId,
                    createdAt = now,
                    shiftId = shiftDao.getOpenShift()?.id,
                ),
            )
        }
        val remaining = total - draft.amountPaid
        if (remaining > 0) {
            val debtId = operationsDao.insertDebt(
                DebtEntity(
                    kind = DebtKind.PAYABLE.name,
                    partyId = supplier.id,
                    partyName = supplier.name,
                    sourceType = "PURCHASE",
                    sourceId = purchaseId,
                    originalAmount = total,
                    paidAmount = draft.amountPaid,
                    settlementStatus = status,
                    note = "Utang pembelian $invoice",
                    createdAt = now,
                    updatedAt = now,
                ),
            )
            if (draft.amountPaid > 0) {
                operationsDao.insertDebtPayment(
                    DebtPaymentEntity(
                        debtId = debtId,
                        amount = draft.amountPaid,
                        paymentMethod = "CASH",
                        note = "Pembayaran awal pembelian $invoice",
                        paidAt = now,
                    ),
                )
            }
        }
        purchaseId
    }

    suspend fun addManualCashEntry(
        type: ManualCashType,
        amount: Long,
        category: String,
        note: String,
        paymentMethod: String = "CASH",
    ): Long {
        require(amount in 1..MoneyMath.MAX_MONEY) { "Nominal wajib lebih dari nol" }
        require(category.isNotBlank()) { "Kategori wajib diisi" }
        return operationsDao.insertCashEntry(
            CashEntryEntity(
                type = type.name,
                amount = amount,
                category = category.trim(),
                note = note.trim(),
                paymentMethod = paymentMethod,
                referenceType = null,
                referenceId = null,
                createdAt = clock(),
                shiftId = if (paymentMethod == "CASH") shiftDao.getOpenShift()?.id else null,
            ),
        )
    }

    suspend fun openShift(
        cashierName: String,
        openingCash: Long,
        openingNote: String,
    ): Long = database.withTransaction {
        val normalizedCashier = cashierName.trim()
        require(normalizedCashier.isNotBlank()) { "Nama kasir wajib diisi" }
        require(openingCash in 0..MoneyMath.MAX_MONEY) { "Modal awal tidak valid" }
        require(shiftDao.getOpenShift() == null) { "Masih ada shift yang aktif" }
        shiftDao.insertShift(
            ShiftEntity(
                cashierName = normalizedCashier,
                openedAt = clock(),
                openingCash = openingCash,
                openingNote = openingNote.trim(),
                openSlot = 1,
            ),
        )
    }

    suspend fun readOpenShiftSummary(): ShiftSummary? {
        requireOwnerForShift()
        val shift = shiftDao.getOpenShift() ?: return null
        return calculateShiftSummary(shift)
    }

    suspend fun closeShift(
        closingCash: Long,
        closingNote: String,
    ): ShiftSummary = database.withTransaction {
        requireOwnerForShift()
        require(closingCash in 0..MoneyMath.MAX_MONEY) { "Uang fisik tidak valid" }
        val shift = requireNotNull(shiftDao.getOpenShift()) { "Belum ada shift aktif" }
        val closedAt = clock()
        val summary = calculateShiftSummary(shift)
        val cashDifference = Math.subtractExact(closingCash, summary.expectedCash)
        val closedShift = shift.copy(
            status = ShiftStatus.CLOSED.name,
            closedAt = closedAt,
            closingCash = closingCash,
            closingNote = closingNote.trim(),
            totalSales = summary.totalSales,
            cashSales = summary.cashSales,
            nonCashSales = summary.nonCashSales,
            otherCashIn = summary.otherCashIn,
            cashOut = summary.cashOut,
            refundAmount = summary.refundAmount,
            expectedCash = summary.expectedCash,
            cashDifference = cashDifference,
            openSlot = null,
        )
        shiftDao.updateShift(closedShift)
        summary.copy(
            shift = closedShift,
            physicalCash = closingCash,
            cashDifference = cashDifference,
        )
    }

    suspend fun createDebt(
        kind: DebtKind,
        partyId: Long,
        originalAmount: Long,
        initialPayment: Long,
        note: String,
    ): Long = database.withTransaction {
        val party = requireNotNull(operationsDao.getParty(partyId)) { "Pihak tidak tersedia" }
        val expectedKind = if (kind == DebtKind.PAYABLE) {
            PartyKind.SUPPLIER.name
        } else {
            PartyKind.CUSTOMER.name
        }
        require(party.kind == expectedKind) { "Jenis pihak tidak sesuai" }
        require(originalAmount in 1..MoneyMath.MAX_MONEY) { "Nilai tagihan tidak valid" }
        require(initialPayment in 0..originalAmount) { "Pembayaran awal tidak valid" }
        val now = clock()
        val status = LedgerRules.status(
            originalAmount,
            listOf(initialPayment).filter { it > 0 },
        ).name
        val debtId = operationsDao.insertDebt(
            DebtEntity(
                kind = kind.name,
                partyId = party.id,
                partyName = party.name,
                sourceType = "MANUAL",
                sourceId = now,
                originalAmount = originalAmount,
                paidAmount = initialPayment,
                settlementStatus = status,
                note = note.trim(),
                createdAt = now,
                updatedAt = now,
            ),
        )
        if (initialPayment > 0) {
            operationsDao.insertDebtPayment(
                DebtPaymentEntity(
                    debtId = debtId,
                    amount = initialPayment,
                    paymentMethod = "CASH",
                    note = "Pembayaran awal",
                    paidAt = now,
                ),
            )
            operationsDao.insertCashEntry(
                CashEntryEntity(
                    type = if (kind == DebtKind.PAYABLE) "PAYABLE_OUT" else "RECEIVABLE_IN",
                    amount = initialPayment,
                    category = if (kind == DebtKind.PAYABLE) "Bayar utang" else "Terima piutang",
                    note = "Pembayaran awal ${note.trim().ifBlank { "tagihan" }}",
                    paymentMethod = "CASH",
                    referenceType = "DEBT",
                    referenceId = debtId,
                    createdAt = now,
                    shiftId = if (initialPayment > 0) shiftDao.getOpenShift()?.id else null,
                ),
            )
        }
        debtId
    }

    suspend fun payDebt(
        debtId: Long,
        amount: Long,
        paymentMethod: String,
        note: String,
    ) = database.withTransaction {
        val debt = requireNotNull(operationsDao.getDebt(debtId)) { "Tagihan tidak tersedia" }
        require(amount in 1..MoneyMath.MAX_MONEY) { "Nominal pembayaran tidak valid" }
        val totalPaid = Math.addExact(debt.paidAmount, amount)
        require(totalPaid <= debt.originalAmount) { "Pembayaran melebihi sisa tagihan" }
        val now = clock()
        val status = LedgerRules.status(debt.originalAmount, listOf(totalPaid)).name
        operationsDao.insertDebtPayment(
            DebtPaymentEntity(
                debtId = debt.id,
                amount = amount,
                paymentMethod = paymentMethod,
                note = note.trim(),
                paidAt = now,
            ),
        )
        operationsDao.updateDebt(
            debt.copy(
                paidAmount = totalPaid,
                settlementStatus = status,
                updatedAt = now,
            ),
        )
        operationsDao.insertCashEntry(
            CashEntryEntity(
                type = if (debt.kind == DebtKind.PAYABLE.name) {
                    "PAYABLE_OUT"
                } else {
                    "RECEIVABLE_IN"
                },
                amount = amount,
                category = if (debt.kind == DebtKind.PAYABLE.name) "Bayar utang" else "Terima piutang",
                note = note.trim().ifBlank { debt.note },
                paymentMethod = paymentMethod,
                referenceType = "DEBT",
                referenceId = debt.id,
                createdAt = now,
                shiftId = if (paymentMethod == "CASH") shiftDao.getOpenShift()?.id else null,
            ),
        )
    }

    private data class ResolvedPurchaseLine(
        val line: PurchaseLineDraft,
        val product: ProductEntity,
        val variant: ProductVariantEntity?,
        val baseQuantity: Int,
        val subtotal: Long,
    )

    private suspend fun calculateShiftSummary(shift: ShiftEntity): ShiftSummary {
        val sales = database.saleDao().getSalesForShift(shift.id)
        val cashEntries = operationsDao.getCashEntriesForShift(shift.id)
        val totalSales = sales.sumAmounts { it.total }
        val cashSales = sales
            .filter { it.paymentMethod == "CASH" }
            .sumAmounts { it.total }
        val nonCashSales = sales
            .filter { it.paymentMethod != "CASH" }
            .sumAmounts { it.total }
        val otherCashIn = cashEntries
            .filter { it.paymentMethod == "CASH" && it.type in OTHER_CASH_IN_TYPES }
            .sumAmounts { it.amount }
        val cashOut = cashEntries
            .filter { it.paymentMethod == "CASH" && it.type in CASH_OUT_TYPES }
            .sumAmounts { it.amount }
        val refundAmount = cashEntries
            .filter { it.paymentMethod == "CASH" && it.type == REFUND_CASH_OUT_TYPE }
            .sumAmounts { it.amount }
        val expectedCash = Math.subtractExact(
            Math.addExact(Math.addExact(shift.openingCash, cashSales), otherCashIn),
            Math.addExact(cashOut, refundAmount),
        )
        return ShiftSummary(
            shift = shift,
            totalSales = totalSales,
            cashSales = cashSales,
            nonCashSales = nonCashSales,
            otherCashIn = otherCashIn,
            cashOut = cashOut,
            refundAmount = refundAmount,
            expectedCash = expectedCash,
        )
    }

    private fun requireOwnerForShift() {
        ownerSession?.requireOwner()
    }

    private companion object {
        val OTHER_CASH_IN_TYPES = setOf("CASH_IN", "RECEIVABLE_IN")
        val CASH_OUT_TYPES = setOf(
            "PURCHASE_OUT",
            "CASH_OUT",
            "EXPENSE",
            "PAYABLE_OUT",
            "WAGE_OUT",
        )
        const val REFUND_CASH_OUT_TYPE = "REFUND_OUT"
    }
}

private fun <T> Iterable<T>.sumAmounts(selector: (T) -> Long): Long = fold(0L) { total, item ->
    Math.addExact(total, selector(item))
}
