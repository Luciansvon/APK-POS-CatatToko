package com.bimacore.usahakecil.report

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.semantics.SemanticsActions
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.bimacore.usahakecil.FirstRunGuidePreferences
import com.bimacore.usahakecil.BuildConfig
import com.bimacore.usahakecil.MainActivity
import com.bimacore.usahakecil.PosApplication
import com.bimacore.usahakecil.data.CashEntryEntity
import com.bimacore.usahakecil.data.CategoryEntity
import com.bimacore.usahakecil.data.DebtEntity
import com.bimacore.usahakecil.data.DebtKind
import com.bimacore.usahakecil.data.PartyEntity
import com.bimacore.usahakecil.data.PartyKind
import com.bimacore.usahakecil.data.ProductEntity
import com.bimacore.usahakecil.data.ReportRepository
import com.bimacore.usahakecil.data.SaleEntity
import com.bimacore.usahakecil.data.SaleItemEntity
import com.bimacore.usahakecil.domain.PaymentMethod
import com.bimacore.usahakecil.export.ExcelExportManager
import com.bimacore.usahakecil.security.ReportSession
import java.util.Calendar
import java.util.zip.ZipInputStream
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ReportDemoTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun seedReportDemo() = runBlocking {
        val application = composeRule.activity.application as PosApplication
        val database = application.database
        val now = System.currentTimeMillis()

        database.openHelper.writableDatabase.execSQL("DELETE FROM sale_item_toppings")
        database.openHelper.writableDatabase.execSQL("DELETE FROM sale_items")
        database.openHelper.writableDatabase.execSQL("DELETE FROM sales")
        database.openHelper.writableDatabase.execSQL("DELETE FROM cash_entries")
        database.openHelper.writableDatabase.execSQL("DELETE FROM debt_payments")
        database.openHelper.writableDatabase.execSQL("DELETE FROM debts")
        database.openHelper.writableDatabase.execSQL("DELETE FROM parties")
        database.openHelper.writableDatabase.execSQL("DELETE FROM report_security")

        val product = database.catalogDao().getActiveProducts().firstOrNull()
            ?: ProductEntity(
                id = 101,
                categoryId = 1,
                name = "Produk Demo",
                basePrice = 25_000,
                stock = 100,
                stockTrackingEnabled = false,
                hasVariants = false,
                lowStockThreshold = 5,
                imageUri = null,
                sortOrder = 1,
            ).also {
                database.catalogDao().insertCategory(
                    CategoryEntity(1, "Demo", "inventory", 1),
                )
                database.catalogDao().insertProduct(it)
            }
        val categoryName = database.catalogDao().getCategory(product.categoryId)?.name ?: "Demo"
        val today = startOfDay(now)

        listOf(
            DemoSale(PaymentMethod.CASH, 25_000, 25_000),
            DemoSale(PaymentMethod.QRIS, 35_000, 35_000),
            DemoSale(PaymentMethod.TRANSFER, 45_000, 45_000),
            DemoSale(PaymentMethod.CREDIT, 30_000, 10_000),
        ).forEachIndexed { index, sale ->
            insertSale(
                database = database,
                product = product,
                categoryName = categoryName,
                receiptNumber = "DEMO-TODAY-${index + 1}",
                createdAt = now - (4 - index).toLong() * 60_000L,
                sale = sale,
            )
        }

        repeat(20) { index ->
            insertSale(
                database = database,
                product = product,
                categoryName = categoryName,
                receiptNumber = "DEMO-HISTORY-${index + 1}",
                createdAt = today - (index + 1) * MILLIS_PER_DAY + 3_600_000L,
                sale = DemoSale(
                    method = PaymentMethod.CASH,
                    total = 12_000L + index * 500L,
                    received = 12_000L + index * 500L,
                ),
            )
        }

        database.operationsDao().insertCashEntry(
            CashEntryEntity(
                type = "CASH_IN",
                amount = 50_000,
                category = "Modal tambahan",
                note = "Demo laporan",
                paymentMethod = "CASH",
                referenceType = null,
                referenceId = null,
                createdAt = now - 30 * 60_000L,
            ),
        )
        database.operationsDao().insertCashEntry(
            CashEntryEntity(
                type = "CASH_OUT",
                amount = 12_000,
                category = "Operasional",
                note = "Demo laporan",
                paymentMethod = "CASH",
                referenceType = null,
                referenceId = null,
                createdAt = now - 20 * 60_000L,
            ),
        )
        database.operationsDao().insertCashEntry(
            CashEntryEntity(
                type = "EXPENSE",
                amount = 8_000,
                category = "Transport",
                note = "Demo laporan",
                paymentMethod = "CASH",
                referenceType = null,
                referenceId = null,
                createdAt = now - 10 * 60_000L,
            ),
        )

        val supplierId = database.operationsDao().insertParty(
            PartyEntity(
                kind = PartyKind.SUPPLIER.name,
                name = "Supplier Demo",
                phone = "",
                address = "",
                createdAt = now,
                updatedAt = now,
            ),
        )
        val customerId = database.operationsDao().insertParty(
            PartyEntity(
                kind = PartyKind.CUSTOMER.name,
                name = "Pelanggan Demo",
                phone = "",
                address = "",
                createdAt = now,
                updatedAt = now,
            ),
        )
        database.operationsDao().insertDebt(
            DebtEntity(
                kind = DebtKind.PAYABLE.name,
                partyId = supplierId,
                partyName = "Supplier Demo",
                sourceType = "DEMO",
                sourceId = 1,
                originalAmount = 100_000,
                paidAmount = 40_000,
                settlementStatus = "PARTIAL",
                note = "Utang demo",
                createdAt = now,
                updatedAt = now,
            ),
        )
        database.operationsDao().insertDebt(
            DebtEntity(
                kind = DebtKind.RECEIVABLE.name,
                partyId = customerId,
                partyName = "Pelanggan Demo",
                sourceType = "DEMO",
                sourceId = 2,
                originalAmount = 80_000,
                paidAmount = 20_000,
                settlementStatus = "PARTIAL",
                note = "Piutang demo",
                createdAt = now,
                updatedAt = now,
            ),
        )

        application.reportSession.lock()
        composeRule.activity.getSharedPreferences(FirstRunGuidePreferences.FILE_NAME, 0)
            .edit()
            .putBoolean(FirstRunGuidePreferences.COMPLETED_KEY, true)
            .commit()
        composeRule.activity.runOnUiThread { composeRule.activity.recreate() }
        composeRule.waitForIdle()
    }

    @Test
    fun demo_covers_all_report_cards_chart_periods_analysis_and_excel() = runBlocking {
        val application = composeRule.activity.application as PosApplication
        val database = application.database
        val now = System.currentTimeMillis()
        val today = startOfDay(now)
        val reports = ReportRepository(database, ReportSession().also { it.unlock() })

        val daily = reports.readSummary(today, now)
        val weekly = reports.readSummary(today - 6 * MILLIS_PER_DAY, now)
        val monthly = reports.readSummary(today - 29 * MILLIS_PER_DAY, now)
        val yearly = reports.readSummary(today - 364 * MILLIS_PER_DAY, now)

        assertEquals(4, daily.transactionCount)
        assertEquals(135_000L, daily.totalSales)
        assertEquals(165_000L, daily.cashIn)
        assertEquals(20_000L, daily.cashOut)
        assertEquals(8_000L, daily.expenses)
        assertEquals(145_000L, daily.netCash)
        assertEquals(60_000L, daily.outstandingPayables)
        assertEquals(60_000L, daily.outstandingReceivables)
        assertEquals(4, daily.payments.size)
        assertTrue(weekly.transactionCount > daily.transactionCount)
        assertTrue(monthly.transactionCount >= weekly.transactionCount)
        assertEquals(monthly.transactionCount, yearly.transactionCount)

        val dailyTrend = reports.readTrend(
            com.bimacore.usahakecil.data.ReportChartGranularity.DAILY,
            now,
        )
        assertEquals(14, dailyTrend.points.size)
        assertEquals(135_000L, dailyTrend.points.last().sales)
        assertEquals(165_000L, dailyTrend.points.last().cashIn)
        assertEquals(20_000L, dailyTrend.points.last().cashOut)
        assertTrue(dailyTrend.products.isNotEmpty())
        assertEquals(
            8,
            reports.readTrend(
                com.bimacore.usahakecil.data.ReportChartGranularity.WEEKLY,
                now,
            ).points.size,
        )
        assertEquals(
            12,
            reports.readTrend(
                com.bimacore.usahakecil.data.ReportChartGranularity.MONTHLY,
                now,
            ).points.size,
        )
        assertEquals(
            5,
            reports.readTrend(
                com.bimacore.usahakecil.data.ReportChartGranularity.YEARLY,
                now,
            ).points.size,
        )

        val forecast = reports.readProductForecasts(
            fromInclusive = today - 30 * MILLIS_PER_DAY,
            toInclusive = now,
        )
        val forecastResult = forecast.products.firstNotNullOfOrNull { it.result }
        assertNotNull(forecastResult)
        assertTrue(requireNotNull(forecastResult).normalizedHistoryDays >= 19)
        assertEquals(7, requireNotNull(forecastResult).forecast.size)
        assertTrue(requireNotNull(forecastResult).rankedCandidates.isNotEmpty())

        val exportManager = ExcelExportManager(
            context = composeRule.activity,
            database = database,
            ownerSession = ReportSession().also { it.unlock() },
            businessType = BuildConfig.BUSINESS_TYPE,
        )
        val entries = readWorkbook(exportManager.createExport())
        assertTrue(entries.values.any { it.contains("DEMO-TODAY-1") })
        assertTrue(entries.values.any { it.contains("Ringkasan Keuangan") })
        val dailyEntries = readWorkbook(exportManager.createExport(com.bimacore.usahakecil.data.ReportPeriod.DAY))
        assertTrue(dailyEntries.values.any { it.contains("DEMO-TODAY-1") })
        assertTrue(dailyEntries.values.any { it.contains("Periode") && it.contains("Hari ini") })

        unlockOwner()
        composeRule.onNodeWithText("Laporan").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("Omzet hari ini")
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("Omzet hari ini").assertIsDisplayed()
        composeRule.onNodeWithText("Rp135.000").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("report-period-selector").assertIsDisplayed()
        composeRule.onNodeWithTag("report-sales-chart").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("report-full-details")
            .performScrollTo()
            .performSemanticsAction(SemanticsActions.OnClick)
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodesWithTag("report-chart-mode")
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("report-chart-mode").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("report-chart-granularity").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Arus kas tercatat").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("report-chart-mode").performClick()
        composeRule.onNodeWithTag("report-chart-mode-option-Penjualan").performClick()
        assertTrue(
            composeRule.onAllNodesWithText("Penjualan")
                .fetchSemanticsNodes()
                .isNotEmpty(),
        )
        composeRule.onNodeWithTag("report-chart-mode").performClick()
        composeRule.onNodeWithTag("report-chart-mode-option-Produk").performClick()
        composeRule.onNodeWithTag("report-product-selector").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Semua produk / Omzet").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("report-product-measure")
            .performSemanticsAction(SemanticsActions.OnClick)
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("Tampilkan sebagai")
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onAllNodesWithText("Terjual").assertCountEquals(0)
        composeRule.onNodeWithTag("report-product-selector-option-Semua produk")
            .performSemanticsAction(SemanticsActions.OnClick)
        composeRule.onNodeWithText("Penjualan produk").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("report-chart-granularity").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("report-period-selector").performScrollTo().performClick()
        composeRule.onAllNodesWithText("\u00E2", substring = true).assertCountEquals(0)
        assertTrue(
            composeRule.onAllNodesWithText(" - ", substring = true)
                .fetchSemanticsNodes().size >= 3,
        )
        composeRule.onNodeWithTag("report-period-option-WEEK").performClick()
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodesWithText("Omzet minggu ini")
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("report-period-selector").performScrollTo().performClick()
        composeRule.onNodeWithTag("report-period-option-MONTH").performClick()
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodesWithText("Omzet bulan ini")
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("report-period-selector").performScrollTo().performClick()
        composeRule.onNodeWithTag("report-period-option-YEAR").performClick()
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodesWithText("Omzet tahun ini")
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("report-period-selector").performScrollTo().performClick()
        composeRule.onNodeWithTag("report-period-option-DAY").performClick()
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodesWithText("Omzet hari ini")
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("payment-method-chart")
            .performScrollTo()
            .assertIsDisplayed()
        listOf("Tunai", "QRIS", "Transfer", "Piutang").forEach { method ->
            assertTrue(
                composeRule.onAllNodesWithText(method)
                    .fetchSemanticsNodes()
                    .isNotEmpty(),
            )
        }
        composeRule.onNodeWithText("Perkiraan penjualan")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithText("Perkiraan 7 hari:", substring = true)
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithText("Nilai per hari", substring = true)
            .performScrollTo()
            .assertIsDisplayed()
        Unit
    }

    private fun unlockOwner() {
        composeRule.onNodeWithTag("owner-access").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("PIN Owner")
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("owner-pin-input").performTextInput("2468")
        composeRule.onNodeWithTag("owner-submit").performClick()
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodesWithText("Laporan")
                .fetchSemanticsNodes().isNotEmpty()
        }
    }

    private suspend fun insertSale(
        database: com.bimacore.usahakecil.data.PosDatabase,
        product: ProductEntity,
        categoryName: String,
        receiptNumber: String,
        createdAt: Long,
        sale: DemoSale,
    ) {
        val saleId = database.saleDao().insertSale(
            SaleEntity(
                receiptNumber = receiptNumber,
                businessName = "Demo CatatToko",
                createdAt = createdAt,
                paymentMethod = sale.method.name,
                total = sale.total,
                amountReceived = sale.received,
                changeAmount = 0,
                customerId = null,
                updatedAt = createdAt,
            ),
        )
        database.saleDao().insertItems(
            listOf(
                SaleItemEntity(
                    saleId = saleId,
                    productId = product.id,
                    variantId = null,
                    productName = product.name,
                    variantName = null,
                    categoryName = categoryName,
                    unitPrice = sale.total,
                    quantity = 1,
                    subtotal = sale.total,
                    baseQuantity = 1,
                    unitLabel = product.unitLabel,
                ),
            ),
        )
        if (sale.received > 0) {
            database.operationsDao().insertCashEntry(
                CashEntryEntity(
                    type = if (sale.method == PaymentMethod.CREDIT) "RECEIVABLE_IN" else "SALE_IN",
                    amount = sale.received,
                    category = "Penjualan",
                    note = "Penjualan demo $receiptNumber",
                    paymentMethod = sale.method.name,
                    referenceType = "SALE",
                    referenceId = saleId,
                    createdAt = createdAt,
                ),
            )
        }
    }

    private fun readWorkbook(uri: android.net.Uri): Map<String, String> {
        val entries = linkedMapOf<String, String>()
        composeRule.activity.contentResolver.openInputStream(uri)!!.use { input ->
            ZipInputStream(input).use { zip ->
                while (true) {
                    val entry = zip.nextEntry ?: break
                    entries[entry.name] = zip.readBytes().toString(Charsets.UTF_8)
                }
            }
        }
        return entries
    }

    private fun startOfDay(timestamp: Long): Long = Calendar.getInstance().apply {
        timeInMillis = timestamp
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    private data class DemoSale(
        val method: PaymentMethod,
        val total: Long,
        val received: Long,
    )

    private companion object {
        const val MILLIS_PER_DAY = 86_400_000L
    }
}
