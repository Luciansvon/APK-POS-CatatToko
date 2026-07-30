package com.bimacore.usahakecil.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface CatalogDao {
    @Query("SELECT * FROM categories ORDER BY sortOrder, name")
    fun observeCategories(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM products ORDER BY sortOrder, name")
    fun observeProducts(): Flow<List<ProductEntity>>

    @Query("SELECT * FROM product_variants ORDER BY sortOrder, label")
    fun observeVariants(): Flow<List<ProductVariantEntity>>

    @Query("SELECT * FROM products WHERE id = :id")
    suspend fun getProduct(id: Long): ProductEntity?

    @Query("SELECT * FROM categories WHERE id = :id")
    suspend fun getCategory(id: Long): CategoryEntity?

    @Query("SELECT * FROM product_variants WHERE id = :id")
    suspend fun getVariant(id: Long): ProductVariantEntity?

    @Query("SELECT COUNT(*) FROM products")
    suspend fun productCount(): Int

    @Query("SELECT COALESCE(MAX(id), 0) + 1 FROM categories")
    suspend fun nextCategoryId(): Long

    @Query("SELECT COALESCE(MAX(id), 0) + 1 FROM products")
    suspend fun nextProductId(): Long

    @Query("SELECT COALESCE(MAX(id), 0) + 1 FROM product_variants")
    suspend fun nextVariantId(): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategories(items: List<CategoryEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProducts(items: List<ProductEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVariants(items: List<ProductVariantEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(item: CategoryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(item: ProductEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVariant(item: ProductVariantEntity)

    @Update
    suspend fun updateCategory(category: CategoryEntity)

    @Update
    suspend fun updateProduct(product: ProductEntity)

    @Update
    suspend fun updateVariant(variant: ProductVariantEntity)
}

@Dao
interface CartDao {
    @Query("SELECT * FROM cart_lines ORDER BY updatedAt")
    fun observeLines(): Flow<List<CartLineEntity>>

    @Query("SELECT * FROM cart_lines ORDER BY updatedAt")
    suspend fun getLines(): List<CartLineEntity>

    @Query("SELECT * FROM cart_lines WHERE id = :lineId")
    suspend fun getLine(lineId: String): CartLineEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertLine(line: CartLineEntity)

    @Query("DELETE FROM cart_lines WHERE id = :lineId")
    suspend fun deleteLine(lineId: String)

    @Query("DELETE FROM cart_lines")
    suspend fun clearLines()

    @Query("SELECT * FROM draft_cart WHERE id = 1")
    suspend fun getDraft(): DraftCartEntity?

    @Query("SELECT * FROM draft_cart WHERE id = 1")
    fun observeDraft(): Flow<DraftCartEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDraft(draft: DraftCartEntity)
}

@Dao
interface SaleDao {
    @Insert
    suspend fun insertSale(sale: SaleEntity): Long

    @Insert
    suspend fun insertItems(items: List<SaleItemEntity>): List<Long>

    @Query("SELECT * FROM sales WHERE id = :saleId")
    suspend fun getSale(saleId: Long): SaleEntity?

    @Query("SELECT * FROM sale_items WHERE saleId = :saleId ORDER BY id")
    suspend fun getItems(saleId: Long): List<SaleItemEntity>

    @Query("SELECT * FROM sales ORDER BY createdAt DESC")
    fun observeSales(): Flow<List<SaleEntity>>

    @Query("SELECT * FROM sales WHERE orderStatus != 'COMPLETED' ORDER BY createdAt")
    fun observeOpenOrders(): Flow<List<SaleEntity>>

    @Query("SELECT * FROM sales WHERE createdAt BETWEEN :fromInclusive AND :toInclusive ORDER BY createdAt DESC")
    suspend fun getSalesBetween(fromInclusive: Long, toInclusive: Long): List<SaleEntity>

    @Update
    suspend fun updateSale(sale: SaleEntity)
}

@Dao
interface StockDao {
    @Insert
    suspend fun insertMovements(items: List<StockMovementEntity>)

    @Query("SELECT * FROM stock_movements WHERE saleId = :saleId ORDER BY id")
    suspend fun getMovementsForSale(saleId: Long): List<StockMovementEntity>
}
