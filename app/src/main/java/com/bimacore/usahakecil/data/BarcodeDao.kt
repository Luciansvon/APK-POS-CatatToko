package com.bimacore.usahakecil.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface BarcodeDao {
    @Query("SELECT * FROM product_barcodes ORDER BY isActive DESC, barcode")
    fun observeAll(): Flow<List<ProductBarcodeEntity>>

    @Query("SELECT * FROM product_barcodes WHERE id = :id")
    suspend fun getById(id: Long): ProductBarcodeEntity?

    @Query("SELECT * FROM product_barcodes WHERE barcode = :barcode LIMIT 1")
    suspend fun getByBarcode(barcode: String): ProductBarcodeEntity?

    @Query("SELECT * FROM product_barcodes WHERE productId = :productId AND variantId IS NULL AND unitId IS NULL ORDER BY isActive DESC, id LIMIT 1")
    suspend fun getBaseForProduct(productId: Long): ProductBarcodeEntity?

    @Query("SELECT * FROM product_barcodes WHERE productId = :productId ORDER BY isActive DESC, barcode")
    fun observeForProduct(productId: Long): Flow<List<ProductBarcodeEntity>>

    @Insert
    suspend fun insert(item: ProductBarcodeEntity): Long

    @Update
    suspend fun update(item: ProductBarcodeEntity)
}
