package com.bimacore.usahakecil.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey val id: Long,
    val name: String,
    val iconKey: String,
    val sortOrder: Int,
    @ColumnInfo(defaultValue = "1") val isActive: Boolean = true,
    @ColumnInfo(defaultValue = "0") val updatedAt: Long = 0,
)

@Entity(
    tableName = "products",
    indices = [Index("categoryId")],
)
data class ProductEntity(
    @PrimaryKey val id: Long,
    val categoryId: Long,
    val name: String,
    val basePrice: Long,
    val stock: Int,
    val stockTrackingEnabled: Boolean,
    val hasVariants: Boolean,
    val lowStockThreshold: Int,
    val imageUri: String?,
    val sortOrder: Int,
    @ColumnInfo(defaultValue = "1") val isActive: Boolean = true,
    @ColumnInfo(defaultValue = "'pcs'") val unitLabel: String = "pcs",
    @ColumnInfo(defaultValue = "0") val updatedAt: Long = 0,
)

@Entity(
    tableName = "product_variants",
    indices = [Index("productId")],
)
data class ProductVariantEntity(
    @PrimaryKey val id: Long,
    val productId: Long,
    val label: String,
    val priceOverride: Long?,
    val stock: Int,
    val sortOrder: Int,
    @ColumnInfo(defaultValue = "1") val isActive: Boolean = true,
    @ColumnInfo(defaultValue = "0") val updatedAt: Long = 0,
)

@Entity(tableName = "draft_cart")
data class DraftCartEntity(
    @PrimaryKey val id: Int = 1,
    val completedSaleId: Long? = null,
    val updatedAt: Long,
)

@Entity(
    tableName = "cart_lines",
    indices = [Index("productId"), Index("variantId")],
)
data class CartLineEntity(
    @PrimaryKey val id: String,
    val productId: Long,
    val variantId: Long?,
    val quantity: Int,
    val updatedAt: Long,
)

@Entity(
    tableName = "sales",
    indices = [Index(value = ["receiptNumber"], unique = true), Index("shiftId")],
)
data class SaleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val receiptNumber: String,
    val businessName: String,
    val createdAt: Long,
    val paymentMethod: String,
    val total: Long,
    val amountReceived: Long,
    val changeAmount: Long,
    val customerId: Long? = null,
    @ColumnInfo(defaultValue = "'PAID'") val settlementStatus: String = "PAID",
    @ColumnInfo(defaultValue = "'COMPLETED'") val orderStatus: String = "COMPLETED",
    @ColumnInfo(defaultValue = "''") val note: String = "",
    @ColumnInfo(defaultValue = "0") val updatedAt: Long = 0,
    val shiftId: Long? = null,
)

@Entity(
    tableName = "sale_items",
    indices = [Index("saleId")],
)
data class SaleItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val saleId: Long,
    val productId: Long,
    val variantId: Long?,
    val productName: String,
    val variantName: String?,
    val categoryName: String,
    val unitPrice: Long,
    val quantity: Int,
    val subtotal: Long,
    @ColumnInfo(defaultValue = "1") val baseQuantity: Int = quantity,
    @ColumnInfo(defaultValue = "'pcs'") val unitLabel: String = "pcs",
    @ColumnInfo(defaultValue = "''") val note: String = "",
)

@Entity(
    tableName = "stock_movements",
    indices = [
        Index("productId"),
        Index("variantId"),
        Index("saleId"),
    ],
)
data class StockMovementEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val productId: Long,
    val variantId: Long?,
    val saleId: Long,
    val type: String,
    val quantityDelta: Int,
    val reason: String,
    val createdAt: Long,
    @ColumnInfo(defaultValue = "'SALE'") val referenceType: String = "SALE",
    val referenceId: Long? = null,
    @ColumnInfo(defaultValue = "'pcs'") val unitLabel: String = "pcs",
    @ColumnInfo(defaultValue = "0") val baseQuantityDelta: Int = quantityDelta,
)
