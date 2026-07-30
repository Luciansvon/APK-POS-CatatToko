package com.bimacore.usahakecil.domain

enum class OrderStatus {
    NEW,
    PROCESSING,
    READY,
    COMPLETED,
}

object CulinaryRules {
    fun canMove(
        current: OrderStatus,
        next: OrderStatus,
    ): Boolean = next.ordinal == current.ordinal + 1

    fun ingredientQuantity(
        quantityPerMenu: Int,
        menuQuantity: Int,
    ): Int {
        require(quantityPerMenu > 0) { "Jumlah bahan harus lebih dari nol" }
        require(menuQuantity > 0) { "Jumlah menu harus lebih dari nol" }
        return Math.multiplyExact(quantityPerMenu, menuQuantity).also {
            require(it <= InventoryRules.MAX_STOCK) { "Jumlah bahan terlalu besar" }
        }
    }

    fun toppingTotal(
        toppingPrices: List<Long>,
        menuQuantity: Int,
    ): Long {
        require(menuQuantity in 1..MoneyMath.MAX_QUANTITY) { "Jumlah menu tidak valid" }
        val perMenu = toppingPrices.fold(0L) { current, price ->
            require(price in 0..MoneyMath.MAX_MONEY) { "Harga topping tidak valid" }
            Math.addExact(current, price).also {
                require(it <= MoneyMath.MAX_MONEY) { "Harga topping terlalu besar" }
            }
        }
        return MoneyMath.multiply(perMenu, menuQuantity)
    }
}
