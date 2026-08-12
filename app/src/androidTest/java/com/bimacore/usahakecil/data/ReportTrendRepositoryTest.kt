package com.bimacore.usahakecil.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.bimacore.usahakecil.security.ReportSession
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ReportTrendRepositoryTest {
    private lateinit var database: PosDatabase

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext<Context>(),
            PosDatabase::class.java,
        ).allowMainThreadQueries().build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun empty_trend_keeps_all_zero_buckets_for_monitoring() = runBlocking {
        val repository = ReportRepository(database, ReportSession().also { it.unlock() })

        val trend = repository.readTrend(
            ReportChartGranularity.DAILY,
            now = 1_754_147_100_000L,
        )

        assertEquals(14, trend.points.size)
        assertTrue(trend.points.all { point ->
            point.sales == 0L &&
                point.transactionCount == 0 &&
                point.quantity == 0L &&
                point.cashIn == 0L &&
                point.cashOut == 0L &&
                point.netCash == 0L
        })
        assertTrue(trend.products.isEmpty())
    }

    @Test
    fun trend_can_follow_the_selected_report_period_instead_of_a_fixed_history() = runBlocking {
        val repository = ReportRepository(database, ReportSession().also { it.unlock() })
        val mondayStart = 1_754_262_400_000L
        val wednesdayNoon = mondayStart + (2L * 86_400_000L) + (12L * 3_600_000L)

        val trend = repository.readTrend(
            granularity = ReportChartGranularity.DAILY,
            now = wednesdayNoon,
            fromInclusive = mondayStart,
        )

        assertEquals(3, trend.points.size)
        assertEquals(mondayStart, trend.fromInclusive)
        assertEquals(wednesdayNoon, trend.toInclusive)
    }

    @Test
    fun active_product_without_sales_stays_available_with_zero_performance() = runBlocking {
        database.catalogDao().insertProduct(
            ProductEntity(
                id = 7L,
                categoryId = 1L,
                name = "Dimsum Mentai",
                basePrice = 15_000L,
                stock = 10,
                stockTrackingEnabled = true,
                hasVariants = false,
                lowStockThreshold = 2,
                imageUri = null,
                sortOrder = 1,
                unitLabel = "porsi",
            ),
        )
        val repository = ReportRepository(database, ReportSession().also { it.unlock() })

        val trend = repository.readTrend(
            ReportChartGranularity.DAILY,
            now = 1_754_147_100_000L,
        )

        val product = trend.products.single { it.productId == 7L }
        assertEquals("Dimsum Mentai", product.productName)
        assertEquals("porsi", product.unitLabel)
        assertTrue(product.points.all { it.sales == 0L && it.quantity == 0L })
    }

    @Test
    fun trend_groups_by_variant_and_supports_more_than_5_products() = runBlocking {
        val now = 1_754_147_100_000L
        val saleId = database.saleDao().insertSale(
            SaleEntity(
                receiptNumber = "INV-001",
                businessName = "Toko Retail",
                total = 60_000,
                amountReceived = 60_000,
                changeAmount = 0,
                paymentMethod = "CASH",
                orderStatus = "COMPLETED",
                createdAt = now,
                updatedAt = now,
            ),
        )
        for (i in 1..6) {
            database.saleDao().insertItems(
                listOf(
                    SaleItemEntity(
                        saleId = saleId,
                        productId = i.toLong(),
                        variantId = if (i == 1) 10L else null,
                        productName = "Produk $i",
                        variantName = if (i == 1) "Varian A" else null,
                        categoryName = "Kategori",
                        unitPrice = 10_000,
                        quantity = 1,
                        subtotal = 10_000,
                        baseQuantity = 1,
                        unitLabel = "pcs",
                    ),
                ),
            )
        }

        val repository = ReportRepository(database, ReportSession().also { it.unlock() })
        val trend = repository.readTrend(ReportChartGranularity.DAILY, now = now)

        assertEquals(6, trend.products.size)
        val variantProduct = trend.products.firstOrNull { it.productId == 1L }
        assertTrue(variantProduct != null)
        assertEquals("Produk 1 (Varian A)", variantProduct?.productName)
        assertEquals(10L, variantProduct?.variantId)
        assertEquals("Varian A", variantProduct?.variantName)
    }
}
