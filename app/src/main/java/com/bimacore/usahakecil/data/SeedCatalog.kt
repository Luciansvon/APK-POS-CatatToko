package com.bimacore.usahakecil.data

import com.bimacore.usahakecil.domain.BusinessType

data class SeedData(
    val categories: List<CategoryEntity>,
    val products: List<ProductEntity>,
    val variants: List<ProductVariantEntity>,
)

object SeedCatalog {
    fun forBusiness(type: BusinessType): SeedData = when (type) {
        BusinessType.RETAIL -> retail()
        BusinessType.WHOLESALE -> wholesale()
        BusinessType.CULINARY -> culinary()
    }

    private fun retail(): SeedData {
        val categories = listOf(
            CategoryEntity(1, "Semua", "all", 0),
            CategoryEntity(2, "Makanan", "snack", 1),
            CategoryEntity(3, "Minuman", "drink", 2),
            CategoryEntity(4, "Pakaian", "shirt", 3),
            CategoryEntity(5, "Perawatan", "care", 4),
            CategoryEntity(6, "Alat Tulis", "stationery", 5),
        )
        val products = listOf(
            product(101, 2, "Keripik Singkong", 12_000, 18, 1),
            product(102, 2, "Biskuit Cokelat", 9_500, 6, 2),
            product(103, 3, "Air Mineral 600ml", 4_000, 36, 3),
            product(104, 3, "Teh Botol", 6_000, 4, 4),
            product(105, 4, "Kaos Polos Premium", 65_000, 0, 5, hasVariants = true),
            product(106, 4, "Kemeja Casual", 125_000, 0, 6, hasVariants = true),
            product(107, 5, "Sabun Mandi", 8_500, 22, 7),
            product(108, 5, "Sampo Sachet", 2_000, 50, 8),
            product(109, 6, "Buku Tulis", 6_500, 14, 9),
            product(110, 6, "Pulpen Gel", 5_000, 0, 10),
        )
        val variants = listOf(
            variant(1_051, 105, "S · Hitam", 8, 1),
            variant(1_052, 105, "M · Hitam", 10, 2),
            variant(1_053, 105, "L · Putih", 5, 3),
            variant(1_061, 106, "M · Navy", 4, 1),
            variant(1_062, 106, "L · Navy", 3, 2),
            variant(1_063, 106, "XL · Cream", 2, 3, 135_000),
        )
        return SeedData(categories, products, variants)
    }

    private fun wholesale(): SeedData {
        val categories = listOf(
            CategoryEntity(1, "Semua", "all", 0),
            CategoryEntity(2, "Sembako", "box", 1),
            CategoryEntity(3, "Minuman", "drink", 2),
            CategoryEntity(4, "Kebutuhan Toko", "store", 3),
        )
        val products = listOf(
            product(201, 2, "Beras Premium 5kg", 78_000, 24, 1),
            product(202, 2, "Gula Pasir 1kg", 18_500, 40, 2),
            product(203, 2, "Minyak Goreng 1L", 19_000, 32, 3),
            product(204, 3, "Air Mineral 1 Dus", 52_000, 18, 4),
            product(205, 3, "Teh Kotak 1 Dus", 82_000, 7, 5),
            product(206, 4, "Kantong Plastik Besar", 28_000, 11, 6),
            product(207, 4, "Tisu 1 Pak", 36_000, 5, 7),
            product(208, 4, "Lakban Cokelat", 14_000, 0, 8),
        )
        return SeedData(categories, products, emptyList())
    }

    private fun culinary(): SeedData {
        val categories = listOf(
            CategoryEntity(1, "Semua", "all", 0),
            CategoryEntity(2, "Makanan", "meal", 1),
            CategoryEntity(3, "Minuman", "drink", 2),
            CategoryEntity(4, "Tambahan", "extra", 3),
        )
        val products = listOf(
            product(301, 2, "Nasi Goreng Kampung", 22_000, 99, 1, trackStock = false),
            product(302, 2, "Mie Goreng Jawa", 20_000, 99, 2, trackStock = false),
            product(303, 2, "Ayam Geprek", 24_000, 99, 3, trackStock = false),
            product(304, 3, "Es Teh Manis", 6_000, 99, 4, trackStock = false),
            product(305, 3, "Kopi Susu", 12_000, 99, 5, trackStock = false),
            product(306, 3, "Jeruk Hangat", 8_000, 99, 6, trackStock = false),
            product(307, 4, "Telur Mata Sapi", 6_000, 20, 7),
            product(308, 4, "Extra Sambal", 3_000, 99, 8, trackStock = false),
        )
        return SeedData(categories, products, emptyList())
    }

    private fun product(
        id: Long,
        categoryId: Long,
        name: String,
        price: Long,
        stock: Int,
        sortOrder: Int,
        hasVariants: Boolean = false,
        trackStock: Boolean = true,
    ) = ProductEntity(
        id = id,
        categoryId = categoryId,
        name = name,
        basePrice = price,
        stock = stock,
        stockTrackingEnabled = trackStock,
        hasVariants = hasVariants,
        lowStockThreshold = 5,
        imageUri = null,
        sortOrder = sortOrder,
    )

    private fun variant(
        id: Long,
        productId: Long,
        label: String,
        stock: Int,
        sortOrder: Int,
        priceOverride: Long? = null,
    ) = ProductVariantEntity(id, productId, label, priceOverride, stock, sortOrder)
}
