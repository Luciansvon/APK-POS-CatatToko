package com.bimacore.usahakecil.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class WorkforceRulesTest {
    @Test
    fun `half day pay keeps additions and deductions separate`() {
        assertEquals(
            135_000L,
            WorkforceRules.dailyPay(
                rate = 100_000L,
                attendance = AttendanceStatus.HALF_DAY,
                overtime = 25_000L,
                bonus = 70_000L,
                deduction = 10_000L,
                advance = 0L,
            ),
        )
    }

    @Test
    fun `absent worker has no base pay but can retain an approved bonus`() {
        assertEquals(
            20_000L,
            WorkforceRules.dailyPay(
                rate = 100_000L,
                attendance = AttendanceStatus.ABSENT,
                overtime = 0L,
                bonus = 20_000L,
                deduction = 0L,
                advance = 0L,
            ),
        )
    }

    @Test
    fun `deductions cannot make payment negative`() {
        assertThrows(IllegalArgumentException::class.java) {
            WorkforceRules.dailyPay(
                rate = 50_000L,
                attendance = AttendanceStatus.PRESENT,
                overtime = 0L,
                bonus = 0L,
                deduction = 60_000L,
                advance = 0L,
            )
        }
    }

    @Test
    fun `rate selection uses the latest rate effective on work date`() {
        val rates = listOf(
            EffectiveRate(amount = 80_000L, effectiveAt = 1_000L),
            EffectiveRate(amount = 100_000L, effectiveAt = 2_000L),
        )

        assertEquals(80_000L, WorkforceRules.rateAt(rates, workAt = 1_500L))
        assertEquals(100_000L, WorkforceRules.rateAt(rates, workAt = 2_500L))
    }
}
