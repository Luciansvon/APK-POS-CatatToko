package com.bimacore.usahakecil.data

import com.bimacore.usahakecil.domain.BusinessType
import org.junit.Assert.assertTrue
import org.junit.Test

class SeedCatalogTest {
    @Test
    fun `every flavor ships a usable offline catalog`() {
        BusinessType.entries.forEach { type ->
            val seed = SeedCatalog.forBusiness(type)
            assertTrue("$type categories", seed.categories.isNotEmpty())
            assertTrue("$type products", seed.products.size >= 8)
            assertTrue("$type positive prices", seed.products.all { it.basePrice > 0 })
        }
    }

    @Test
    fun `retail includes optional variants with stock`() {
        val seed = SeedCatalog.forBusiness(BusinessType.RETAIL)
        val variantProductIds = seed.products.filter { it.hasVariants }.map { it.id }.toSet()

        assertTrue(variantProductIds.isNotEmpty())
        assertTrue(seed.variants.all { it.productId in variantProductIds && it.stock >= 0 })
    }
}
