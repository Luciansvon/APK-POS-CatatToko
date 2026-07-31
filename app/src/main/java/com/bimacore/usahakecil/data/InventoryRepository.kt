package com.bimacore.usahakecil.data

import androidx.room.withTransaction
import com.bimacore.usahakecil.domain.BusinessCapabilities
import com.bimacore.usahakecil.domain.InventoryRules
import com.bimacore.usahakecil.domain.MoneyMath
import kotlinx.coroutines.flow.Flow

data class ProductDraft(
    val id: Long? = null,
    val categoryId: Long,
    val name: String,
    val basePrice: Long,
    val openingStock: Int,
    val stockTrackingEnabled: Boolean,
    val lowStockThreshold: Int,
    val unitLabel: String,
)

data class CategoryDraft(
    val id: Long? = null,
    val name: String,
    val iconKey: String = "inventory",
)

data class VariantDraft(
    val id: Long? = null,
    val productId: Long,
    val label: String,
    val priceOverride: Long?,
    val openingStock: Int,
)

class InventoryRepository(
    private val database: PosDatabase,
    private val capabilities: BusinessCapabilities,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    private val catalogDao = database.catalogDao()
    private val adminDao = database.inventoryAdminDao()

    val categories: Flow<List<CategoryEntity>> = catalogDao.observeCategories()
    val products: Flow<List<ProductEntity>> = catalogDao.observeProducts()
    val variants: Flow<List<ProductVariantEntity>> = catalogDao.observeVariants()
    val stockMovements: Flow<List<StockMovementEntity>> = adminDao.observeStockMovements()

    fun observeUnits(productId: Long): Flow<List<UnitConversionEntity>> =
        adminDao.observeUnits(productId)

    fun observePriceTiers(productId: Long): Flow<List<PriceTierEntity>> =
        adminDao.observePriceTiers(productId)

    suspend fun saveCategory(draft: CategoryDraft): Long {
        require(draft.name.isNotBlank()) { "Nama kategori wajib diisi" }
        val now = clock()
        val id = draft.id ?: catalogDao.nextCategoryId()
        val current = draft.id?.let { catalogDao.getCategory(it) }
        catalogDao.insertCategory(
            CategoryEntity(
                id = id,
                name = draft.name.trim(),
                iconKey = draft.iconKey.ifBlank { "inventory" },
                sortOrder = current?.sortOrder ?: id.toInt(),
                isActive = current?.isActive ?: true,
                updatedAt = now,
            ),
        )
        return id
    }

    suspend fun saveProduct(draft: ProductDraft): Long = database.withTransaction {
        require(draft.name.isNotBlank()) { "Nama produk wajib diisi" }
        require(draft.basePrice in 0..MoneyMath.MAX_MONEY) { "Harga jual tidak valid" }
        require(draft.openingStock in 0..InventoryRules.MAX_STOCK) { "Stok tidak valid" }
        require(draft.lowStockThreshold in 0..InventoryRules.MAX_STOCK) {
            "Batas stok menipis tidak valid"
        }
        require(draft.unitLabel.isNotBlank()) { "Satuan wajib diisi" }
        require(catalogDao.getCategory(draft.categoryId) != null) { "Kategori tidak tersedia" }

        val now = clock()
        val id = draft.id ?: catalogDao.nextProductId()
        val current = draft.id?.let { catalogDao.getProduct(it) }
        val newStock = current?.stock ?: draft.openingStock
        catalogDao.insertProduct(
            ProductEntity(
                id = id,
                categoryId = draft.categoryId,
                name = draft.name.trim(),
                basePrice = draft.basePrice,
                stock = newStock,
                stockTrackingEnabled = draft.stockTrackingEnabled,
                hasVariants = current?.hasVariants ?: false,
                lowStockThreshold = draft.lowStockThreshold,
                imageUri = current?.imageUri,
                sortOrder = current?.sortOrder ?: id.toInt(),
                isActive = current?.isActive ?: true,
                unitLabel = draft.unitLabel.trim(),
                updatedAt = now,
            ),
        )
        if (current == null && draft.stockTrackingEnabled && draft.openingStock > 0) {
            adminDao.insertStockMovement(
                StockMovementEntity(
                    productId = id,
                    variantId = null,
                    saleId = 0,
                    type = "OPENING",
                    quantityDelta = draft.openingStock,
                    reason = "Stok awal produk",
                    createdAt = now,
                    referenceType = "PRODUCT",
                    referenceId = id,
                    unitLabel = draft.unitLabel.trim(),
                    baseQuantityDelta = draft.openingStock,
                ),
            )
        }
        id
    }

    suspend fun saveVariant(draft: VariantDraft): Long = database.withTransaction {
        require(draft.label.isNotBlank()) { "Nama varian wajib diisi" }
        require(draft.openingStock in 0..InventoryRules.MAX_STOCK) { "Stok varian tidak valid" }
        draft.priceOverride?.let {
            require(it in 0..MoneyMath.MAX_MONEY) { "Harga varian tidak valid" }
        }
        val product = requireNotNull(catalogDao.getProduct(draft.productId)) {
            "Produk tidak tersedia"
        }
        val now = clock()
        val id = draft.id ?: catalogDao.nextVariantId()
        val current = draft.id?.let { catalogDao.getVariant(it) }
        catalogDao.insertVariant(
            ProductVariantEntity(
                id = id,
                productId = draft.productId,
                label = draft.label.trim(),
                priceOverride = draft.priceOverride,
                stock = current?.stock ?: draft.openingStock,
                sortOrder = current?.sortOrder ?: id.toInt(),
                isActive = current?.isActive ?: true,
                updatedAt = now,
            ),
        )
        if (!product.hasVariants) {
            catalogDao.updateProduct(product.copy(hasVariants = true, updatedAt = now))
        }
        if (current == null && draft.openingStock > 0) {
            adminDao.insertStockMovement(
                StockMovementEntity(
                    productId = product.id,
                    variantId = id,
                    saleId = 0,
                    type = "OPENING",
                    quantityDelta = draft.openingStock,
                    reason = "Stok awal varian",
                    createdAt = now,
                    referenceType = "VARIANT",
                    referenceId = id,
                    unitLabel = product.unitLabel,
                    baseQuantityDelta = draft.openingStock,
                ),
            )
        }
        id
    }

    suspend fun setProductActive(productId: Long, active: Boolean) {
        val product = requireNotNull(catalogDao.getProduct(productId)) { "Produk tidak tersedia" }
        catalogDao.updateProduct(product.copy(isActive = active, updatedAt = clock()))
    }

    suspend fun setVariantActive(variantId: Long, active: Boolean) {
        val variant = requireNotNull(catalogDao.getVariant(variantId)) { "Varian tidak tersedia" }
        catalogDao.updateVariant(variant.copy(isActive = active, updatedAt = clock()))
    }

    suspend fun adjustStock(
        productId: Long,
        variantId: Long?,
        delta: Int,
        type: String,
        reason: String,
        unitLabel: String? = null,
        factorToBase: Int = 1,
    ) = database.withTransaction {
        require(type in STOCK_TYPES) { "Jenis pergerakan stok tidak valid" }
        val product = requireNotNull(catalogDao.getProduct(productId)) { "Produk tidak tersedia" }
        require(!product.hasVariants || variantId != null) {
            "Produk bervarian wajib memilih varian untuk penyesuaian stok"
        }
        val baseDelta = InventoryRules.toBaseQuantity(kotlin.math.abs(delta), factorToBase) *
            if (delta < 0) -1 else 1
        if (variantId != null) {
            val variant = requireNotNull(catalogDao.getVariant(variantId)) { "Varian tidak tersedia" }
            require(variant.productId == productId) { "Varian tidak sesuai produk" }
            val next = InventoryRules.adjustStock(variant.stock, baseDelta, reason)
            catalogDao.updateVariant(variant.copy(stock = next, updatedAt = clock()))
        } else {
            val next = InventoryRules.adjustStock(product.stock, baseDelta, reason)
            catalogDao.updateProduct(product.copy(stock = next, updatedAt = clock()))
        }
        adminDao.insertStockMovement(
            StockMovementEntity(
                productId = productId,
                variantId = variantId,
                saleId = 0,
                type = type,
                quantityDelta = delta,
                reason = reason.trim(),
                createdAt = clock(),
                referenceType = "MANUAL",
                referenceId = null,
                unitLabel = unitLabel?.trim().takeUnless { it.isNullOrBlank() } ?: product.unitLabel,
                baseQuantityDelta = baseDelta,
            ),
        )
    }

    suspend fun saveUnit(
        id: Long?,
        productId: Long,
        label: String,
        factorToBase: Int,
        salePrice: Long,
    ): Long {
        require(capabilities.multiUnit) { "Multi-satuan tidak aktif pada APK ini" }
        require(catalogDao.getProduct(productId) != null) { "Produk tidak tersedia" }
        require(label.isNotBlank()) { "Nama satuan wajib diisi" }
        InventoryRules.toBaseQuantity(1, factorToBase)
        require(salePrice in 0..MoneyMath.MAX_MONEY) { "Harga satuan tidak valid" }
        val now = clock()
        return if (id == null) {
            adminDao.insertUnit(
                UnitConversionEntity(
                    productId = productId,
                    label = label.trim(),
                    factorToBase = factorToBase,
                    salePrice = salePrice,
                    createdAt = now,
                    updatedAt = now,
                ),
            )
        } else {
            val current = requireNotNull(adminDao.getUnit(id)) { "Satuan tidak tersedia" }
            adminDao.updateUnit(
                current.copy(
                    label = label.trim(),
                    factorToBase = factorToBase,
                    salePrice = salePrice,
                    updatedAt = now,
                ),
            )
            id
        }
    }

    suspend fun savePriceTier(
        id: Long?,
        productId: Long,
        minimumBaseQuantity: Int,
        unitPrice: Long,
    ): Long {
        require(capabilities.tierPricing) { "Harga bertingkat tidak aktif pada APK ini" }
        require(catalogDao.getProduct(productId) != null) { "Produk tidak tersedia" }
        InventoryRules.resolveUnitPrice(
            basePrice = unitPrice,
            baseQuantity = minimumBaseQuantity,
            tiers = listOf(PriceTierEntity(0, productId, minimumBaseQuantity, unitPrice, 0, 0).toDomain()),
        )
        val now = clock()
        return if (id == null) {
            adminDao.insertPriceTier(
                PriceTierEntity(
                    productId = productId,
                    minimumBaseQuantity = minimumBaseQuantity,
                    unitPrice = unitPrice,
                    createdAt = now,
                    updatedAt = now,
                ),
            )
        } else {
            val current = adminDao.getPriceTiers(productId).firstOrNull { it.id == id }
                ?: throw IllegalArgumentException("Harga bertingkat tidak tersedia")
            adminDao.updatePriceTier(
                current.copy(
                    minimumBaseQuantity = minimumBaseQuantity,
                    unitPrice = unitPrice,
                    updatedAt = now,
                ),
            )
            id
        }
    }

    companion object {
        private val STOCK_TYPES = setOf(
            "STOCK_IN",
            "STOCK_OUT",
            "ADJUSTMENT_IN",
            "ADJUSTMENT_OUT",
            "DAMAGED",
            "LOST",
        )
    }
}

private fun PriceTierEntity.toDomain() = com.bimacore.usahakecil.domain.PriceTier(
    minimumBaseQuantity = minimumBaseQuantity,
    unitPrice = unitPrice,
)
