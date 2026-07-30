package com.bimacore.usahakecil.domain

import com.bimacore.usahakecil.ui.AppDestination
import com.bimacore.usahakecil.ui.availableDestinations
import com.bimacore.usahakecil.ui.destinationsForAccess
import com.bimacore.usahakecil.ui.navigationPresentationFor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BusinessCapabilitiesTest {
    @Test
    fun `all flavors receive the approved shared offline modules`() {
        BusinessType.entries.forEach { type ->
            val capabilities = BusinessCapabilities.forType(type)

            assertTrue(capabilities.inventory)
            assertTrue(capabilities.purchasing)
            assertTrue(capabilities.cashLedger)
            assertTrue(capabilities.supplierPayables)
            assertTrue(capabilities.reports)
            assertTrue(capabilities.workforce)
            assertTrue(capabilities.backupRestore)
        }
    }

    @Test
    fun `retail enables customer receivables without wholesale or culinary tools`() {
        val capabilities = BusinessCapabilities.forType(BusinessType.RETAIL)

        assertTrue(capabilities.customerReceivables)
        assertFalse(capabilities.multiUnit)
        assertFalse(capabilities.tierPricing)
        assertFalse(capabilities.culinaryOrders)
        assertFalse(capabilities.recipes)
    }

    @Test
    fun `wholesale enables multi unit tier prices and receivables`() {
        val capabilities = BusinessCapabilities.forType(BusinessType.WHOLESALE)

        assertTrue(capabilities.customerReceivables)
        assertTrue(capabilities.multiUnit)
        assertTrue(capabilities.tierPricing)
        assertFalse(capabilities.culinaryOrders)
        assertFalse(capabilities.recipes)
    }

    @Test
    fun `culinary enables order and recipe tools without wholesale or receivable screens`() {
        val capabilities = BusinessCapabilities.forType(BusinessType.CULINARY)

        assertFalse(capabilities.customerReceivables)
        assertFalse(capabilities.multiUnit)
        assertFalse(capabilities.tierPricing)
        assertTrue(capabilities.culinaryOrders)
        assertTrue(capabilities.recipes)
    }

    @Test
    fun `main navigation keeps the cashier and approved offline areas reachable`() {
        val destinations = availableDestinations(
            BusinessCapabilities.forType(BusinessType.RETAIL),
        )

        assertEquals(
            listOf(
                AppDestination.POS,
                AppDestination.OPERATIONS,
                AppDestination.FINANCE,
                AppDestination.REPORTS,
                AppDestination.MORE,
            ),
            destinations,
        )
    }

    @Test
    fun `worker mode can only reach the cashier`() {
        val destinations = destinationsForAccess(
            capabilities = BusinessCapabilities.forType(BusinessType.RETAIL),
            ownerUnlocked = false,
        )

        assertEquals(listOf(AppDestination.POS), destinations)
    }

    @Test
    fun `owner mode can reach every approved offline area`() {
        val capabilities = BusinessCapabilities.forType(BusinessType.RETAIL)

        assertEquals(
            availableDestinations(capabilities),
            destinationsForAccess(capabilities, ownerUnlocked = true),
        )
    }

    @Test
    fun `retail navigation highlights receivables`() {
        val presentation = navigationPresentationFor(BusinessType.RETAIL)

        assertEquals("Operasional", presentation.operationsLabel)
        assertEquals("Produk", presentation.operationsStartSection)
        assertEquals("Piutang", presentation.financeLabel)
        assertEquals(1, presentation.financeStartTab)
    }

    @Test
    fun `wholesale navigation opens wholesale tools directly`() {
        val presentation = navigationPresentationFor(BusinessType.WHOLESALE)

        assertEquals("Grosir", presentation.operationsLabel)
        assertEquals("Grosir", presentation.operationsStartSection)
        assertEquals("Keuangan", presentation.financeLabel)
    }

    @Test
    fun `culinary navigation opens order tools directly`() {
        val presentation = navigationPresentationFor(BusinessType.CULINARY)

        assertEquals("Pesanan", presentation.operationsLabel)
        assertEquals("Kuliner", presentation.operationsStartSection)
        assertEquals("Keuangan", presentation.financeLabel)
    }
}
