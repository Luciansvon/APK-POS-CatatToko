package com.bimacore.usahakecil.data

import com.bimacore.usahakecil.domain.forecast.DailySales
import com.bimacore.usahakecil.domain.forecast.SalesForecastEngine
import com.bimacore.usahakecil.domain.forecast.SalesForecastResult
import com.bimacore.usahakecil.security.PinHashRecord
import com.bimacore.usahakecil.security.PinHasher
import com.bimacore.usahakecil.security.ReportSession
import java.util.Calendar
import java.util.TimeZone
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ReportLockedException : IllegalStateException("Laporan masih terkunci")

data class ReportSummary(
    val fromInclusive: Long,
    val toInclusive: Long,
    val transactionCount: Int,
    val totalSales: Long,
    val payments: List<PaymentAggregate>,
    val cashIn: Long,
    val cashOut: Long,
    val expenses: Long,
    val netCash: Long,
    val outstandingPayables: Long,
    val outstandingReceivables: Long,
)

data class ProductForecast(
    val productId: Long,
    val productName: String,
    val unitLabel: String,
    val result: SalesForecastResult?,
)

data class ProductForecastReport(
    val fromInclusive: Long,
    val toInclusive: Long,
    val products: List<ProductForecast>,
)

class ReportRepository(
    private val database: PosDatabase,
    val session: ReportSession,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    private val securityDao = database.securityDao()
    private val reportDao = database.reportDao()

    suspend fun hasPin(): Boolean = securityDao.getReportSecurity() != null

    suspend fun createPin(pin: String) {
        require(securityDao.getReportSecurity() == null) { "PIN Owner sudah dibuat" }
        savePin(pin)
        session.unlock()
    }

    suspend fun unlock(pin: String): Boolean {
        val stored = securityDao.getReportSecurity() ?: return false
        val valid = PinHasher.verify(pin, stored.toHashRecord())
        if (valid) session.unlock() else session.lock()
        return valid
    }

    suspend fun changePin(
        currentPin: String,
        newPin: String,
    ) {
        val stored = requireNotNull(securityDao.getReportSecurity()) {
            "PIN Owner belum dibuat"
        }
        require(PinHasher.verify(currentPin, stored.toHashRecord())) { "PIN lama salah" }
        savePin(newPin)
        session.unlock()
    }

    fun lock() {
        session.lock()
    }

    suspend fun readSummary(
        fromInclusive: Long,
        toInclusive: Long,
    ): ReportSummary {
        ensureUnlocked()
        require(fromInclusive <= toInclusive) { "Rentang tanggal laporan tidak valid" }
        val sales = reportDao.salesSummary(fromInclusive, toInclusive)
        val cash = reportDao.cashSummary(fromInclusive, toInclusive)
        val cashIn = cash
            .filter { it.type in CASH_IN_TYPES }
            .fold(0L) { total, item -> Math.addExact(total, item.total) }
        val cashOut = cash
            .filter { it.type in CASH_OUT_TYPES }
            .fold(0L) { total, item -> Math.addExact(total, item.total) }
        val expenses = cash
            .filter { it.type == "EXPENSE" }
            .fold(0L) { total, item -> Math.addExact(total, item.total) }
        return ReportSummary(
            fromInclusive = fromInclusive,
            toInclusive = toInclusive,
            transactionCount = sales.transactionCount,
            totalSales = sales.totalSales,
            payments = reportDao.paymentSummary(fromInclusive, toInclusive),
            cashIn = cashIn,
            cashOut = cashOut,
            expenses = expenses,
            netCash = Math.subtractExact(cashIn, cashOut),
            outstandingPayables = reportDao.outstandingDebt(DebtKind.PAYABLE.name),
            outstandingReceivables = reportDao.outstandingDebt(DebtKind.RECEIVABLE.name),
        )
    }

    suspend fun readProductForecasts(
        fromInclusive: Long = clock() - FORECAST_HISTORY_MILLIS,
        toInclusive: Long = clock(),
    ): ProductForecastReport {
        ensureUnlocked()
        require(fromInclusive <= toInclusive) { "Rentang tanggal prediksi tidak valid" }

        return withContext(Dispatchers.Default) {
            val activeProducts = database.catalogDao().getActiveProducts()
            val rows = reportDao.forecastSales(fromInclusive, toInclusive)
            val rowsByProduct = rows.groupBy { it.productId }
            val products = activeProducts.map { product ->
                val history = rowsByProduct[product.id]
                    .orEmpty()
                    .map { row ->
                        DailySales(
                            epochDay = toBusinessEpochDay(row.createdAt),
                            quantity = row.baseQuantity.toLong(),
                        )
                    }
                ProductForecast(
                    productId = product.id,
                    productName = product.name,
                    unitLabel = product.unitLabel,
                    result = history.takeIf { it.isNotEmpty() }
                        ?.let { runCatching { SalesForecastEngine.forecast(it) }.getOrNull() },
                )
            }
            ProductForecastReport(
                fromInclusive = fromInclusive,
                toInclusive = toInclusive,
                products = products,
            )
        }
    }

    private suspend fun savePin(pin: String) {
        val record = PinHasher.create(pin)
        securityDao.saveReportSecurity(
            ReportSecurityEntity(
                saltBase64 = record.saltBase64,
                hashBase64 = record.hashBase64,
                iterations = record.iterations,
                updatedAt = clock(),
            ),
        )
    }

    private fun ensureUnlocked() {
        if (!session.isUnlocked) throw ReportLockedException()
    }

    companion object {
        private const val MILLIS_PER_DAY = 86_400_000L
        private const val FORECAST_HISTORY_MILLIS = 730L * MILLIS_PER_DAY

        private val CASH_IN_TYPES = setOf("SALE_IN", "CASH_IN", "RECEIVABLE_IN")
        private val CASH_OUT_TYPES = setOf(
            "PURCHASE_OUT",
            "CASH_OUT",
            "EXPENSE",
            "PAYABLE_OUT",
            "WAGE_OUT",
        )
    }

    private fun toBusinessEpochDay(timestamp: Long): Long {
        val local = Calendar.getInstance().apply { timeInMillis = timestamp }
        val utcDate = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            clear()
            set(
                local.get(Calendar.YEAR),
                local.get(Calendar.MONTH),
                local.get(Calendar.DAY_OF_MONTH),
                0,
                0,
                0,
            )
        }
        return Math.floorDiv(utcDate.timeInMillis, MILLIS_PER_DAY)
    }
}

private fun ReportSecurityEntity.toHashRecord() = PinHashRecord(
    saltBase64 = saltBase64,
    hashBase64 = hashBase64,
    iterations = iterations,
)
