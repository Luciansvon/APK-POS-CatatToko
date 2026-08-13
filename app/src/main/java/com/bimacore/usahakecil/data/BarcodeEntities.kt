package com.bimacore.usahakecil.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "product_barcodes",
    foreignKeys = [
        ForeignKey(
            entity = ProductEntity::class,
            parentColumns = ["id"],
            childColumns = ["productId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = ProductVariantEntity::class,
            parentColumns = ["id"],
            childColumns = ["variantId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = UnitConversionEntity::class,
            parentColumns = ["id"],
            childColumns = ["unitId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["barcode"], unique = true),
        Index("productId"),
        Index("variantId"),
        Index("unitId"),
    ],
)
data class ProductBarcodeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val barcode: String,
    val productId: Long,
    val variantId: Long? = null,
    val unitId: Long? = null,
    @ColumnInfo(defaultValue = "1") val isActive: Boolean = true,
    val createdAt: Long,
    val updatedAt: Long,
)
