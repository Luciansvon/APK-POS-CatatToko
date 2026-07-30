package com.bimacore.usahakecil.domain

data class PriceTier(
    val minimumBaseQuantity: Int,
    val unitPrice: Long,
)

object InventoryRules {
    const val MAX_STOCK = 99_999_999

    fun toBaseQuantity(
        quantity: Int,
        factor: Int,
    ): Int {
        require(quantity >= 0) { "Jumlah tidak boleh negatif" }
        require(factor > 0) { "Konversi satuan harus lebih dari nol" }
        return Math.multiplyExact(quantity, factor).also {
            require(it <= MAX_STOCK) { "Jumlah stok terlalu besar" }
        }
    }

    fun resolveUnitPrice(
        basePrice: Long,
        baseQuantity: Int,
        tiers: List<PriceTier>,
    ): Long {
        require(basePrice in 0..MoneyMath.MAX_MONEY) { "Harga tidak valid" }
        require(baseQuantity >= 0) { "Jumlah tidak boleh negatif" }
        tiers.forEach {
            require(it.minimumBaseQuantity > 0) { "Minimum harga bertingkat tidak valid" }
            require(it.unitPrice in 0..MoneyMath.MAX_MONEY) { "Harga bertingkat tidak valid" }
        }
        return tiers
            .filter { baseQuantity >= it.minimumBaseQuantity }
            .maxByOrNull { it.minimumBaseQuantity }
            ?.unitPrice
            ?: basePrice
    }

    fun adjustStock(
        current: Int,
        delta: Int,
        reason: String,
    ): Int {
        require(current in 0..MAX_STOCK) { "Stok saat ini tidak valid" }
        require(reason.isNotBlank()) { "Alasan penyesuaian wajib diisi" }
        return Math.addExact(current, delta).also {
            require(it in 0..MAX_STOCK) { "Stok tidak boleh negatif atau terlalu besar" }
        }
    }
}
