package com.bimacore.usahakecil.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class InventoryRulesTest {
    @Test
    fun `wholesale quantity converts to the base stock unit`() {
        assertEquals(24, InventoryRules.toBaseQuantity(quantity = 2, factor = 12))
    }

    @Test
    fun `tier price uses the highest qualifying minimum quantity`() {
        val price = InventoryRules.resolveUnitPrice(
            basePrice = 10_000L,
            baseQuantity = 25,
            tiers = listOf(
                PriceTier(minimumBaseQuantity = 10, unitPrice = 9_500L),
                PriceTier(minimumBaseQuantity = 20, unitPrice = 9_000L),
            ),
        )

        assertEquals(9_000L, price)
    }

    @Test
    fun `manual stock adjustment requires a reason`() {
        assertThrows(IllegalArgumentException::class.java) {
            InventoryRules.adjustStock(current = 10, delta = 2, reason = " ")
        }
    }

    @Test
    fun `stock operation refuses a negative result`() {
        assertThrows(IllegalArgumentException::class.java) {
            InventoryRules.adjustStock(current = 3, delta = -4, reason = "Rusak")
        }
    }

    @Test
    fun `valid stock adjustment returns the new stock`() {
        assertEquals(
            7,
            InventoryRules.adjustStock(current = 10, delta = -3, reason = "Rusak"),
        )
    }
}
