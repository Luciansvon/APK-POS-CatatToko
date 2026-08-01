package com.bimacore.usahakecil.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.bimacore.usahakecil.domain.AttendanceStatus
import com.bimacore.usahakecil.domain.BusinessCapabilities
import com.bimacore.usahakecil.domain.BusinessType
import com.bimacore.usahakecil.domain.CheckoutResult
import com.bimacore.usahakecil.domain.OrderStatus
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
class RepositoryFeatureCoverageTest {
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
    fun cashier_payment_matrix_enforces_short_cash_external_confirmation_and_receivables() = runBlocking {
        var now = 1_700_000_000_000L
        val pos = PosRepository(
            database = database,
            businessType = BusinessType.RETAIL,
            businessName = "Retail QA",
            clock = { now },
        )
        database.shiftDao().insertShift(
            ShiftEntity(
                cashierName = "Kasir QA",
                openedAt = 1L,
                openingCash = 0,
                openSlot = 1,
            ),
        )
        pos.seedIfNeeded()

        pos.addProduct(101)
        val shortCash = pos.completeSale(
            CheckoutRequest(PaymentMethod.CASH, amountReceived = 1_000, externalPaymentConfirmed = false),
        )
        assertTrue(shortCash is CheckoutResult.Error)
        assertTrue((shortCash as CheckoutResult.Error).message.contains("Uang kurang"))
        assertEquals(0, database.saleDao().getSalesBetween(0, Long.MAX_VALUE).size)

        pos.newTransaction()
        now += 1
        pos.addProduct(101)
        val qrisWithoutConfirmation = pos.completeSale(
            CheckoutRequest(PaymentMethod.QRIS, amountReceived = 0, externalPaymentConfirmed = false),
        )
        assertTrue(qrisWithoutConfirmation is CheckoutResult.Error)

        pos.newTransaction()
        now += 1
        pos.addProduct(101)
        val qris = pos.completeSale(
            CheckoutRequest(PaymentMethod.QRIS, amountReceived = 0, externalPaymentConfirmed = true),
        ) as CheckoutResult.Success
        assertEquals(PaymentMethod.QRIS, qris.receipt.paymentMethod)
        assertEquals(qris.receipt.total, qris.receipt.amountReceived)

        val operations = OperationsRepository(database)
        val customerId = operations.saveParty(null, PartyKind.CUSTOMER, "Pelanggan QA", "", "")
        pos.newTransaction()
        now += 1
        pos.addProduct(101)
        val credit = pos.completeSale(
            CheckoutRequest(
                method = PaymentMethod.CREDIT,
                amountReceived = 0,
                externalPaymentConfirmed = false,
                customerId = customerId,
            ),
        ) as CheckoutResult.Success
        assertEquals(PaymentMethod.CREDIT, credit.receipt.paymentMethod)
        assertEquals("UNPAID", database.saleDao().getSale(credit.receipt.saleId)?.settlementStatus)
        val receivable = operations.debts.first().single()
        assertEquals(DebtKind.RECEIVABLE.name, receivable.kind)
        assertEquals(credit.receipt.total, receivable.originalAmount)
    }

    @Test
    fun report_summary_reconciles_sales_cash_expenses_and_outstanding_debts() = runBlocking {
        val operations = OperationsRepository(database, clock = { 500L })
        val supplierId = operations.saveParty(null, PartyKind.SUPPLIER, "Supplier QA", "", "")
        val customerId = operations.saveParty(null, PartyKind.CUSTOMER, "Customer QA", "", "")
        database.saleDao().insertSale(
            SaleEntity(
                receiptNumber = "REPORT-CASH",
                businessName = "Retail QA",
                createdAt = 100,
                paymentMethod = PaymentMethod.CASH.name,
                total = 100_000,
                amountReceived = 120_000,
                changeAmount = 20_000,
            ),
        )
        database.saleDao().insertSale(
            SaleEntity(
                receiptNumber = "REPORT-QRIS",
                businessName = "Retail QA",
                createdAt = 200,
                paymentMethod = PaymentMethod.QRIS.name,
                total = 200_000,
                amountReceived = 200_000,
                changeAmount = 0,
            ),
        )
        database.operationsDao().insertCashEntry(
            CashEntryEntity(
                type = "SALE_IN",
                amount = 100_000,
                category = "Penjualan",
                note = "REPORT-CASH",
                paymentMethod = PaymentMethod.CASH.name,
                referenceType = "SALE",
                referenceId = null,
                createdAt = 100,
            ),
        )
        operations.addManualCashEntry(ManualCashType.CASH_IN, 50_000, "Modal tambahan", "")
        operations.addManualCashEntry(ManualCashType.EXPENSE, 30_000, "Parkir", "")
        operations.addManualCashEntry(ManualCashType.CASH_OUT, 40_000, "Belanja kecil", "")
        operations.createDebt(DebtKind.PAYABLE, supplierId, 90_000, 20_000, "Bahan")
        operations.createDebt(DebtKind.RECEIVABLE, customerId, 60_000, 10_000, "Penjualan")

        val reports = ReportRepository(database, ReportSession(), clock = { 1_000L })
        reports.createPin("2468")
        val summary = reports.readSummary(0, 1_000)

        assertEquals(2, summary.transactionCount)
        assertEquals(300_000L, summary.totalSales)
        assertEquals(160_000L, summary.cashIn)
        assertEquals(90_000L, summary.cashOut)
        assertEquals(30_000L, summary.expenses)
        assertEquals(70_000L, summary.netCash)
        assertEquals(70_000L, summary.outstandingPayables)
        assertEquals(50_000L, summary.outstandingReceivables)
        assertEquals(100_000L, summary.payments.single { it.paymentMethod == "CASH" }.total)
        assertEquals(200_000L, summary.payments.single { it.paymentMethod == "QRIS" }.total)
    }

