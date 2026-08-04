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

    suspend fun readTrend(
        granularity: ReportChartGranularity,
        now: Long = clock(),
    ): ReportTrendReport {
        ensureUnlocked()
        val buckets = createTrendBuckets(granularity, now)
        val fromInclusive = buckets.first().start
        val toInclusive = now
        val salesRows = reportDao.salesTrendRows(fromInclusive, toInclusive)
        val productRows = reportDao.productTrendRows(fromInclusive, toInclusive)
        val cashRows = reportDao.cashTrendRows(fromInclusive, toInclusive)

        val salesByBucket = buckets.associate { it.start to TrendAccumulator() }
        salesRows.forEach { row ->
            findTrendBucket(buckets, row.createdAt)?.let { bucket ->
                val accumulator = salesByBucket.getValue(bucket.start)
                accumulator.sales = Math.addExact(accumulator.sales, row.total)
                accumulator.transactionCount++
            }
        }

        val productsByKey = productRows.groupBy { Pair(it.productId, it.variantId) }
        val productTrends = productsByKey.map { (key, rows) ->
            val first = rows.first()
            val (productId, variantId) = key
            val productAccumulators = buckets.associate { it.start to TrendAccumulator() }
            rows.forEach { row ->
                findTrendBucket(buckets, row.createdAt)?.let { bucket ->
                    val accumulator = productAccumulators.getValue(bucket.start)
                    accumulator.sales = Math.addExact(accumulator.sales, row.subtotal)
                    accumulator.quantity = Math.addExact(
                        accumulator.quantity,
                        row.baseQuantity.toLong(),
                    )
                }
            }
            val displayName = if (first.variantName.isNullOrBlank()) {
                first.productName
            } else {
                "${first.productName} (${first.variantName})"
            }
            ReportProductTrend(
                productId = productId,
                productName = displayName,
                variantId = variantId,
                variantName = first.variantName,
                unitLabel = first.unitLabel,
                points = buckets.map { bucket ->
                    productAccumulators.getValue(bucket.start).toPoint(bucket.start)
                },
            )
        }.sortedByDescending { product ->
            product.points.sumOf(ReportTrendPoint::sales)
        }

        cashRows.forEach { row ->
            findTrendBucket(buckets, row.createdAt)?.let { bucket ->
                val accumulator = salesByBucket.getValue(bucket.start)
                when {
                    row.type in CASH_IN_TYPES -> {
                        accumulator.cashIn = Math.addExact(accumulator.cashIn, row.amount)
                    }
                    row.type in CASH_OUT_TYPES -> {
                        accumulator.cashOut = Math.addExact(accumulator.cashOut, row.amount)
                    }
                }
            }
        }

        val points = buckets.map { bucket ->
            val accumulator = salesByBucket.getValue(bucket.start)
            accumulator.quantity = productTrends.sumOf { product ->
                product.points.first { point -> point.bucketStart == bucket.start }.quantity
            }
            accumulator.toPoint(bucket.start)
        }
        return ReportTrendReport(
            granularity = granularity,
            fromInclusive = fromInclusive,
            toInclusive = toInclusive,
            points = points,
            products = productTrends,
        )
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

    private fun createTrendBuckets(
        granularity: ReportChartGranularity,
        now: Long,
    ): List<TrendBucket> {
        val currentStart = Calendar.getInstance().apply {
            timeInMillis = now
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            when (granularity) {
                ReportChartGranularity.DAILY -> Unit
                ReportChartGranularity.WEEKLY -> {
                    val daysFromMonday =
                        (get(Calendar.DAY_OF_WEEK) - Calendar.MONDAY + 7) % 7
                    add(Calendar.DAY_OF_MONTH, -daysFromMonday)
                }
                ReportChartGranularity.MONTHLY -> set(Calendar.DAY_OF_MONTH, 1)
                ReportChartGranularity.YEARLY -> {
                    set(Calendar.MONTH, Calendar.JANUARY)
                    set(Calendar.DAY_OF_MONTH, 1)
                }
            }
        }
        val firstStart = (currentStart.clone() as Calendar).apply {
            when (granularity) {
                ReportChartGranularity.DAILY -> add(
                    Calendar.DAY_OF_MONTH,
                    -(granularity.bucketCount - 1),
                )
                ReportChartGranularity.WEEKLY -> add(
                    Calendar.DAY_OF_MONTH,
                    -7 * (granularity.bucketCount - 1),
                )
                ReportChartGranularity.MONTHLY -> add(
                    Calendar.MONTH,
                    -(granularity.bucketCount - 1),
                )
                ReportChartGranularity.YEARLY -> add(
                    Calendar.YEAR,
                    -(granularity.bucketCount - 1),
                )
            }
        }
        return (0 until granularity.bucketCount).map { index ->
            val start = (firstStart.clone() as Calendar).apply {
                when (granularity) {
                    ReportChartGranularity.DAILY -> add(Calendar.DAY_OF_MONTH, index)
                    ReportChartGranularity.WEEKLY -> add(Calendar.DAY_OF_MONTH, index * 7)
                    ReportChartGranularity.MONTHLY -> add(Calendar.MONTH, index)
                    ReportChartGranularity.YEARLY -> add(Calendar.YEAR, index)
                }
            }.timeInMillis
            val nextStart = (firstStart.clone() as Calendar).apply {
                when (granularity) {
                    ReportChartGranularity.DAILY -> add(Calendar.DAY_OF_MONTH, index + 1)
                    ReportChartGranularity.WEEKLY -> add(Calendar.DAY_OF_MONTH, (index + 1) * 7)
                    ReportChartGranularity.MONTHLY -> add(Calendar.MONTH, index + 1)
                    ReportChartGranularity.YEARLY -> add(Calendar.YEAR, index + 1)
                }
            }.timeInMillis
            TrendBucket(start = start, end = nextStart - 1L)
        }
    }

    private fun findTrendBucket(
        buckets: List<TrendBucket>,
        timestamp: Long,
    ): TrendBucket? = buckets.lastOrNull { timestamp >= it.start && timestamp <= it.end }

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

    private data class TrendBucket(
        val start: Long,
        val end: Long,
    )

    private class TrendAccumulator {
        var sales: Long = 0
        var transactionCount: Int = 0
        var quantity: Long = 0
        var cashIn: Long = 0
        var cashOut: Long = 0

        fun toPoint(bucketStart: Long) = ReportTrendPoint(
            bucketStart = bucketStart,
            sales = sales,
            transactionCount = transactionCount,
            quantity = quantity,
            cashIn = cashIn,
            cashOut = cashOut,
            netCash = Math.subtractExact(cashIn, cashOut),
        )
    }
}

private fun ReportSecurityEntity.toHashRecord() = PinHashRecord(
    saltBase64 = saltBase64,
    hashBase64 = hashBase64,
    iterations = iterations,
)
