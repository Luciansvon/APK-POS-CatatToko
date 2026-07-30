package com.bimacore.usahakecil.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CulinaryRulesTest {
    @Test
    fun `order status only moves forward one operational step`() {
        assertTrue(CulinaryRules.canMove(OrderStatus.NEW, OrderStatus.PROCESSING))
        assertTrue(CulinaryRules.canMove(OrderStatus.READY, OrderStatus.COMPLETED))
        assertFalse(CulinaryRules.canMove(OrderStatus.READY, OrderStatus.NEW))
        assertFalse(CulinaryRules.canMove(OrderStatus.NEW, OrderStatus.COMPLETED))
    }

    @Test
    fun `ingredient consumption follows menu quantity`() {
        assertEquals(
            6,
            CulinaryRules.ingredientQuantity(
                quantityPerMenu = 2,
                menuQuantity = 3,
            ),
        )
    }

    @Test
    fun `topping prices are included for every menu quantity`() {
        assertEquals(
            12_000L,
            CulinaryRules.toppingTotal(
                toppingPrices = listOf(2_000L, 4_000L),
                menuQuantity = 2,
            ),
        )
    }
}
