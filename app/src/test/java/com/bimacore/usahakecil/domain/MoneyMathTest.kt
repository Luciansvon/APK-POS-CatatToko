package com.bimacore.usahakecil.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class MoneyMathTest {
    @Test
    fun `total uses integer rupiah without rounding`() {
        val total = MoneyMath.total(
            listOf(
                12_000L to 2,
                4_500L to 3,
            ),
        )

        assertEquals(37_500L, total)
    }

    @Test
    fun `insufficient cash reports shortage and never negative change`() {
        assertEquals(7_500L, MoneyMath.shortage(25_000L, 17_500L))
        assertEquals(0L, MoneyMath.change(25_000L, 17_500L))
    }

    @Test
    fun `exact cash has zero shortage and zero change`() {
        assertEquals(0L, MoneyMath.shortage(25_000L, 25_000L))
        assertEquals(0L, MoneyMath.change(25_000L, 25_000L))
    }

    @Test
    fun `overpayment returns change`() {
        assertEquals(25_000L, MoneyMath.change(75_000L, 100_000L))
    }

    @Test
    fun `quick cash amounts include exact and unique rounded suggestions`() {
        assertEquals(
            listOf(37_500L, 40_000L, 50_000L, 100_000L),
            MoneyMath.quickCashAmounts(37_500L),
        )
    }

    @Test
    fun `unsafe quantity is rejected`() {
        assertThrows(IllegalArgumentException::class.java) {
            MoneyMath.multiply(10_000L, MoneyMath.MAX_QUANTITY + 1)
        }
    }
}
