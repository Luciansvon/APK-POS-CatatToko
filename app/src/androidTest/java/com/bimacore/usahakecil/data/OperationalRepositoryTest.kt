package com.bimacore.usahakecil.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
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
}
