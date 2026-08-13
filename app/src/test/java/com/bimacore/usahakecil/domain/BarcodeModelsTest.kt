package com.bimacore.usahakecil.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BarcodeModelsTest {
    @Test
    fun `normalization trims outer spaces without changing leading zero or case`() {
        assertEquals("0012AbC", normalizeBarcode("  0012AbC  "))
    }

    @Test
    fun `normalization rejects empty control character and overlong values`() {
        assertTrue(runCatching { normalizeBarcode("   ") }.isFailure)
        assertTrue(runCatching { normalizeBarcode("12\n34") }.isFailure)
        assertTrue(runCatching { normalizeBarcode("x".repeat(129)) }.isFailure)
    }
}
