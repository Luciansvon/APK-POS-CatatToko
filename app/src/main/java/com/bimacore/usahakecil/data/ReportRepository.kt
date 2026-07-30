package com.bimacore.usahakecil.data

import com.bimacore.usahakecil.security.PinHashRecord
import com.bimacore.usahakecil.security.PinHasher
import com.bimacore.usahakecil.security.ReportSession

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
        private val CASH_IN_TYPES = setOf("SALE_IN", "CASH_IN", "RECEIVABLE_IN")
        private val CASH_OUT_TYPES = setOf(
            "PURCHASE_OUT",
            "CASH_OUT",
            "EXPENSE",
            "PAYABLE_OUT",
            "WAGE_OUT",
        )
    }
}

private fun ReportSecurityEntity.toHashRecord() = PinHashRecord(
    saltBase64 = saltBase64,
    hashBase64 = hashBase64,
    iterations = iterations,
)
