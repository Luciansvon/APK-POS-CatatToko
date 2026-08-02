package com.bimacore.usahakecil.report

import android.content.Context
import android.os.Environment
import androidx.room.Room
import androidx.room.withTransaction
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.bimacore.usahakecil.data.BusinessProfileEntity
import com.bimacore.usahakecil.data.CashEntryEntity
import com.bimacore.usahakecil.data.PosDatabase
import com.bimacore.usahakecil.data.ProductEntity
import com.bimacore.usahakecil.data.ReportPeriod
import com.bimacore.usahakecil.data.SaleEntity
import com.bimacore.usahakecil.data.SaleItemEntity
import com.bimacore.usahakecil.data.SeedCatalog
import com.bimacore.usahakecil.domain.BusinessType
import com.bimacore.usahakecil.domain.PaymentMethod
import com.bimacore.usahakecil.export.ExcelExportManager
import com.bimacore.usahakecil.security.ReportSession
import java.io.File
import java.util.Calendar
import java.util.UUID
import kotlin.math.roundToInt
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AnnualSalesExportTest {
    private lateinit var context: Context
    private lateinit var database: PosDatabase

    @Before
    fun openDatabase() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, PosDatabase::class.java).build()
    }

    @After
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun creates_realistic_retail_sales_and_exports_year_report_from_apk() = runBlocking {
        val now = System.currentTimeMillis()
        val year = Calendar.getInstance().apply { timeInMillis = now }.get(Calendar.YEAR)
        val products = seedCatalog(now)
        val generated = generateSales(products, now, year)

        val session = ReportSession().also { it.unlock() }
        val exportManager = ExcelExportManager(
            context = context,
            database = database,
            ownerSession = session,
            businessType = BusinessType.RETAIL.name,
        )
        val uri = exportManager.createExport(ReportPeriod.YEAR)
        val outputDirectory = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
            ?: context.filesDir
        val output = File(outputDirectory, "catattoko-demo-penjualan-tahun-$year.xlsx")
        context.contentResolver.openInputStream(uri)!!.use { input ->
            output.outputStream().use { fileOutput -> input.copyTo(fileOutput) }
        }

        assertTrue(generated.transactionCount >= 800)
        assertTrue(generated.productIds.size >= 9)
        assertTrue(generated.monthlySales.distinct().size >= 5)
        assertTrue(output.exists())
        assertTrue(output.length() > 20_000L)
        println(
            "ANNUAL_DEMO_OUTPUT=${output.absolutePath} " +
                "TRANSACTIONS=${generated.transactionCount} " +
                "ITEMS=${generated.itemCount} " +
                "SALES=${generated.totalSales}",
        )
    }

    private suspend fun seedCatalog(now: Long): Map<Long, ProductEntity> {
        val seed = SeedCatalog.forBusiness(BusinessType.RETAIL)
        database.withTransaction {
            database.catalogDao().insertCategories(seed.categories)
            database.catalogDao().insertProducts(seed.products)
            database.catalogDao().insertVariants(seed.variants)
            database.profileDao().saveProfile(
                BusinessProfileEntity(
                    businessUid = UUID.nameUUIDFromBytes("annual-retail-demo".toByteArray()).toString(),
                    businessName = "Toko Harian Demo",
                    businessType = BusinessType.RETAIL.name,
                    createdAt = now,
                    updatedAt = now,
                ),
            )
        }
        return seed.products.associateBy { it.id }
    }

    private suspend fun generateSales(
        products: Map<Long, ProductEntity>,
        now: Long,
        year: Int,
    ): GeneratedStats {
        val range = ReportPeriod.YEAR.range(now)
        val day = Calendar.getInstance().apply {
            timeInMillis = range.first
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        var dayIndex = 0
        var transactionCount = 0
        var itemCount = 0
        var totalSales = 0L
        val productIds = mutableSetOf<Long>()
        val monthlySales = mutableListOf<Long>()

        database.withTransaction {
            while (day.timeInMillis <= range.last) {
                val month = day.get(Calendar.MONTH)
                val dayOfWeek = day.get(Calendar.DAY_OF_WEEK)
                val dayOfMonth = day.get(Calendar.DAY_OF_MONTH)
                val monthMultiplier = MONTH_MULTIPLIERS[month]
                val weekMultiplier = if (
                    dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY
                ) {
                    1.30
                } else if (dayOfWeek == Calendar.MONDAY) {
                    0.78
                } else {
                    1.0
                }
                val paydayMultiplier = if (dayOfMonth <= 3 || dayOfMonth >= 25) 1.16 else 1.0
                val openMultiplier = if (dayOfMonth % 19 == 0) 0.56 else 1.0
                val baseTransactions = if (
                    dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY
                ) {
                    8.0
                } else {
                    5.0
                }
                val transactions = (
                    baseTransactions * monthMultiplier * weekMultiplier * paydayMultiplier * openMultiplier +
                        ((dayIndex * 7) % 3 - 1)
                    ).roundToInt().coerceIn(2, 14)
                var dailySales = 0L

                repeat(transactions) { transactionIndex ->
                    val createdAt = day.timeInMillis + transactionTime(dayIndex, transactionIndex)
                    val basket = basketFor(
                        month = month,
                        dayOfWeek = dayOfWeek,
                        dayIndex = dayIndex,
                        transactionIndex = transactionIndex,
                        products = products,
                    )
                    val lines = basket.map { line ->
                        val product = products.getValue(line.productId)
                        val unitPrice = priceFor(product, month)
                        SaleLine(
                            product = product,
                            quantity = line.quantity,
                            unitPrice = unitPrice,
                            subtotal = unitPrice * line.quantity,
                        )
                    }
                    val total = lines.sumOf { it.subtotal }
                    val payment = paymentFor(dayIndex, transactionIndex)
                    val received = receivedFor(payment, total, dayIndex, transactionIndex)
                    val receipt = "ANNUAL-DEMO-$year-" +
                        dayIndex.toString().padStart(3, '0') + "-" +
                        transactionIndex.toString().padStart(2, '0')
                    val saleId = database.saleDao().insertSale(
                        SaleEntity(
                            receiptNumber = receipt,
                            businessName = "Toko Harian Demo",
                            createdAt = createdAt,
                            paymentMethod = payment.name,
                            total = total,
                            amountReceived = received,
                            changeAmount = if (payment == PaymentMethod.CASH) received - total else 0,
                            settlementStatus = settlementStatus(payment, received, total),
                            orderStatus = "COMPLETED",
                            note = noteFor(month, dayOfWeek),
                            updatedAt = createdAt,
                        ),
                    )
                    database.saleDao().insertItems(
                        lines.map { line ->
                            SaleItemEntity(
                                saleId = saleId,
                                productId = line.product.id,
                                variantId = null,
                                productName = line.product.name,
                                variantName = null,
                                categoryName = categoryName(line.product.categoryId),
                                unitPrice = line.unitPrice,
                                quantity = line.quantity,
                                subtotal = line.subtotal,
                                baseQuantity = line.quantity,
                                unitLabel = line.product.unitLabel,
                            )
                        },
                    )
                    if (received > 0) {
                        database.operationsDao().insertCashEntry(
                            CashEntryEntity(
                                type = if (payment == PaymentMethod.CREDIT) "RECEIVABLE_IN" else "SALE_IN",
                                amount = received,
                                category = "Penjualan",
                                note = "Penjualan demo $receipt",
                                paymentMethod = payment.name,
                                referenceType = "SALE",
                                referenceId = saleId,
                                createdAt = createdAt,
                            ),
                        )
                    }
                    transactionCount++
                    itemCount += lines.size
                    totalSales += total
                    dailySales += total
                    lines.forEach { productIds += it.product.id }
                }
                monthlySales += dailySales
                day.add(Calendar.DAY_OF_MONTH, 1)
                dayIndex++
            }
        }
        return GeneratedStats(transactionCount, itemCount, totalSales, productIds, monthlySales)
    }

    private fun basketFor(
        month: Int,
        dayOfWeek: Int,
        dayIndex: Int,
        transactionIndex: Int,
        products: Map<Long, ProductEntity>,
    ): List<BasketLine> {
        val selector = (dayIndex * 17 + transactionIndex * 31 + month * 13) % 100
        val schoolSeason = month in 5..7
        val weekend = dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY
        return when {
            schoolSeason && selector < 22 -> listOf(
                BasketLine(109, 1 + (transactionIndex % 2)),
                BasketLine(110, 1 + ((dayIndex + transactionIndex) % 3)),
                BasketLine(103, 1),
            )
            month in 10..11 && selector < 15 -> listOf(
                BasketLine(105, 1),
                BasketLine(if (selector % 2 == 0) 106 else 101, 1),
            )
            selector < 42 -> listOf(
                BasketLine(103, 1 + ((dayIndex + transactionIndex) % 3)),
                BasketLine(if (weekend) 101 else 102, 1 + (transactionIndex % 2)),
            )
            selector < 68 -> listOf(
                BasketLine(103, 1),
                BasketLine(104, 1),
                BasketLine(101, if (weekend) 2 else 1),
            )
            selector < 86 -> listOf(
                BasketLine(107, 1),
                BasketLine(108, 2 + ((dayIndex + transactionIndex) % 4)),
                BasketLine(103, 1),
            )
            selector < 95 -> listOf(
                BasketLine(109, 1),
                BasketLine(110, 1 + (transactionIndex % 2)),
            )
            else -> listOf(
                BasketLine(if (selector % 2 == 0) 105 else 106, 1),
                BasketLine(if (dayIndex % 2 == 0) 101 else 103, 1),
            )
        }.filter { products.containsKey(it.productId) }
    }

    private fun categoryName(categoryId: Long): String = when (categoryId) {
        2L -> "Makanan"
        3L -> "Minuman"
        4L -> "Pakaian"
        5L -> "Perawatan"
        6L -> "Alat Tulis"
        else -> "Lainnya"
    }

    private fun priceFor(product: ProductEntity, month: Int): Long = when {
        month >= 8 && product.id in setOf(101L, 102L, 103L, 104L, 107L, 108L) -> product.basePrice + 500L
        month >= 6 && product.id in setOf(105L, 106L) -> product.basePrice + 5_000L
        else -> product.basePrice
    }

    private fun paymentFor(dayIndex: Int, transactionIndex: Int): PaymentMethod {
        val score = (dayIndex * 13 + transactionIndex * 7) % 100
        return when {
            score < 58 -> PaymentMethod.CASH
            score < 84 -> PaymentMethod.QRIS
            score < 95 -> PaymentMethod.TRANSFER
            else -> PaymentMethod.CREDIT
        }
    }

    private fun receivedFor(
        payment: PaymentMethod,
        total: Long,
        dayIndex: Int,
        transactionIndex: Int,
    ): Long = when (payment) {
        PaymentMethod.CASH -> ((total + 4_999L) / 5_000L) * 5_000L
        PaymentMethod.QRIS,
        PaymentMethod.TRANSFER,
        -> total
        PaymentMethod.CREDIT -> if ((dayIndex + transactionIndex) % 3 == 0) total / 2 else 0L
    }

    private fun settlementStatus(payment: PaymentMethod, received: Long, total: Long): String = when {
        payment != PaymentMethod.CREDIT -> "PAID"
        received == 0L -> "UNPAID"
        received < total -> "PARTIAL"
        else -> "PAID"
    }

    private fun noteFor(month: Int, dayOfWeek: Int): String = when {
        month in 5..7 -> "Pola musim sekolah"
        dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY -> "Pola akhir pekan"
        else -> "Pola hari kerja"
    }

    private fun transactionTime(dayIndex: Int, transactionIndex: Int): Long {
        val hour = 8 + ((dayIndex * 3 + transactionIndex * 2) % 12)
        val minute = (dayIndex * 11 + transactionIndex * 17) % 60
        return (hour * 60L + minute) * 60_000L
    }

    private data class BasketLine(
        val productId: Long,
        val quantity: Int,
    )

    private data class SaleLine(
        val product: ProductEntity,
        val quantity: Int,
        val unitPrice: Long,
        val subtotal: Long,
    )

    private data class GeneratedStats(
        val transactionCount: Int,
        val itemCount: Int,
        val totalSales: Long,
        val productIds: Set<Long>,
        val monthlySales: List<Long>,
    )

    private companion object {
        val MONTH_MULTIPLIERS = listOf(
            0.86, 0.90, 0.96, 1.00, 1.04, 1.08,
            1.23, 1.14, 0.98, 1.02, 1.10, 1.28,
        )
    }
}
