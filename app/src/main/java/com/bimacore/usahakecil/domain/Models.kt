package com.bimacore.usahakecil.domain

enum class BusinessType {
    RETAIL,
    WHOLESALE,
    CULINARY,
}

enum class PaymentMethod {
    CASH,
    QRIS,
    TRANSFER,
    CREDIT,
}

data class Category(
    val id: Long,
    val name: String,
    val iconKey: String,
)

data class Product(
    val id: Long,
    val categoryId: Long,
    val name: String,
    val basePrice: Long,
    val stock: Int,
    val stockTrackingEnabled: Boolean,
    val hasVariants: Boolean,
    val lowStockThreshold: Int,
    val imageUri: String?,
)

data class ProductVariant(
    val id: Long,
    val productId: Long,
    val label: String,
    val priceOverride: Long?,
    val stock: Int,
)

data class CartItem(
    val lineId: String,
    val productId: Long,
    val variantId: Long?,
    val productName: String,
    val variantName: String?,
    val categoryName: String,
    val unitPrice: Long,
    val quantity: Int,
    val availableStock: Int?,
    val unitLabel: String = "pcs",
    val factorToBase: Int = 1,
    val note: String = "",
    val toppings: List<CartTopping> = emptyList(),
) {
    val subtotal: Long
        get() = MoneyMath.multiply(unitPrice, quantity)
}

data class CartTopping(
    val toppingId: Long,
    val name: String,
    val unitPrice: Long,
    val quantityPerItem: Int,
)

data class ReceiptItem(
    val productName: String,
    val variantName: String?,
    val quantity: Int,
    val unitPrice: Long,
    val subtotal: Long,
    val unitLabel: String = "pcs",
    val note: String = "",
    val toppingNames: List<String> = emptyList(),
)

data class Receipt(
    val saleId: Long,
    val receiptNumber: String,
    val businessName: String,
    val createdAt: Long,
    val paymentMethod: PaymentMethod,
    val total: Long,
    val amountReceived: Long,
    val changeAmount: Long,
    val items: List<ReceiptItem>,
)

sealed interface AddToCartResult {
    data object Added : AddToCartResult
    data object VariantRequired : AddToCartResult
    data object OutOfStock : AddToCartResult
    data object CompletedTransactionLocked : AddToCartResult
}

sealed interface CheckoutResult {
    data class Success(val receipt: Receipt) : CheckoutResult
    data class Error(val message: String) : CheckoutResult
}
