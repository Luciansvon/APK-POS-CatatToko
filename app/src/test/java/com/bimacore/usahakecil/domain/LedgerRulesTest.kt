package com.bimacore.usahakecil.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class LedgerRulesTest {
    @Test
    fun `purchase total uses safe integer rupiah`() {
        assertEquals(
            230_000L,
            LedgerRules.total(
                listOf(
                    LedgerLine(unitPrice = 50_000L, quantity = 4),
                    LedgerLine(unitPrice = 15_000L, quantity = 2),
                ),
            ),
        )
    }

    @Test
    fun `debt keeps payment history and calculates remaining balance`() {
        assertEquals(
            40_000L,
            LedgerRules.remaining(
                originalAmount = 100_000L,
                payments = listOf(25_000L, 35_000L),
            ),
        )
    }

    @Test
    fun `payment cannot exceed outstanding debt`() {
        assertThrows(IllegalArgumentException::class.java) {
            LedgerRules.remaining(
                originalAmount = 100_000L,
                payments = listOf(60_000L, 50_000L),
            )
        }
    }

    @Test
    fun `settlement status distinguishes open partial and paid`() {
        assertEquals(SettlementStatus.OPEN, LedgerRules.status(100_000L, emptyList()))
        assertEquals(SettlementStatus.PARTIAL, LedgerRules.status(100_000L, listOf(25_000L)))
        assertEquals(SettlementStatus.PAID, LedgerRules.status(100_000L, listOf(100_000L)))
    }
}
