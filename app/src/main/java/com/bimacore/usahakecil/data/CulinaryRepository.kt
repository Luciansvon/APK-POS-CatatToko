package com.bimacore.usahakecil.data

import com.bimacore.usahakecil.domain.BusinessCapabilities
import com.bimacore.usahakecil.domain.CulinaryRules
import com.bimacore.usahakecil.domain.MoneyMath
import com.bimacore.usahakecil.domain.OrderStatus
import kotlinx.coroutines.flow.Flow

class CulinaryRepository(
    private val database: PosDatabase,
    private val capabilities: BusinessCapabilities,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    private val culinaryDao = database.culinaryDao()
    private val catalogDao = database.catalogDao()
    private val saleDao = database.saleDao()

    val openOrders: Flow<List<SaleEntity>> = saleDao.observeOpenOrders()

    fun observeToppings(productId: Long): Flow<List<ToppingEntity>> {
        requireCulinary()
        return culinaryDao.observeToppings(productId)
    }

    fun observeRecipe(menuProductId: Long): Flow<List<RecipeIngredientEntity>> {
        requireCulinary()
        return culinaryDao.observeRecipe(menuProductId)
    }

    suspend fun saveTopping(
        id: Long?,
        productId: Long,
        label: String,
        price: Long,
    ): Long {
        requireCulinary()
        require(catalogDao.getProduct(productId) != null) { "Menu tidak tersedia" }
        require(label.isNotBlank()) { "Nama topping wajib diisi" }
        require(price in 0..MoneyMath.MAX_MONEY) { "Harga topping tidak valid" }
        val now = clock()
        return if (id == null) {
            culinaryDao.insertTopping(
                ToppingEntity(
                    productId = productId,
                    label = label.trim(),
                    price = price,
                    createdAt = now,
                    updatedAt = now,
                ),
            )
        } else {
            val current = requireNotNull(culinaryDao.getTopping(id)) { "Topping tidak tersedia" }
            culinaryDao.updateTopping(
                current.copy(
                    label = label.trim(),
                    price = price,
                    updatedAt = now,
                ),
            )
            id
        }
    }

    suspend fun setToppingActive(id: Long, active: Boolean) {
        requireCulinary()
        val current = requireNotNull(culinaryDao.getTopping(id)) { "Topping tidak tersedia" }
        culinaryDao.updateTopping(current.copy(isActive = active, updatedAt = clock()))
    }

    suspend fun saveRecipeIngredient(
        menuProductId: Long,
        ingredientProductId: Long,
        quantityPerMenu: Int,
    ) {
        requireCulinary()
        require(menuProductId != ingredientProductId) { "Menu tidak boleh menjadi bahannya sendiri" }
        require(catalogDao.getProduct(menuProductId) != null) { "Menu tidak tersedia" }
        val ingredient = requireNotNull(catalogDao.getProduct(ingredientProductId)) {
            "Bahan tidak tersedia"
        }
        require(ingredient.stockTrackingEnabled) { "Pelacakan stok bahan harus aktif" }
        CulinaryRules.ingredientQuantity(quantityPerMenu, 1)
        culinaryDao.saveRecipeIngredient(
            RecipeIngredientEntity(
                menuProductId = menuProductId,
                ingredientProductId = ingredientProductId,
                quantityPerMenu = quantityPerMenu,
                updatedAt = clock(),
            ),
        )
    }

    suspend fun removeRecipeIngredient(menuProductId: Long, ingredientProductId: Long) {
        requireCulinary()
        culinaryDao.deleteRecipeIngredient(menuProductId, ingredientProductId)
    }

    suspend fun setCartLineNote(lineId: String, note: String) {
        requireCulinary()
        culinaryDao.saveCartLineNote(
            CartLineNoteEntity(
                lineId = lineId,
                note = note.trim(),
                updatedAt = clock(),
            ),
        )
    }

    suspend fun setCartLineTopping(
        lineId: String,
        toppingId: Long,
        quantityPerMenu: Int,
    ) {
        requireCulinary()
        require(quantityPerMenu >= 0) { "Jumlah topping tidak valid" }
        if (quantityPerMenu == 0) {
            culinaryDao.deleteCartLineTopping(lineId, toppingId)
            return
        }
        val topping = requireNotNull(culinaryDao.getTopping(toppingId)) {
            "Topping tidak tersedia"
        }
        require(topping.isActive) { "Topping sudah tidak aktif" }
        culinaryDao.saveCartLineTopping(
            CartLineToppingEntity(
                lineId = lineId,
                toppingId = toppingId,
                quantity = quantityPerMenu,
                updatedAt = clock(),
            ),
        )
    }

    suspend fun moveOrder(
        saleId: Long,
        nextStatus: OrderStatus,
    ) {
        requireCulinary()
        val sale = requireNotNull(saleDao.getSale(saleId)) { "Pesanan tidak tersedia" }
        val current = runCatching { OrderStatus.valueOf(sale.orderStatus) }
            .getOrElse { throw IllegalArgumentException("Status pesanan tidak valid") }
        require(CulinaryRules.canMove(current, nextStatus)) {
            "Pesanan harus dilanjutkan sesuai urutan"
        }
        saleDao.updateSale(sale.copy(orderStatus = nextStatus.name, updatedAt = clock()))
    }

    private fun requireCulinary() {
        require(capabilities.culinaryOrders) { "Fitur Kuliner tidak aktif pada APK ini" }
    }
}
