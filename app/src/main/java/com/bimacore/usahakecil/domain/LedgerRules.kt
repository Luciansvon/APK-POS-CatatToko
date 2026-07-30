package com.bimacore.usahakecil.domain

data class LedgerLine(
    val unitPrice: Long,
    val quantity: Int,
)

enum class SettlementStatus {
    OPEN,
    PARTIAL,
    PAID,
}

object LedgerRules {
    fun total(lines: List<LedgerLine>): Long =
        MoneyMath.total(lines.map { it.unitPrice to it.quantity })

    fun remaining(
        originalAmount: Long,
        payments: List<Long>,
    ): Long {
        require(originalAmount in 0..MoneyMath.MAX_MONEY) { "Nilai awal tidak valid" }
        val paid = payments.fold(0L) { current, payment ->
            require(payment in 0..MoneyMath.MAX_MONEY) { "Pembayaran tidak valid" }
            Math.addExact(current, payment).also {
                require(it <= originalAmount) { "Pembayaran melebihi sisa tagihan" }
            }
        }
        return originalAmount - paid
    }

    fun status(
        originalAmount: Long,
        payments: List<Long>,
    ): SettlementStatus {
        val remaining = remaining(originalAmount, payments)
        return when {
            remaining == 0L -> SettlementStatus.PAID
            remaining == originalAmount -> SettlementStatus.OPEN
            else -> SettlementStatus.PARTIAL
        }
    }
}
