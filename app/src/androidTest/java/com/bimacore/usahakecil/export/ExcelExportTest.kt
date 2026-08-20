package com.bimacore.usahakecil.export

import android.content.Context
import androidx.room.Room
import androidx.room.withTransaction
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.bimacore.usahakecil.data.BusinessProfileEntity
import com.bimacore.usahakecil.data.CategoryEntity
import com.bimacore.usahakecil.data.MIGRATION_1_2
import com.bimacore.usahakecil.data.MIGRATION_2_3
import com.bimacore.usahakecil.data.MIGRATION_3_4
import com.bimacore.usahakecil.data.MIGRATION_4_5
import com.bimacore.usahakecil.data.MIGRATION_5_6
import com.bimacore.usahakecil.data.PosDatabase
import com.bimacore.usahakecil.data.ProductBarcodeEntity
import com.bimacore.usahakecil.data.ProductEntity
import com.bimacore.usahakecil.data.ReportPeriod
import com.bimacore.usahakecil.security.ReportSession
import java.util.zip.ZipInputStream
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ExcelExportTest {
    private lateinit var context: Context
    private lateinit var database: PosDatabase
    private val databaseName = "excel-export-test.db"

    @Before
    fun setUp() {
        runBlocking {
            context = ApplicationProvider.getApplicationContext()
            context.deleteDatabase(databaseName)
            database = Room.databaseBuilder(context, PosDatabase::class.java, databaseName)
                .addMigrations(
                    MIGRATION_1_2,
                    MIGRATION_2_3,
                    MIGRATION_3_4,
                    MIGRATION_4_5,
                    MIGRATION_5_6,
                )
                .build()
            database.profileDao().saveProfile(
                BusinessProfileEntity(
                    businessUid = "excel-test",
                    businessName = "Kopi & Roti",
                    businessType = "RETAIL",
                    createdAt = 1,
                    updatedAt = 1,
                ),
            )
            database.catalogDao().insertCategory(CategoryEntity(1, "Minuman", "inventory", 1))
            database.catalogDao().insertProduct(
                ProductEntity(
                    id = 1,
                    categoryId = 1,
                    name = "Kopi <Susu>",
                    basePrice = 15_000,
                    stock = 4,
                    stockTrackingEnabled = true,
                    hasVariants = false,
                    lowStockThreshold = 1,
                    imageUri = null,
                    sortOrder = 1,
                ),
            )
            database.barcodeDao().insert(
                ProductBarcodeEntity(
                    barcode = "00123AbC",
                    productId = 1,
                    createdAt = 1,
                    updatedAt = 1,
                ),
            )
        }
    }

    @After
    fun tearDown() {
        database.close()
        context.deleteDatabase(databaseName)
    }

    @Test
    fun export_is_owner_only_and_contains_operational_data() = runBlocking {
        val session = ReportSession()
        val manager = ExcelExportManager(
            context = context,
            database = database,
            ownerSession = session,
            businessType = "RETAIL",
            clock = { 123L },
        )

        val lockedFailure = runCatching { manager.createExport() }.exceptionOrNull()
        assertTrue(lockedFailure?.message.orEmpty().contains("Sesi Owner"))

        session.unlock()
        val uri = manager.createExport()
        val entries = linkedMapOf<String, String>()
        context.contentResolver.openInputStream(uri)!!.use { input ->
            ZipInputStream(input).use { zip ->
                while (true) {
                    val entry = zip.nextEntry ?: break
                    entries[entry.name] = zip.readBytes().toString(Charsets.UTF_8)
                }
            }
        }

        assertTrue(entries.containsKey("xl/workbook.xml"))
        assertTrue(
            Regex("<sheet ").findAll(entries.getValue("xl/workbook.xml")).count() == 21,
        )
        assertTrue(entries.getValue("xl/workbook.xml").contains("Info Laporan"))
        assertTrue(entries.getValue("xl/workbook.xml").contains("Ringkasan"))
        assertTrue(entries.values.any { it.contains("Laporan Penjualan -") })
        assertTrue(entries.getValue("xl/workbook.xml").contains("Produk"))
        assertTrue(entries.getValue("xl/workbook.xml").contains("Barcode Produk"))
        assertTrue(entries.values.any { it.contains("00123AbC") })
        assertTrue(entries.values.any { it.contains("Kopi &lt;Susu&gt;") })
        assertTrue(entries.values.any { it.contains("01 Januari 1970") })
        assertTrue(entries.values.any { it.contains("Ringkasan Keuangan") })
        assertTrue(entries.values.none { it.contains("report_security") || it.contains("draft_cart") })
    }

    @Test
    fun summary_export_has_one_summary_sheet_with_main_values_and_print_fit() = runBlocking {
        insertSale("SUMMARY-1", System.currentTimeMillis())
        val manager = ExcelExportManager(
            context = context,
            database = database,
            ownerSession = ReportSession().also { it.unlock() },
            businessType = "RETAIL",
            clock = { System.currentTimeMillis() },
        )

        val entries = readEntries(manager.createExport(ReportPeriod.DAY, ExcelExportMode.SUMMARY))
        val workbook = entries.getValue("xl/workbook.xml")
        val sheet = entries.getValue("xl/worksheets/sheet1.xml")

        assertTrue(Regex("<sheet ").findAll(workbook).count() == 1)
        assertTrue(workbook.contains("name=\"Ringkasan\""))
        assertFalse(workbook.contains("Info Laporan"))
        assertTrue(sheet.contains("Ringkasan Keuangan"))
        assertTrue(sheet.contains("Jumlah transaksi"))
        assertTrue(sheet.contains("Penjualan"))
        assertTrue(sheet.contains("Kas masuk"))
        assertTrue(sheet.contains("fitToWidth=\"1\""))
        assertTrue(sheet.contains("fitToHeight=\"1\""))
    }

    @Test
    fun export_handles_five_hundred_orders() = runBlocking {
        database.withTransaction {
            repeat(500) { index ->
                val saleId = database.saleDao().insertSale(
                    com.bimacore.usahakecil.data.SaleEntity(
                        receiptNumber = "QA-${index.toString().padStart(4, '0')}",
                        businessName = "Kopi & Roti",
                        createdAt = 1_000L + index,
                        paymentMethod = "CASH",
                        total = 15_000,
                        amountReceived = 15_000,
                        changeAmount = 0,
                    ),
                )
                database.saleDao().insertItems(
                    listOf(
                        com.bimacore.usahakecil.data.SaleItemEntity(
                            saleId = saleId,
                            productId = 1,
                            variantId = null,
                            productName = "Kopi <Susu>",
                            variantName = null,
                            categoryName = "Minuman",
                            unitPrice = 15_000,
                            quantity = 1,
                            subtotal = 15_000,
                        ),
                    ),
                )
            }
        }

        val session = ReportSession().also { it.unlock() }
        val manager = ExcelExportManager(
            context = context,
            database = database,
            ownerSession = session,
            businessType = "RETAIL",
            clock = { 123L },
        )
        val entries = readEntries(manager.createExport())
        val salesSheet = entries.values.first { it.contains("QA-0000") }

        assertTrue(salesSheet.contains("QA-0499"))
        assertTrue(Regex("<row r=").findAll(salesSheet).count() >= 501)
    }

    @Test
    fun period_export_contains_only_events_in_selected_period() = runBlocking {
        val exportedAt = System.currentTimeMillis()
        val todayStart = ReportPeriod.DAY.range(exportedAt).first
        insertSale("PERIOD-TODAY", exportedAt - 60_000L)
        insertSale("PERIOD-YESTERDAY", todayStart - 60_000L)

        val session = ReportSession().also { it.unlock() }
        val manager = ExcelExportManager(
            context = context,
            database = database,
            ownerSession = session,
            businessType = "RETAIL",
            clock = { exportedAt },
        )
        val entries = readEntries(manager.createExport(ReportPeriod.DAY))
        val salesSheet = entries.values.first { it.contains("Laporan Penjualan -") }

        assertTrue(salesSheet.contains("PERIOD-TODAY"))
        assertFalse(salesSheet.contains("PERIOD-YESTERDAY"))
        assertTrue(entries.values.any { it.contains("Periode") && it.contains("Hari ini") })
    }

    private suspend fun insertSale(receiptNumber: String, createdAt: Long) {
        val saleId = database.saleDao().insertSale(
            com.bimacore.usahakecil.data.SaleEntity(
                receiptNumber = receiptNumber,
                businessName = "Kopi & Roti",
                createdAt = createdAt,
                paymentMethod = "CASH",
                total = 15_000,
                amountReceived = 15_000,
                changeAmount = 0,
            ),
        )
        database.saleDao().insertItems(
            listOf(
                com.bimacore.usahakecil.data.SaleItemEntity(
                    saleId = saleId,
                    productId = 1,
                    variantId = null,
                    productName = "Kopi <Susu>",
                    variantName = null,
                    categoryName = "Minuman",
                    unitPrice = 15_000,
                    quantity = 1,
                    subtotal = 15_000,
                ),
            ),
        )
    }

    private suspend fun readEntries(uri: android.net.Uri): Map<String, String> {
        val entries = linkedMapOf<String, String>()
        context.contentResolver.openInputStream(uri)!!.use { input ->
            ZipInputStream(input).use { zip ->
                while (true) {
                    val entry = zip.nextEntry ?: break
                    entries[entry.name] = zip.readBytes().toString(Charsets.UTF_8)
                }
            }
        }
        return entries
    }
}
