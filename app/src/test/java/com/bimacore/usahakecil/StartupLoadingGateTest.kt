package com.bimacore.usahakecil

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StartupLoadingGateTest {
    @Test
    fun `brand loading stays visible until initialization and minimum duration both finish`() {
        assertTrue(shouldShowBrandLoading(isInitializing = true, minimumDurationElapsed = false))
        assertTrue(shouldShowBrandLoading(isInitializing = true, minimumDurationElapsed = true))
        assertTrue(shouldShowBrandLoading(isInitializing = false, minimumDurationElapsed = false))
        assertFalse(shouldShowBrandLoading(isInitializing = false, minimumDurationElapsed = true))
    }

    @Test
    fun `startup brand loading minimum is three seconds`() {
        assertEquals(3_000L, MIN_BRAND_LOADING_MILLIS)
    }
}
