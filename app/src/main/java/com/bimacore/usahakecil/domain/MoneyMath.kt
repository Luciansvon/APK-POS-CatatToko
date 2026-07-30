package com.bimacore.usahakecil.domain

object MoneyMath {
    const val MAX_MONEY = 999_999_999_999L
    const val MAX_QUANTITY = 9_999

    fun multiply(unitPrice: Long, quantity: Int): Long {
        require(unitPrice in 0..MAX_MONEY)
        require(quantity in 0..MAX_QUANTITY)
        return Math.multiplyExact(unitPrice, quantity.toLong()).also {
            require(it <= MAX_MONEY)
        }
    }

    fun total(items: Iterable<Pair<Long, Int>>): Long =
        items.fold(0L) { current, (unitPrice, quantity) ->
            Math.addExact(current, multiply(unitPrice, quantity)).also {
                require(it <= MAX_MONEY)
            }
        }

    fun change(total: Long, amountReceived: Long): Long =
        if (amountReceived >= total) amountReceived - total else 0L

    fun shortage(total: Long, amountReceived: Long): Long =
        if (amountReceived < total) total - amountReceived else 0L

    fun quickCashAmounts(total: Long): List<Long> {
        if (total <= 0) return emptyList()
        val steps = listOf(10_000L, 50_000L, 100_000L)
        return buildList {
            add(total)
            steps.forEach { step ->
                val rounded = ((total + step - 1) / step) * step
                if (rounded <= MAX_MONEY && rounded !in this) add(rounded)
            }
        }.take(4)
    }
}