    @Test
    fun workforce_daily_and_freelance_payments_snapshot_rates_and_write_wage_cash() = runBlocking {
        val workforce = WorkforceRepository(database, clock = { 5_000L })
        val dailyId = workforce.saveEmployeeWithInitialRate(
            name = "Pekerja Harian QA",
            phone = "0812",
            scheme = WorkerScheme.DAILY,
            dailyRate = 100_000,
            effectiveAt = 1_000L,
        )
        val attendanceId = workforce.recordAttendance(
            employeeId = dailyId,
            workDate = 2_000L,
            status = AttendanceStatus.PRESENT,
            overtime = 10_000,
            bonus = 5_000,
            deduction = 3_000,
            advance = 2_000,
            note = "Shift pagi",
        )
        assertEquals(110_000L, database.workforceDao().getAttendance(attendanceId)?.netPay)
        workforce.payAttendance(attendanceId, "Transfer upah harian")
        assertTrue(database.workforceDao().getAttendance(attendanceId)?.isPaid == true)

        val freelancerId = workforce.saveEmployee(
            id = null,
            name = "Freelancer QA",
            phone = "",
            scheme = WorkerScheme.FREELANCE,
        )
        val jobId = workforce.createFreelanceJob(freelancerId, "Foto produk", 200_000, 2_000L, "")
        workforce.payFreelanceJob(jobId, 80_000, "Termin 1")
        workforce.payFreelanceJob(jobId, 120_000, "Pelunasan")

        assertEquals("PAID", database.workforceDao().getFreelanceJob(jobId)?.status)
        assertEquals(3, workforce.observePayments(dailyId).first().size + workforce.observePayments(freelancerId).first().size)
        assertEquals(3, database.operationsDao().observeCashEntries().first().size)
        assertTrue(database.operationsDao().observeCashEntries().first().all { it.type == "WAGE_OUT" })
    }

    @Test
    fun culinary_order_status_follows_queue_and_non_culinary_repository_is_rejected() = runBlocking {
        val capabilities = BusinessCapabilities.forType(BusinessType.CULINARY)
        val inventory = InventoryRepository(database, capabilities)
        val culinary = CulinaryRepository(database, capabilities, clock = { 3_000L })
        val menuCategoryId = inventory.saveCategory(CategoryDraft(name = "Menu"))
        val ingredientCategoryId = inventory.saveCategory(CategoryDraft(name = "Bahan"))
        val menuId = inventory.saveProduct(
            ProductDraft(
                categoryId = menuCategoryId,
                name = "Mie QA",
                basePrice = 15_000,
                openingStock = 0,
                stockTrackingEnabled = false,
                lowStockThreshold = 0,
                unitLabel = "porsi",
            ),
        )
        val ingredientId = inventory.saveProduct(
            ProductDraft(
                categoryId = ingredientCategoryId,
                name = "Mie Mentah",
                basePrice = 4_000,
                openingStock = 10,
                stockTrackingEnabled = true,
                lowStockThreshold = 1,
                unitLabel = "pcs",
            ),
        )
        val toppingId = culinary.saveTopping(null, menuId, "Telur", 3_000)
        culinary.saveRecipeIngredient(menuId, ingredientId, 2)

        database.shiftDao().insertShift(
            ShiftEntity(cashierName = "Kasir Kuliner QA", openedAt = 1L, openingCash = 0, openSlot = 1),
        )
        val pos = PosRepository(database, BusinessType.CULINARY, "Kuliner QA", clock = { 4_000L })
        pos.addProduct(menuId)
        val lineId = database.cartDao().getLines().single().id
        pos.setCartCustomization(lineId, "Tidak pedas", mapOf(toppingId to 1))
        val sale = pos.completeSale(
            CheckoutRequest(PaymentMethod.CASH, amountReceived = 20_000, externalPaymentConfirmed = false),
        ) as CheckoutResult.Success

        assertEquals(OrderStatus.NEW.name, database.saleDao().getSale(sale.receipt.saleId)?.orderStatus)
        culinary.moveOrder(sale.receipt.saleId, OrderStatus.PROCESSING)
        val invalidSkip = runCatching { culinary.moveOrder(sale.receipt.saleId, OrderStatus.COMPLETED) }
        assertTrue(invalidSkip.isFailure)
        culinary.moveOrder(sale.receipt.saleId, OrderStatus.READY)
        culinary.moveOrder(sale.receipt.saleId, OrderStatus.COMPLETED)
        assertEquals(OrderStatus.COMPLETED.name, database.saleDao().getSale(sale.receipt.saleId)?.orderStatus)
        assertEquals(1, database.stockDao().getMovementsForSale(sale.receipt.saleId).count { it.type == "INGREDIENT_USE" })

        val retailCulinary = CulinaryRepository(
            database,
            BusinessCapabilities.forType(BusinessType.RETAIL),
        )
        assertTrue(runCatching { retailCulinary.observeToppings(menuId) }.isFailure)
    }
}
