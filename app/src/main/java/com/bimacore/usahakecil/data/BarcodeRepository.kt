package com.bimacore.usahakecil.data

import androidx.room.withTransaction
import com.bimacore.usahakecil.domain.BarcodeLookupResult
import com.bimacore.usahakecil.domain.BarcodeTarget
import com.bimacore.usahakecil.domain.BusinessCapabilities
import com.bimacore.usahakecil.domain.Product
import com.bimacore.usahakecil.domain.normalizeBarcode
import com.bimacore.usahakecil.security.ReportSession
import kotlinx.coroutines.flow.Flow

data class ProductBarcodeDraft(
    val id: Long? = null,
    val barcode: String,
    val productId: Long,
    val variantId: Long? = null,
    val unitId: Long? = null,
)

class BarcodeRepository(
    private val database: PosDatabase,
    private val capabilities: BusinessCapabilities,
    private val ownerSession: ReportSession,
    private val databaseOperations: DatabaseOperationCoordinator = DatabaseOperationCoordinator(),
    private val clock: () -> Long = System::currentTimeMillis,
) {
    private val barcodeDao = database.barcodeDao()
    private val catalogDao = database.catalogDao()
    private val inventoryDao = database.inventoryAdminDao()

    val mappings: Flow<List<ProductBarcodeEntity>> = barcodeDao.observeAll()
        .ownerOnly(ownerSession, emptyList())

    fun observeForProduct(productId: Long): Flow<List<ProductBarcodeEntity>> =
        barcodeDao.observeForProduct(productId).ownerOnly(ownerSession, emptyList())

    suspend fun lookup(rawBarcode: String): BarcodeLookupResult = databaseOperations.withOperation {
        if (!capabilities.barcodeScanner) return@withOperation BarcodeLookupResult.Unsupported
        val barcode = normalizeBarcode(rawBarcode)
        val mapping = barcodeDao.getByBarcode(barcode) ?: return@withOperation BarcodeLookupResult.NotFound
        if (!mapping.isActive) return@withOperation BarcodeLookupResult.Inactive
        val product = catalogDao.getProduct(mapping.productId)
            ?.takeIf(ProductEntity::isActive)
            ?: return@withOperation BarcodeLookupResult.Inactive
        val variant = mapping.variantId?.let { catalogDao.getVariant(it) }
        if (
            mapping.variantId != null &&
            (variant == null || !variant.isActive || variant.productId != product.id)
        ) {
            return@withOperation BarcodeLookupResult.Inactive
        }
        val unit = mapping.unitId?.let { inventoryDao.getUnit(it) }
        if (
            mapping.unitId != null &&
            (!capabilities.multiUnit || unit == null || !unit.isActive || unit.productId != product.id)
        ) {
            return@withOperation BarcodeLookupResult.Inactive
        }
        BarcodeLookupResult.Found(
            BarcodeTarget(
                barcode = barcode,
                product = product.toDomain(),
                variantId = variant?.id,
                unitId = unit?.id,
            ),
        )
    }

    suspend fun save(draft: ProductBarcodeDraft): Long = databaseOperations.withOperation {
        database.withTransaction {
            ownerSession.requireOwner()
            require(capabilities.barcodeScanner) { "Barcode belum aktif pada APK ini" }
            val barcode = normalizeBarcode(draft.barcode)
            val product = requireNotNull(catalogDao.getProduct(draft.productId)) {
                "Produk tidak tersedia"
            }
            require(product.isActive) { "Produk sudah tidak aktif" }
            draft.variantId?.let { variantId ->
                val variant = requireNotNull(catalogDao.getVariant(variantId)) {
                    "Varian tidak tersedia"
                }
                require(variant.isActive && variant.productId == product.id) {
                    "Varian tidak sesuai dengan produk"
                }
            }
            draft.unitId?.let { unitId ->
                require(capabilities.multiUnit) { "Satuan grosir tidak aktif pada APK ini" }
                val unit = requireNotNull(inventoryDao.getUnit(unitId)) { "Satuan tidak tersedia" }
                require(unit.isActive && unit.productId == product.id) {
                    "Satuan tidak sesuai dengan produk"
                }
            }
            val duplicate = barcodeDao.getByBarcode(barcode)
            require(duplicate == null || duplicate.id == draft.id) { "Barcode sudah terdaftar" }

            val now = clock()
            val current = draft.id?.let { barcodeDao.getById(it) }
            if (draft.id == null) {
                barcodeDao.insert(
                    ProductBarcodeEntity(
                        barcode = barcode,
                        productId = product.id,
                        variantId = draft.variantId,
                        unitId = draft.unitId,
                        createdAt = now,
                        updatedAt = now,
                    ),
                )
            } else {
                val existing = requireNotNull(current) { "Barcode tidak tersedia" }
                barcodeDao.update(
                    existing.copy(
                        barcode = barcode,
                        productId = product.id,
                        variantId = draft.variantId,
                        unitId = draft.unitId,
                        updatedAt = now,
                    ),
                )
                existing.id
            }
        }
    }

    suspend fun setActive(id: Long, active: Boolean) = databaseOperations.withOperation {
        database.withTransaction {
            ownerSession.requireOwner()
            require(capabilities.barcodeScanner) { "Barcode belum aktif pada APK ini" }
            val current = requireNotNull(barcodeDao.getById(id)) { "Barcode tidak tersedia" }
            if (active) validateTarget(current)
            barcodeDao.update(current.copy(isActive = active, updatedAt = clock()))
        }
    }

    private suspend fun validateTarget(mapping: ProductBarcodeEntity) {
        val product = requireNotNull(catalogDao.getProduct(mapping.productId)) {
            "Produk tidak tersedia"
        }
        require(product.isActive) { "Produk sudah tidak aktif" }
        mapping.variantId?.let { variantId ->
            val variant = requireNotNull(catalogDao.getVariant(variantId)) {
                "Varian tidak tersedia"
            }
            require(variant.isActive && variant.productId == product.id) {
                "Varian tidak sesuai dengan produk"
            }
        }
        mapping.unitId?.let { unitId ->
            val unit = requireNotNull(inventoryDao.getUnit(unitId)) { "Satuan tidak tersedia" }
            require(capabilities.multiUnit && unit.isActive && unit.productId == product.id) {
                "Satuan tidak sesuai dengan produk"
            }
        }
    }
}

private fun ProductEntity.toDomain() = Product(
    id = id,
    categoryId = categoryId,
    name = name,
    basePrice = basePrice,
    stock = stock,
    stockTrackingEnabled = stockTrackingEnabled,
    hasVariants = hasVariants,
    lowStockThreshold = lowStockThreshold,
    imageUri = imageUri,
)
