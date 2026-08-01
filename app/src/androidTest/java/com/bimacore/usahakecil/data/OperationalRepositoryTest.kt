package com.bimacore.usahakecil.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.bimacore.usahakecil.domain.AddToCartResult
import com.bimacore.usahakecil.domain.BusinessCapabilities
import com.bimacore.usahakecil.domain.BusinessType
import com.bimacore.usahakecil.domain.CheckoutResult
import com.bimacore.usahakecil.domain.PaymentMethod
import com.bimacore.usahakecil.security.ReportSession
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OperationalRepositoryTest {
    private lateinit var database: PosDatabase

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, PosDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun purchase_updates_stock_cash_and_payable_atomically() = runBlocking {
        val inventory = InventoryRepository(
            database,
            BusinessCapabilities.forType(BusinessType.RETAIL),
        )
        val operations = OperationsRepository(database)
        val categoryId = inventory.saveCategory(CategoryDraft(name = "Barang"))
        val productId = inventory.saveProduct(
            ProductDraft(
                categoryId = categoryId,
                name = "Beras",
                basePrice = 15_000,
                openingStock = 2,
                stockTrackingEnabled = true,
                lowStockThreshold = 1,
                unitLabel = "kg",
            ),
        )
        val supplierId = operations.saveParty(
            null,
            PartyKind.SUPPLIER,
            "Supplier A",
            "",
            "",
        )

        operations.recordPurchase(
            PurchaseDraft(
                supplierId = supplierId,
                amountPaid = 20_000,
                lines = listOf(
                    PurchaseLineDraft(
                        productId = productId,
                        unitLabel = "kg",
                        quantity = 5,
                        unitCost = 10_000,
                    ),
                ),
            ),
        )

        assertEquals(7, database.catalogDao().getProduct(productId)?.stock)
        assertEquals(20_000L, operations.cashEntries.first().single().amount)
        val payable = operations.debts.first().single()
        assertEquals(DebtKind.PAYABLE.name, payable.kind)
        assertEquals(30_000L, payable.originalAmount - payable.paidAmount)
    }

    @Test
    fun shift_open_close_calculates_expected_cash_and_preserves_history() = runBlocking {
        var now = 1_700_000_000_000L
        val operations = OperationsRepository(database, clock = { now })

        val shiftId = operations.openShift("Kasir Pagi", 100_000, "Modal awal")
        database.saleDao().insertSale(
            SaleEntity(
                receiptNumber = "SHIFT-CASH",
                businessName = "Tes Shift",
                createdAt = now + 10,
                paymentMethod = PaymentMethod.CASH.name,
                total = 50_000,
                amountReceived = 50_000,
                changeAmount = 0,
                updatedAt = now + 10,
                shiftId = shiftId,
            ),
        )
        database.operationsDao().insertCashEntry(
            CashEntryEntity(
                type = "CASH_IN",
                amount = 25_000,
                category = "Modal tambahan",
                note = "",
                paymentMethod = PaymentMethod.CASH.name,
                referenceType = null,
                referenceId = null,
                createdAt = now + 20,
                shiftId = shiftId,
            ),
        )
        database.operationsDao().insertCashEntry(
            CashEntryEntity(
                type = "EXPENSE",
                amount = 10_000,
                category = "Parkir",
                note = "",
                paymentMethod = PaymentMethod.CASH.name,
                referenceType = null,
                referenceId = null,
                createdAt = now + 30,
                shiftId = shiftId,
            ),
        )

        now += 100
        val closed = operations.closeShift(160_000, "Selisih dicatat")

        assertEquals(50_000L, closed.cashSales)
        assertEquals(25_000L, closed.otherCashIn)
        assertEquals(10_000L, closed.cashOut)
        assertEquals(165_000L, closed.expectedCash)
        assertEquals(-5_000L, closed.cashDifference)
        assertEquals(ShiftStatus.CLOSED.name, closed.shift.status)
        assertTrue(operations.readOpenShiftSummary() == null)
        assertEquals(1, operations.shifts.first().size)
    }

    @Test
    fun worker_can_open_shift_but_owner_still_controls_shift_finance() = runBlocking {
        val session = ReportSession()
        val operations = OperationsRepository(database, ownerSession = session)

        assertTrue(operations.shifts.first().isEmpty())
        operations.openShift("Kasir", 0, "")
        assertEquals("Kasir", operations.openShift.first()?.cashierName)
        assertTrue(runCatching { operations.readOpenShiftSummary() }.isFailure)
        assertTrue(runCatching { operations.closeShift(0, "") }.isFailure)

        session.unlock()
        assertEquals(1, operations.shifts.first().size)
    }

    @Test
    fun shift_does_not_allow_two_open_shifts() = runBlocking {
        val operations = OperationsRepository(database)
        operations.openShift("Kasir", 0, "")

        assertTrue(runCatching { operations.openShift("Kasir 2", 0, "") }.isFailure)
    }

    @Test
    fun wholesale_unit_deducts_base_stock_and_applies_tier_price() = runBlocking {
        val capabilities = BusinessCapabilities.forType(BusinessType.WHOLESALE)
        val inventory = InventoryRepository(database, capabilities)
        val categoryId = inventory.saveCategory(CategoryDraft(name = "Grosir"))
        val productId = inventory.saveProduct(
            ProductDraft(
                categoryId = categoryId,
                name = "Minuman",
                basePrice = 10_000,
                openingStock = 48,
                stockTrackingEnabled = true,
                lowStockThreshold = 6,
                unitLabel = "pcs",
            ),
        )
        val unitId = inventory.saveUnit(null, productId, "dus", 12, 110_000)
        inventory.savePriceTier(null, productId, 12, 8_000)
        val pos = PosRepository(database, BusinessType.WHOLESALE, "Tes Grosir")
        openTestShift()

        pos.addProduct(productId, unitId = unitId)
        val result = pos.completeSale(
            CheckoutRequest(PaymentMethod.CASH, 100_000, false),
        )

        assertTrue(result is CheckoutResult.Success)
        assertEquals(96_000L, (result as CheckoutResult.Success).receipt.total)
        assertEquals(36, database.catalogDao().getProduct(productId)?.stock)
    }

    @Test
    fun culinary_checkout_snapshots_topping_note_and_consumes_recipe() = runBlocking {
        val capabilities = BusinessCapabilities.forType(BusinessType.CULINARY)
        val inventory = InventoryRepository(database, capabilities)
        val culinary = CulinaryRepository(database, capabilities)
        val categoryId = inventory.saveCategory(CategoryDraft(name = "Menu"))
        val menuId = inventory.saveProduct(
            ProductDraft(
                categoryId = categoryId,
                name = "Nasi",
                basePrice = 20_000,
                openingStock = 0,
                stockTrackingEnabled = false,
                lowStockThreshold = 0,
                unitLabel = "porsi",
            ),
        )
        val ingredientId = inventory.saveProduct(
            ProductDraft(
                categoryId = categoryId,
                name = "Beras",
                basePrice = 0,
                openingStock = 10,
                stockTrackingEnabled = true,
                lowStockThreshold = 2,
                unitLabel = "takaran",
            ),
        )
        culinary.saveRecipeIngredient(menuId, ingredientId, 2)
        val toppingId = culinary.saveTopping(null, menuId, "Telur", 5_000)
        val pos = PosRepository(database, BusinessType.CULINARY, "Tes Kuliner")
        openTestShift()
        pos.addProduct(menuId)
        val line = database.cartDao().getLines().single()
        pos.setCartCustomization(line.id, "Tidak pedas", mapOf(toppingId to 1))

        val result = pos.completeSale(
            CheckoutRequest(PaymentMethod.CASH, 25_000, false),
        )

        assertTrue(result is CheckoutResult.Success)
        val receipt = (result as CheckoutResult.Success).receipt
        assertEquals(25_000L, receipt.total)
        assertEquals("Tidak pedas", receipt.items.single().note)
        assertEquals(listOf("Telur"), receipt.items.single().toppingNames)
        assertEquals(8, database.catalogDao().getProduct(ingredientId)?.stock)
        assertEquals("NEW", database.saleDao().getSale(receipt.saleId)?.orderStatus)
    }

    @Test
    fun report_data_is_blocked_until_pin_unlocks_session() = runBlocking {
        val reports = ReportRepository(database, ReportSession(), clock = { 10L })
        assertTrue(runCatching { reports.readSummary(0, 20) }.isFailure)

        reports.createPin("1234")
        val summary = reports.readSummary(0, 20)

        assertEquals(0, summary.transactionCount)
        reports.lock()
        assertTrue(runCatching { reports.readSummary(0, 20) }.isFailure)
    }

    @Test
    fun changed_report_pin_rejects_old_pin_and_accepts_new_pin() = runBlocking {
        val reports = ReportRepository(database, ReportSession(), clock = { 10L })
        reports.createPin("1234")

        reports.changePin("1234", "5678")
        reports.lock()

        assertTrue(!reports.unlock("1234"))
        assertTrue(reports.unlock("5678"))
        assertEquals(0, reports.readSummary(0, 20).transactionCount)
    }

    @Test
    fun wholesale_forecast_uses_base_quantity_snapshot_from_room_history() = runBlocking {
        val inventory = InventoryRepository(
            database,
            BusinessCapabilities.forType(BusinessType.WHOLESALE),
        )
        val categoryId = inventory.saveCategory(CategoryDraft(name = "Grosir"))
        val productId = inventory.saveProduct(
            ProductDraft(
                categoryId = categoryId,
                name = "Minuman",
                basePrice = 10_000,
                openingStock = 252,
                stockTrackingEnabled = true,
                lowStockThreshold = 12,
                unitLabel = "pcs",
            ),
        )
        val firstSaleAt = 1_700_000_000_000L
        val dayMillis = 86_400_000L
        repeat(21) { day ->
            val createdAt = firstSaleAt + day * dayMillis
            val saleId = database.saleDao().insertSale(
                SaleEntity(
                    receiptNumber = "FORECAST-${day + 1}",
                    businessName = "Tes Grosir",
                    createdAt = createdAt,
                    paymentMethod = PaymentMethod.CASH.name,
                    total = 110_000,
                    amountReceived = 110_000,
                    changeAmount = 0,
                    updatedAt = createdAt,
                ),
            )
            database.saleDao().insertItems(
                listOf(
                    SaleItemEntity(
                        saleId = saleId,
                        productId = productId,
                        variantId = null,
                        productName = "Minuman",
                        variantName = null,
                        categoryName = "Grosir",
                        unitPrice = 110_000,
                        quantity = 1,
                        subtotal = 110_000,
                        baseQuantity = 12,
                        unitLabel = "dus",
                    ),
                ),
            )
        }
        val lastSaleAt = firstSaleAt + 20 * dayMillis
        val reports = ReportRepository(database, ReportSession(), clock = { lastSaleAt })
        reports.createPin("1234")

        val productForecast = reports.readProductForecasts(
            fromInclusive = firstSaleAt,
            toInclusive = lastSaleAt,
        ).products.single { it.productId == productId }
        val result = requireNotNull(productForecast.result)

        assertEquals("pcs", productForecast.unitLabel)
        assertEquals(21, result.normalizedHistoryDays)
        result.forecast.forEach { point ->
            assertEquals(12.0, point.expectedQuantity, 0.000001)
        }
    }

    @Test
    fun product_forecast_is_blocked_while_owner_session_is_locked() = runBlocking {
        val reports = ReportRepository(database, ReportSession(), clock = { 10L })

        val result = runCatching {
            reports.readProductForecasts(fromInclusive = 0, toInclusive = 20)
        }

        assertTrue(result.exceptionOrNull() is ReportLockedException)
    }

    @Test
    fun manual_stock_adjustment_uses_supported_movement_type() = runBlocking {
        val inventory = InventoryRepository(
            database,
            BusinessCapabilities.forType(BusinessType.RETAIL),
        )
        val categoryId = inventory.saveCategory(CategoryDraft(name = "Barang"))
        val productId = inventory.saveProduct(
            ProductDraft(
                categoryId = categoryId,
                name = "Gula",
                basePrice = 10_000,
                openingStock = 2,
                stockTrackingEnabled = true,
                lowStockThreshold = 1,
                unitLabel = "kg",
            ),
        )

        inventory.adjustStock(
            productId = productId,
            variantId = null,
            delta = 3,
            type = "ADJUSTMENT_IN",
            reason = "Stok opname",
        )

        assertEquals(5, database.catalogDao().getProduct(productId)?.stock)
        assertEquals("ADJUSTMENT_IN", inventory.stockMovements.first().first().type)
    }

    @Test
    fun editing_product_preserves_inactive_status_and_stock_history() = runBlocking {
        val inventory = InventoryRepository(
            database,
            BusinessCapabilities.forType(BusinessType.RETAIL),
        )
        val categoryId = inventory.saveCategory(CategoryDraft(name = "Barang"))
        val productId = inventory.saveProduct(
            ProductDraft(
                categoryId = categoryId,
                name = "Produk lama",
                basePrice = 10_000,
                openingStock = 7,
                stockTrackingEnabled = true,
                lowStockThreshold = 1,
                unitLabel = "pcs",
            ),
        )
        inventory.setProductActive(productId, false)

        inventory.saveProduct(
            ProductDraft(
                id = productId,
                categoryId = categoryId,
                name = "Produk baru",
                basePrice = 12_000,
                openingStock = 999,
                stockTrackingEnabled = true,
                lowStockThreshold = 2,
                unitLabel = "pcs",
            ),
        )

        val edited = requireNotNull(database.catalogDao().getProduct(productId))
        assertEquals("Produk baru", edited.name)
        assertEquals(7, edited.stock)
        assertTrue(!edited.isActive)
    }

    // ---- ERR-016: Stok produk bervarian ----

    @Test
    fun purchase_variant_updates_variant_stock_not_parent() = runBlocking {
        val inventory = InventoryRepository(
            database,
            BusinessCapabilities.forType(BusinessType.RETAIL),
        )
        val operations = OperationsRepository(database)
        val categoryId = inventory.saveCategory(CategoryDraft(name = "Barang"))
        val productId = inventory.saveProduct(
            ProductDraft(
                categoryId = categoryId,
                name = "Kaos",
                basePrice = 50_000,
                openingStock = 0,
                stockTrackingEnabled = true,
                lowStockThreshold = 1,
                unitLabel = "pcs",
            ),
        )
        val variantId = inventory.saveVariant(
            VariantDraft(
                productId = productId,
                label = "Merah L",
                priceOverride = null,
                openingStock = 5,
            ),
        )
        val supplierId = operations.saveParty(
            null,
            PartyKind.SUPPLIER,
            "Supplier Kaos",
            "",
            "",
        )

        operations.recordPurchase(
            PurchaseDraft(
                supplierId = supplierId,
                amountPaid = 30_000,
                lines = listOf(
                    PurchaseLineDraft(
                        productId = productId,
                        variantId = variantId,
                        unitLabel = "pcs",
                        quantity = 3,
                        unitCost = 10_000,
                    ),
                ),
            ),
        )

        val variant = requireNotNull(database.catalogDao().getVariant(variantId))
        assertEquals(8, variant.stock) // 5 awal + 3 pembelian
        val product = requireNotNull(database.catalogDao().getProduct(productId))
        assertEquals(0, product.stock) // parent tidak berubah
    }

    @Test
    fun adjust_stock_variant_product_without_variant_id_is_rejected() = runBlocking {
        val inventory = InventoryRepository(
            database,
            BusinessCapabilities.forType(BusinessType.RETAIL),
        )
        val categoryId = inventory.saveCategory(CategoryDraft(name = "Barang"))
        val productId = inventory.saveProduct(
            ProductDraft(
                categoryId = categoryId,
                name = "Kaos",
                basePrice = 50_000,
                openingStock = 0,
                stockTrackingEnabled = true,
                lowStockThreshold = 1,
                unitLabel = "pcs",
            ),
        )
        inventory.saveVariant(
            VariantDraft(
                productId = productId,
                label = "Hitam M",
                priceOverride = null,
                openingStock = 10,
            ),
        )

        val result = runCatching {
            inventory.adjustStock(
                productId = productId,
                variantId = null,
                delta = 5,
                type = "ADJUSTMENT_IN",
                reason = "Stok opname",
            )
        }

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("varian") == true)
    }

    @Test
    fun adjust_stock_variant_updates_variant_stock() = runBlocking {
        val inventory = InventoryRepository(
            database,
            BusinessCapabilities.forType(BusinessType.RETAIL),
        )
        val categoryId = inventory.saveCategory(CategoryDraft(name = "Barang"))
        val productId = inventory.saveProduct(
            ProductDraft(
                categoryId = categoryId,
                name = "Kaos",
                basePrice = 50_000,
                openingStock = 0,
                stockTrackingEnabled = true,
                lowStockThreshold = 1,
                unitLabel = "pcs",
            ),
        )
        val variantId = inventory.saveVariant(
            VariantDraft(
                productId = productId,
                label = "Putih S",
                priceOverride = null,
                openingStock = 3,
            ),
        )

        inventory.adjustStock(
            productId = productId,
            variantId = variantId,
            delta = 5,
            type = "ADJUSTMENT_IN",
            reason = "Stok opname",
        )

        val variant = requireNotNull(database.catalogDao().getVariant(variantId))
        assertEquals(8, variant.stock) // 3 awal + 5 penyesuaian
        val product = requireNotNull(database.catalogDao().getProduct(productId))
        assertEquals(0, product.stock) // parent tidak berubah
    }

    @Test
    fun purchase_variant_product_without_variant_id_is_rejected() = runBlocking {
        val inventory = InventoryRepository(
            database,
            BusinessCapabilities.forType(BusinessType.RETAIL),
        )
        val operations = OperationsRepository(database)
        val categoryId = inventory.saveCategory(CategoryDraft(name = "Barang"))
        val productId = inventory.saveProduct(
            ProductDraft(
                categoryId = categoryId,
                name = "Kaos",
                basePrice = 50_000,
                openingStock = 0,
                stockTrackingEnabled = true,
                lowStockThreshold = 1,
                unitLabel = "pcs",
            ),
        )
        inventory.saveVariant(
            VariantDraft(
                productId = productId,
                label = "Biru XL",
                priceOverride = null,
                openingStock = 5,
            ),
        )
        val supplierId = operations.saveParty(
            null,
            PartyKind.SUPPLIER,
            "Supplier Kaos",
            "",
            "",
        )

        val result = runCatching {
            operations.recordPurchase(
                PurchaseDraft(
                    supplierId = supplierId,
                    amountPaid = 10_000,
                    lines = listOf(
                        PurchaseLineDraft(
                            productId = productId,
                            unitLabel = "pcs",
                            quantity = 2,
                            unitCost = 10_000,
                        ),
                    ),
                ),
            )
        }

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("varian") == true)
    }

    @Test
    fun stock_adjustment_refuses_negative_result() = runBlocking {
        val inventory = InventoryRepository(
            database,
            BusinessCapabilities.forType(BusinessType.RETAIL),
        )
        val categoryId = inventory.saveCategory(CategoryDraft(name = "Barang"))
        val productId = inventory.saveProduct(
            ProductDraft(
                categoryId = categoryId,
                name = "Gula",
                basePrice = 10_000,
                openingStock = 3,
                stockTrackingEnabled = true,
                lowStockThreshold = 1,
                unitLabel = "kg",
            ),
        )

        val result = runCatching {
            inventory.adjustStock(
                productId = productId,
                variantId = null,
                delta = -5,
                type = "ADJUSTMENT_OUT",
                reason = "Koreksi stok",
            )
        }

        assertTrue(result.isFailure)
    }

    // ---- ERR-017: Inkonsistensi Pembayaran Utang/Piutang dan Kas ----

    @Test
    fun createDebt_with_initial_payment_creates_cash_entry_and_debt_payment() = runBlocking {
        val operations = OperationsRepository(database)
        val supplierId = operations.saveParty(null, PartyKind.SUPPLIER, "Supplier B", "", "")

        val debtId = operations.createDebt(
            kind = DebtKind.PAYABLE,
            partyId = supplierId,
            originalAmount = 100_000,
            initialPayment = 30_000,
            note = "Beli peralatan",
        )

        val debt = requireNotNull(database.operationsDao().getDebt(debtId))
        assertEquals(30_000L, debt.paidAmount)
        assertEquals("PARTIAL", debt.settlementStatus)

        val payments = database.operationsDao().getDebtPayments(debtId)
        assertEquals(1, payments.size)
        assertEquals(30_000L, payments.first().amount)

        val cashEntries = operations.cashEntries.first()
        assertEquals(1, cashEntries.size)
        assertEquals("PAYABLE_OUT", cashEntries.first().type)
        assertEquals(30_000L, cashEntries.first().amount)
    }

    @Test
    fun purchase_with_downpayment_creates_debt_payment_record() = runBlocking {
        val inventory = InventoryRepository(
            database,
            BusinessCapabilities.forType(BusinessType.RETAIL),
        )
        val operations = OperationsRepository(database)
        val categoryId = inventory.saveCategory(CategoryDraft(name = "Barang"))
        val productId = inventory.saveProduct(
            ProductDraft(
                categoryId = categoryId,
                name = "Terigu",
                basePrice = 12_000,
                openingStock = 0,
                stockTrackingEnabled = true,
                lowStockThreshold = 1,
                unitLabel = "kg",
            ),
        )
        val supplierId = operations.saveParty(null, PartyKind.SUPPLIER, "Supplier Terigu", "", "")

        val purchaseId = operations.recordPurchase(
            PurchaseDraft(
                supplierId = supplierId,
                amountPaid = 20_000,
                lines = listOf(
                    PurchaseLineDraft(
                        productId = productId,
                        unitLabel = "kg",
                        quantity = 5,
                        unitCost = 10_000,
                    ),
                ),
            ),
        )

        val debt = operations.debts.first().single()
        assertEquals(20_000L, debt.paidAmount)
        assertEquals(50_000L, debt.originalAmount)

        val debtPayments = database.operationsDao().getDebtPayments(debt.id)
        assertEquals(1, debtPayments.size)
        assertEquals(20_000L, debtPayments.first().amount)
    }

    // ---- ERR-018: Orphan Records pada Notes dan Toppings Keranjang Kuliner ----

    @Test
    fun deleting_cart_line_clears_associated_notes_and_toppings() = runBlocking {
        val capabilities = BusinessCapabilities.forType(BusinessType.CULINARY)
        val inventory = InventoryRepository(database, capabilities)
        val culinary = CulinaryRepository(database, capabilities)
        val categoryId = inventory.saveCategory(CategoryDraft(name = "Menu"))
        val menuId = inventory.saveProduct(
            ProductDraft(
                categoryId = categoryId,
                name = "Mie Goreng",
                basePrice = 15_000,
                openingStock = 0,
                stockTrackingEnabled = false,
                lowStockThreshold = 0,
                unitLabel = "porsi",
            ),
        )
        val toppingId = culinary.saveTopping(null, menuId, "Telur", 3_000)
        val pos = PosRepository(database, BusinessType.CULINARY, "Tes Kuliner")

        pos.addProduct(menuId)
        val line = database.cartDao().getLines().single()
        pos.setCartCustomization(line.id, "Pedas sedang", mapOf(toppingId to 1))

        assertEquals("Pedas sedang", database.culinaryDao().getCartLineNote(line.id)?.note)
        assertEquals(1, database.culinaryDao().getCartLineToppings(line.id).size)

        // Delete line by setting quantity to 0
        pos.setQuantity(line.id, 0)

        assertEquals(0, database.cartDao().getLines().size)
        assertTrue(database.culinaryDao().getCartLineNote(line.id) == null)
        assertEquals(0, database.culinaryDao().getCartLineToppings(line.id).size)
    }

    // ---- ERR-019: Penjualan Produk/Varian Nonaktif ----

    @Test
    fun inactive_product_cannot_be_added_or_checked_out() = runBlocking {
        val inventory = InventoryRepository(
            database,
            BusinessCapabilities.forType(BusinessType.RETAIL),
        )
        val categoryId = inventory.saveCategory(CategoryDraft(name = "Barang"))
        val productId = inventory.saveProduct(
            ProductDraft(
                categoryId = categoryId,
                name = "Kopi Bubuk",
                basePrice = 10_000,
                openingStock = 10,
                stockTrackingEnabled = true,
                lowStockThreshold = 1,
                unitLabel = "pcs",
            ),
        )
        val pos = PosRepository(database, BusinessType.RETAIL, "Tes Retail")
        openTestShift()

        // Deactivate product
        inventory.setProductActive(productId, false)

        val addResult = pos.addProduct(productId)
        assertEquals(AddToCartResult.OutOfStock, addResult)

        // Force add to cart line to test completeSale validation
        database.cartDao().upsertLine(
            CartLineEntity(
                id = "$productId:0:0",
                productId = productId,
                variantId = null,
                quantity = 1,
                updatedAt = 10L,
            ),
        )

        val checkoutResult = pos.completeSale(CheckoutRequest(PaymentMethod.CASH, 10_000, false))
        assertTrue(checkoutResult is CheckoutResult.Error)
        assertTrue((checkoutResult as CheckoutResult.Error).message.contains("tidak aktif"))
    }

    private suspend fun openTestShift() {
        database.shiftDao().insertShift(
            ShiftEntity(
                cashierName = "Kasir Test",
                openedAt = 0,
                openingCash = 0,
                openSlot = 1,
            ),
        )
    }
}
