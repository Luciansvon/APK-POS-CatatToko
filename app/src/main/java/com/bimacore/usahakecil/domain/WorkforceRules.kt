package com.bimacore.usahakecil.domain

enum class AttendanceStatus {
    PRESENT,
    HALF_DAY,
    LEAVE,
    ABSENT,
}

data class EffectiveRate(
    val amount: Long,
    val effectiveAt: Long,
)

object WorkforceRules {
    fun dailyPay(
        rate: Long,
        attendance: AttendanceStatus,
        overtime: Long,
        bonus: Long,
        deduction: Long,
        advance: Long,
    ): Long {
        listOf(rate, overtime, bonus, deduction, advance).forEach {
            require(it in 0..MoneyMath.MAX_MONEY) { "Komponen upah tidak valid" }
        }
        val basePay = when (attendance) {
            AttendanceStatus.PRESENT -> rate
            AttendanceStatus.HALF_DAY -> rate / 2L
            AttendanceStatus.LEAVE,
            AttendanceStatus.ABSENT,
            -> 0L
        }
        val gross = Math.addExact(
            Math.addExact(basePay, overtime),
            bonus,
        ).also {
            require(it <= MoneyMath.MAX_MONEY) { "Nilai upah terlalu besar" }
        }
        val totalDeductions = Math.addExact(deduction, advance).also {
            require(it <= MoneyMath.MAX_MONEY) { "Nilai potongan terlalu besar" }
        }
        require(totalDeductions <= gross) { "Potongan melebihi upah" }
        return gross - totalDeductions
    }

    fun rateAt(
        rates: List<EffectiveRate>,
        workAt: Long,
    ): Long = rates
        .onEach {
            require(it.amount in 0..MoneyMath.MAX_MONEY) { "Tarif tidak valid" }
        }
        .filter { it.effectiveAt <= workAt }
        .maxByOrNull { it.effectiveAt }
        ?.amount
        ?: throw IllegalArgumentException("Tarif belum berlaku pada tanggal pekerjaan")
}
