package com.bimacore.usahakecil.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class FormattersTest {
    @Test
    fun `compact rupiah keeps currency context for chart values`() {
        assertEquals("Rp84", formatCompactRupiah(84))
        assertEquals("Rp84 rb", formatCompactRupiah(84_000))
        assertEquals("Rp1,5 jt", formatCompactRupiah(1_500_000))
        assertEquals("Rp2 M", formatCompactRupiah(2_000_000_000))
    }

    @Test
    fun `compact rupiah preserves negative cash context`() {
        assertEquals("Rp-84 rb", formatCompactRupiah(-84_000))
    }
}
