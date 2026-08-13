package com.bimacore.usahakecil.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.bimacore.usahakecil.domain.AddToCartResult
import com.bimacore.usahakecil.domain.BarcodeLookupResult
import com.bimacore.usahakecil.domain.BusinessCapabilities
import com.bimacore.usahakecil.domain.BusinessType
import com.bimacore.usahakecil.security.ReportSession
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BarcodeRepositoryTest {
    private lateinit var database: PosDatabase
    private lateinit var ownerSession: ReportSession

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, PosDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        ownerSession = ReportSession().apply { unlock() }
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun barcode_maps_exact_variant_and_wholesale_unit_into_existing_cart() = runBlocking {
        val capabilities = BusinessCapabilities.forType(BusinessType.WHOLESALE)
        val inventory = InventoryRepository(database, capabilities, ownerSession)
        val barcodeRepository = BarcodeRepository(database, capabilities, ownerSession)
        val categoryId = inventory.saveCategory(CategoryDraft(name = "Pakaian"))
        val productId = inventory.saveProduct(
            ProductDraft(
                categoryId = categoryId,
                name = "Kaos",
                basePrice = 10_000,
                openingStock = 0,
                stockTrackingEnabled = true,
                lowStockThreshold = 2,
                unitLabel = "pcs",
            ),
        )
        val variantId = inventory.saveVariant(
            VariantDraft(
                productId = productId,
                label = "L",
                priceOverride = null,
                openingStock = 24,
            ),
        )
        val unitId = inventory.saveUnit(null, productId, "dus", 12, 100_000)
        barcodeRepository.save(
            ProductBarcodeDraft(
                barcode = "0012AbC",
                productId = productId,
                variantId = variantId,
                unitId = unitId,
            ),
        )

        val found = barcodeRepository.lookup(" 0012AbC ") as BarcodeLookupResult.Found
        assertEquals("0012AbC", found.target.barcode)
        assertEquals(variantId, found.target.variantId)
        assertEquals(unitId, found.target.unitId)

        val pos = PosRepository(
            database = database,
            businessType = BusinessType.WHOLESALE,
            businessName = "Grosir Test",
            ownerSession = ownerSession,
            barcodeRepository = barcodeRepository,
        )
        pos.seedIfNeeded()
        assertEquals(
            AddToCartResult.Added,
            pos.addProduct(found.target.product.id, found.target.variantId, found.target.unitId),
        )
        val item = pos.snapshot.first().cartItems.single()
        assertEquals("dus", item.unitLabel)
        assertEquals(12, item.factorToBase)
        assertEquals(variantId, item.variantId)
    }

    @Test
    fun duplicate_and_cross_product_targets_are_rejected() = runBlocking {
        val capabilities = BusinessCapabilities.forType(BusinessType.WHOLESALE)
        val inventory = InventoryRepository(database, capabilities, ownerSession)
        val barcodeRepository = BarcodeRepository(database, capabilities, ownerSession)
        val categoryId = inventory.saveCategory(CategoryDraft(name = "Barang"))
        val first = inventory.saveProduct(productDraft(categoryId, "Produk A"))
        val second = inventory.saveProduct(productDraft(categoryId, "Produk B"))
        val secondVariant = inventory.saveVariant(
            VariantDraft(null, second, "Besar", null, 10),
        )
        val secondUnit = inventory.saveUnit(null, second, "pak", 6, 50_000)
        barcodeRepository.save(ProductBarcodeDraft(barcode = "ABC", productId = first))

        assertTrue(
            runCatching {
                barcodeRepository.save(ProductBarcodeDraft(barcode = "ABC", productId = second))
            }.exceptionOrNull()?.message.orEmpty().contains("sudah terdaftar"),
        )
        assertTrue(
            runCatching {
                barcodeRepository.save(
                    ProductBarcodeDraft(
                        barcode = "VARIANT-SALAH",
                        productId = first,
                        variantId = secondVariant,
                    ),
                )
            }.isFailure,
        )
        assertTrue(
            runCatching {
                barcodeRepository.save(
                    ProductBarcodeDraft(
                        barcode = "UNIT-SALAH",
                        productId = first,
                        unitId = secondUnit,
                    ),
                )
            }.isFailure,
        )
    }

    @Test
    fun management_is_owner_only_and_inactive_mapping_is_distinct_from_unknown() = runBlocking {
        val capabilities = BusinessCapabilities.forType(BusinessType.RETAIL)
        val inventory = InventoryRepository(database, capabilities, ownerSession)
        val barcodeRepository = BarcodeRepository(database, capabilities, ownerSession)
        val categoryId = inventory.saveCategory(CategoryDraft(name = "Barang"))
        val productId = inventory.saveProduct(productDraft(categoryId, "Sabun"))
        val mappingId = barcodeRepository.save(
            ProductBarcodeDraft(barcode = "899000", productId = productId),
        )
        barcodeRepository.setActive(mappingId, false)

        assertEquals(BarcodeLookupResult.Inactive, barcodeRepository.lookup("899000"))
        assertEquals(BarcodeLookupResult.NotFound, barcodeRepository.lookup("899999"))

        ownerSession.lock()
        assertTrue(barcodeRepository.mappings.first().isEmpty())
        assertTrue(
            runCatching {
                barcodeRepository.save(
                    ProductBarcodeDraft(barcode = "LOCKED", productId = productId),
                )
            }.exceptionOrNull()?.message.orEmpty().contains("Sesi Owner"),
        )
    }

    @Test
    fun culinary_keeps_shared_schema_but_rejects_barcode_feature() = runBlocking {
        val capabilities = BusinessCapabilities.forType(BusinessType.CULINARY)
        val inventory = InventoryRepository(database, capabilities, ownerSession)
        val repository = BarcodeRepository(database, capabilities, ownerSession)
        val categoryId = inventory.saveCategory(CategoryDraft(name = "Menu"))
        val productId = inventory.saveProduct(productDraft(categoryId, "Nasi"))

        assertEquals(BarcodeLookupResult.Unsupported, repository.lookup("123"))
        assertTrue(
            runCatching {
                repository.save(ProductBarcodeDraft(barcode = "123", productId = productId))
            }.isFailure,
        )
    }

    @Test
    fun barcode_write_waits_for_application_database_coordinator() = runBlocking {
        val capabilities = BusinessCapabilities.forType(BusinessType.RETAIL)
        val inventory = InventoryRepository(database, capabilities, ownerSession)
        val coordinator = DatabaseOperationCoordinator()
        val repository = BarcodeRepository(
            database = database,
            capabilities = capabilities,
            ownerSession = ownerSession,
            databaseOperations = coordinator,
        )
        val categoryId = inventory.saveCategory(CategoryDraft(name = "Barang"))
        val productId = inventory.saveProduct(productDraft(categoryId, "Minyak"))
        val lockEntered = CompletableDeferred<Unit>()
        val releaseLock = CompletableDeferred<Unit>()
        val blockingOperation = async {
            coordinator.withOperation {
                lockEntered.complete(Unit)
                releaseLock.await()
            }
        }
        lockEntered.await()

        val pendingSave = async {
            repository.save(ProductBarcodeDraft(barcode = "WAIT-1", productId = productId))
        }
        yield()
        assertFalse(pendingSave.isCompleted)

        releaseLock.complete(Unit)
        blockingOperation.await()
        pendingSave.await()
        assertEquals("WAIT-1", database.barcodeDao().getByBarcode("WAIT-1")?.barcode)
    }

    private fun productDraft(categoryId: Long, name: String) = ProductDraft(
        categoryId = categoryId,
        name = name,
        basePrice = 10_000,
        openingStock = 10,
        stockTrackingEnabled = true,
        lowStockThreshold = 2,
        unitLabel = "pcs",
    )
}
