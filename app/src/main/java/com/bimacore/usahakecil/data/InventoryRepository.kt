package com.bimacore.usahakecil.data

import androidx.room.withTransaction
import com.bimacore.usahakecil.domain.BusinessCapabilities
import com.bimacore.usahakecil.domain.InventoryRules
import com.bimacore.usahakecil.domain.MoneyMath
import com.bimacore.usahakecil.domain.normalizeBarcode
import com.bimacore.usahakecil.security.ReportSession
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
    val imageUri: String? = null,
    val barcode: String? = null,
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
    private val ownerSession: ReportSession,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    private val catalogDao = database.catalogDao()
    private val adminDao = database.inventoryAdminDao()
    private val barcodeDao = database.barcodeDao()

    val categories: Flow<List<CategoryEntity>> = catalogDao.observeCategories()
        .ownerOnly(ownerSession, emptyList())
    val products: Flow<List<ProductEntity>> = catalogDao.observeProducts()
        .ownerOnly(ownerSession, emptyList())
    val variants: Flow<List<ProductVariantEntity>> = catalogDao.observeVariants()
        .ownerOnly(ownerSession, emptyList())
    val stockMovements: Flow<List<StockMovementEntity>> = adminDao.observeStockMovements()
        .ownerOnly(ownerSession, emptyList())
    val allUnits: Flow<List<UnitConversionEntity>> = adminDao.observeAllUnits()
        .ownerOnly(ownerSession, emptyList())

    fun observeUnits(productId: Long): Flow<List<UnitConversionEntity>> =
        adminDao.observeUnits(productId).ownerOnly(ownerSession, emptyList())

    fun observePriceTiers(productId: Long): Flow<List<PriceTierEntity>> =
        adminDao.observePriceTiers(productId).ownerOnly(ownerSession, emptyList())

    suspend fun saveCategory(draft: CategoryDraft): Long = database.withTransaction {
        ownerSession.requireOwner()
        require(draft.name.isNotBlank()) { "Nama kategori wajib diisi" }
        val now = clock()
        val current = draft.id?.let { catalogDao.getCategory(it) }
        if (draft.id != null) {
            requireNotNull(current) { "Kategori tidak tersedia" }
        }
        val id = draft.id ?: catalogDao.nextCategoryId()
        val category = CategoryEntity(
            id = id,
            name = draft.name.trim(),
            iconKey = draft.iconKey.ifBlank { "inventory" },
            sortOrder = current?.sortOrder ?: id.toInt(),
            isActive = current?.isActive ?: true,
            updatedAt = now,
        )
        if (current == null) catalogDao.insertCategory(category) else catalogDao.updateCategory(category)
        id
    }

    suspend fun saveProduct(draft: ProductDraft): Long = database.withTransaction {
        ownerSession.requireOwner()
        require(draft.name.isNotBlank()) { "Nama produk wajib diisi" }
        require(draft.basePrice in 0..MoneyMath.MAX_MONEY) { "Harga jual tidak valid" }
        require(draft.openingStock in 0..InventoryRules.MAX_STOCK) { "Stok tidak valid" }
        require(draft.lowStockThreshold in 0..InventoryRules.MAX_STOCK) {
            "Batas stok menipis tidak valid"
        }
        require(draft.unitLabel.isNotBlank()) { "Satuan wajib diisi" }
        val category = requireNotNull(catalogDao.getCategory(draft.categoryId)) {
            "Kategori tidak tersedia"
        }
        require(category.isActive) { "Kategori sudah tidak aktif" }

        val now = clock()
        val current = draft.id?.let { catalogDao.getProduct(it) }
        if (draft.id != null) {
            requireNotNull(current) { "Produk tidak tersedia" }
        }
        val id = draft.id ?: catalogDao.nextProductId()
        val newStock = current?.stock ?: draft.openingStock
        val imageUri = when {
            draft.imageUri == null -> current?.imageUri
            else -> draft.imageUri.trim().takeIf(String::isNotBlank)
        }
        val product = ProductEntity(
            id = id,
            categoryId = draft.categoryId,
            name = draft.name.trim(),
            basePrice = draft.basePrice,
            stock = newStock,
            stockTrackingEnabled = draft.stockTrackingEnabled,
            hasVariants = current?.hasVariants ?: false,
            lowStockThreshold = draft.lowStockThreshold,
            imageUri = imageUri,
            sortOrder = current?.sortOrder ?: id.toInt(),
            isActive = current?.isActive ?: true,
            unitLabel = draft.unitLabel.trim(),
            updatedAt = now,
        )
        if (current == null) catalogDao.insertProduct(product) else catalogDao.updateProduct(product)
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
        draft.barcode?.let { rawBarcode ->
            require(capabilities.barcodeScanner) { "Barcode belum aktif pada APK ini" }
            val existing = barcodeDao.getBaseForProduct(id)
            if (rawBarcode.isBlank()) {
                if (existing?.isActive == true) {
                    barcodeDao.update(existing.copy(isActive = false, updatedAt = now))
                }
            } else {
                val barcode = normalizeBarcode(rawBarcode)
                val duplicate = barcodeDao.getByBarcode(barcode)
                require(duplicate == null || duplicate.id == existing?.id) {
                    "Barcode sudah terdaftar"
                }
                if (existing == null) {
                    barcodeDao.insert(
                        ProductBarcodeEntity(
                            barcode = barcode,
                            productId = id,
                            createdAt = now,
                            updatedAt = now,
                        ),
                    )
                } else {
                    barcodeDao.update(
                        existing.copy(
                            barcode = barcode,
                            isActive = true,
                            updatedAt = now,
                        ),
                    )
                }
            }
        }
        id
    }

    suspend fun saveVariant(draft: VariantDraft): Long = database.withTransaction {
        ownerSession.requireOwner()
        require(draft.label.isNotBlank()) { "Nama varian wajib diisi" }
        require(draft.openingStock in 0..InventoryRules.MAX_STOCK) { "Stok varian tidak valid" }
        draft.priceOverride?.let {
            require(it in 0..MoneyMath.MAX_MONEY) { "Harga varian tidak valid" }
        }
        val product = requireNotNull(catalogDao.getProduct(draft.productId)) {
            "Produk tidak tersedia"
        }
        require(product.isActive) { "Produk sudah tidak aktif" }
        val now = clock()
        val current = draft.id?.let { catalogDao.getVariant(it) }
        if (draft.id != null) {
            requireNotNull(current) { "Varian tidak tersedia" }
            require(current.productId == product.id) { "Varian tidak sesuai produk" }
        } else {
            require(product.stock == 0) {
                "Kosongkan stok produk sebelum menambahkan varian agar stok tidak hilang"
            }
        }
        val id = draft.id ?: catalogDao.nextVariantId()
        val variant = ProductVariantEntity(
            id = id,
            productId = draft.productId,
            label = draft.label.trim(),
            priceOverride = draft.priceOverride,
            stock = current?.stock ?: draft.openingStock,
            sortOrder = current?.sortOrder ?: id.toInt(),
            isActive = current?.isActive ?: true,
            updatedAt = now,
        )
        if (current == null) catalogDao.insertVariant(variant) else catalogDao.updateVariant(variant)
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
        ownerSession.requireOwner()
        val product = requireNotNull(catalogDao.getProduct(productId)) { "Produk tidak tersedia" }
        if (active) {
            require(catalogDao.getCategory(product.categoryId)?.isActive == true) {
                "Aktifkan kategori produk terlebih dahulu"
            }
        }
        catalogDao.updateProduct(product.copy(isActive = active, updatedAt = clock()))
    }

    suspend fun setCategoryActive(categoryId: Long, active: Boolean) {
        ownerSession.requireOwner()
        val category = requireNotNull(catalogDao.getCategory(categoryId)) { "Kategori tidak tersedia" }
        if (!active) {
            require(catalogDao.activeProductCountForCategory(categoryId) == 0) {
                "Arsipkan semua produk aktif di kategori ini terlebih dahulu"
            }
        }
        catalogDao.updateCategory(category.copy(isActive = active, updatedAt = clock()))
    }

    suspend fun setVariantActive(variantId: Long, active: Boolean) {
        ownerSession.requireOwner()
        val variant = requireNotNull(catalogDao.getVariant(variantId)) { "Varian tidak tersedia" }
        if (active) {
            val product = requireNotNull(catalogDao.getProduct(variant.productId)) {
                "Produk varian tidak tersedia"
            }
            require(product.isActive) { "Aktifkan produk terlebih dahulu" }
        } else {
            require(catalogDao.activeVariantCountForProduct(variant.productId) > 1) {
                "Arsipkan produk jika varian terakhir tidak dipakai"
            }
        }
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
        ownerSession.requireOwner()
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
        ownerSession.requireOwner()
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
            require(current.productId == productId) { "Satuan tidak sesuai produk" }
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
        ownerSession.requireOwner()
        require(capabilities.tierPricing) { "Harga bertingkat tidak aktif pada APK ini" }
        require(catalogDao.getProduct(productId) != null) { "Produk tidak tersedia" }
        require(minimumBaseQuantity > 0) { "Batas jumlah minimal harus lebih dari nol" }
        require(unitPrice in 0..MoneyMath.MAX_MONEY) { "Harga bertingkat tidak valid" }
        val duplicate = adminDao.getPriceTiers(productId).firstOrNull {
            it.id != id && it.minimumBaseQuantity == minimumBaseQuantity
        }
        require(duplicate == null) { "Batas jumlah harga bertingkat sudah ada" }
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
