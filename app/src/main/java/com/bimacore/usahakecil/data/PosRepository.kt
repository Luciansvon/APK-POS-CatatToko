package com.bimacore.usahakecil.data

import androidx.room.withTransaction
import com.bimacore.usahakecil.domain.AddToCartResult
import com.bimacore.usahakecil.domain.BusinessType
import com.bimacore.usahakecil.domain.CartItem
import com.bimacore.usahakecil.domain.CartTopping
import com.bimacore.usahakecil.domain.Category
import com.bimacore.usahakecil.domain.CheckoutResult
import com.bimacore.usahakecil.domain.InventoryRules
import com.bimacore.usahakecil.domain.MoneyMath
import com.bimacore.usahakecil.domain.PaymentMethod
import com.bimacore.usahakecil.domain.PriceTier
import com.bimacore.usahakecil.domain.Product
import com.bimacore.usahakecil.domain.ProductVariant
import com.bimacore.usahakecil.domain.Receipt
import com.bimacore.usahakecil.domain.ReceiptItem
import com.bimacore.usahakecil.security.ReportSession
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class CatalogSnapshot(
    val categories: List<Category>,
    val products: List<Product>,
    val variants: List<ProductVariant>,
    val cartItems: List<CartItem>,
    val completedSaleId: Long?,
)

data class CheckoutRequest(
    val method: PaymentMethod,
    val amountReceived: Long,
    val externalPaymentConfirmed: Boolean,
    val customerId: Long? = null,
)

data class SaleUnitOption(
    val id: Long?,
    val label: String,
    val factorToBase: Int,
    val salePrice: Long,
)

class PosRepository(
    private val database: PosDatabase,
    private val businessType: BusinessType,
    private val businessName: String,
    private val ownerSession: ReportSession? = null,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    val supportsCulinaryCustomization: Boolean
        get() = businessType == BusinessType.CULINARY
    val supportsCustomerReceivables: Boolean
        get() = businessType != BusinessType.CULINARY

    private val catalogDao = database.catalogDao()
    private val cartDao = database.cartDao()
    private val saleDao = database.saleDao()
    private val inventoryDao = database.inventoryAdminDao()
    private val culinaryDao = database.culinaryDao()
    private val operationsDao = database.operationsDao()
    private val checkoutMutex = Mutex()
    val customers: Flow<List<PartyEntity>> =
        operationsDao.observeParties(PartyKind.CUSTOMER.name)
    val sales: Flow<List<SaleEntity>> = saleDao.observeSales()

    private val catalogFlow = combine(
        catalogDao.observeCategories(),
        catalogDao.observeProducts(),
        catalogDao.observeVariants(),
    ) { categories, products, variants ->
        CatalogData(categories, products, variants)
    }
    private val cartFlow = combine(
        cartDao.observeLines(),
        cartDao.observeDraft(),
    ) { lines, draft -> CartData(lines, draft) }
    private val pricingFlow = combine(
        inventoryDao.observeAllUnits(),
        inventoryDao.observeAllPriceTiers(),
        culinaryDao.observeCartLineNotes(),
        culinaryDao.observeCartLineToppings(),
        culinaryDao.observeAllToppings(),
    ) { units, tiers, notes, cartToppings, toppings ->
        PricingData(units, tiers, notes, cartToppings, toppings)
    }

    val snapshot: Flow<CatalogSnapshot> = combine(
        catalogFlow,
        cartFlow,
        pricingFlow,
    ) { catalog, cart, pricing ->
        val categoryMap = catalog.categories.associateBy { it.id }
        val productMap = catalog.products.associateBy { it.id }
        val variantMap = catalog.variants.associateBy { it.id }
        val unitMap = pricing.units.associateBy { it.id }
        val noteMap = pricing.notes.associateBy { it.lineId }
        val toppingMap = pricing.toppings.associateBy { it.id }
        val cartToppingsByLine = pricing.cartToppings.groupBy { it.lineId }
        val tiersByProduct = pricing.tiers.groupBy { it.productId }

        val combinedBaseQuantities = mutableMapOf<Pair<Long, Long?>, Int>()
        cart.lines.forEach { line ->
            val parsed = parseLineId(line.id)
            val unit = parsed.unitId?.let(unitMap::get)
            val factor = unit?.factorToBase ?: 1
            val baseQuantity = InventoryRules.toBaseQuantity(line.quantity, factor)
            val key = Pair(line.productId, line.variantId)
            combinedBaseQuantities[key] = (combinedBaseQuantities[key] ?: 0) + baseQuantity
        }

        CatalogSnapshot(
            categories = catalog.categories.filter { it.isActive }.map {
                Category(it.id, it.name, it.iconKey)
            },
            products = catalog.products.filter { it.isActive }.map { it.toDomain() },
            variants = catalog.variants.filter { it.isActive }.map { it.toDomain() },
            cartItems = cart.lines.mapNotNull { line ->
                val product = productMap[line.productId] ?: return@mapNotNull null
                val variant = line.variantId?.let(variantMap::get)
                val category = categoryMap[product.categoryId]
                val parsed = parseLineId(line.id)
                val unit = parsed.unitId?.let(unitMap::get)
                val factor = unit?.factorToBase ?: 1
                val baseQuantity = InventoryRules.toBaseQuantity(line.quantity, factor)
                val combinedBaseQuantity = combinedBaseQuantities[Pair(product.id, variant?.id)] ?: baseQuantity
                val basePrice = variant?.priceOverride ?: product.basePrice
                val applicableTier = tiersByProduct[product.id]
                    .orEmpty()
                    .filter { combinedBaseQuantity >= it.minimumBaseQuantity }
                    .maxByOrNull { it.minimumBaseQuantity }
                val selectedUnitPrice = if (applicableTier != null) {
                    MoneyMath.multiply(applicableTier.unitPrice, factor)
                } else {
                    unit?.salePrice ?: basePrice
                }
                val lineToppings = cartToppingsByLine[line.id].orEmpty().mapNotNull { selected ->
                    val topping = toppingMap[selected.toppingId]
                        ?.takeIf { it.isActive }
                        ?: return@mapNotNull null
                    CartTopping(
                        toppingId = topping.id,
                        name = topping.label,
                        unitPrice = topping.price,
                        quantityPerItem = selected.quantity,
                    )
                }
                val toppingUnitTotal = lineToppings.fold(0L) { total, topping ->
                    Math.addExact(
                        total,
                        MoneyMath.multiply(topping.unitPrice, topping.quantityPerItem),
                    )
                }
                CartItem(
                    lineId = line.id,
                    productId = product.id,
                    variantId = variant?.id,
                    productName = product.name,
                    variantName = variant?.label,
                    categoryName = category?.name.orEmpty(),
                    unitPrice = Math.addExact(selectedUnitPrice, toppingUnitTotal),
                    quantity = line.quantity,
                    availableStock = when {
                        variant != null -> variant.stock / factor
                        product.stockTrackingEnabled -> product.stock / factor
                        else -> null
                    },
                    unitLabel = unit?.label ?: product.unitLabel,
                    factorToBase = factor,
                    note = noteMap[line.id]?.note.orEmpty(),
                    toppings = lineToppings,
                )
            },
            completedSaleId = cart.draft?.completedSaleId,
        )
    }

    suspend fun seedIfNeeded() {
        database.withTransaction {
            if (catalogDao.productCount() > 0) {
                ensureDraft()
                ensureProfile()
                return@withTransaction
            }
            val seed = SeedCatalog.forBusiness(businessType)
            catalogDao.insertCategories(seed.categories)
            catalogDao.insertProducts(seed.products)
            catalogDao.insertVariants(seed.variants)
            ensureDraft()
            ensureProfile()
        }
    }

    suspend fun getSaleUnits(productId: Long): List<SaleUnitOption> {
        val product = requireNotNull(catalogDao.getProduct(productId)) { "Produk tidak tersedia" }
        val base = SaleUnitOption(
            id = null,
            label = product.unitLabel,
            factorToBase = 1,
            salePrice = product.basePrice,
        )
        if (businessType != BusinessType.WHOLESALE) return listOf(base)
        return listOf(base) + inventoryDao.getActiveUnits(productId).map {
            SaleUnitOption(it.id, it.label, it.factorToBase, it.salePrice)
        }
    }

    suspend fun getAvailableToppings(productId: Long): List<ToppingEntity> {
        require(businessType == BusinessType.CULINARY) { "Topping hanya tersedia di APK Kuliner" }
        return culinaryDao.getActiveToppings(productId)
    }

    suspend fun setCartCustomization(
        lineId: String,
        note: String,
        toppingQuantities: Map<Long, Int>,
    ) = database.withTransaction {
        require(businessType == BusinessType.CULINARY) {
            "Catatan dan topping hanya tersedia di APK Kuliner"
        }
        requireNotNull(cartDao.getLine(lineId)) { "Item keranjang tidak tersedia" }
        culinaryDao.saveCartLineNote(
            CartLineNoteEntity(lineId = lineId, note = note.trim(), updatedAt = clock()),
        )
        toppingQuantities.forEach { (toppingId, quantity) ->
            require(quantity >= 0) { "Jumlah topping tidak valid" }
            if (quantity == 0) {
                culinaryDao.deleteCartLineTopping(lineId, toppingId)
            } else {
                val topping = requireNotNull(culinaryDao.getTopping(toppingId)) {
                    "Topping tidak tersedia"
                }
                require(topping.isActive) { "Topping sudah tidak aktif" }
                culinaryDao.saveCartLineTopping(
                    CartLineToppingEntity(
                        lineId = lineId,
                        toppingId = toppingId,
                        quantity = quantity,
                        updatedAt = clock(),
                    ),
                )
            }
        }
    }

    suspend fun addProduct(
        productId: Long,
        variantId: Long? = null,
        unitId: Long? = null,
    ): AddToCartResult =
        database.withTransaction {
            if (cartDao.getDraft()?.completedSaleId != null) {
                return@withTransaction AddToCartResult.CompletedTransactionLocked
            }
            val product = catalogDao.getProduct(productId)
                ?: return@withTransaction AddToCartResult.OutOfStock
            if (!product.isActive) {
                return@withTransaction AddToCartResult.OutOfStock
            }
            if (product.hasVariants && variantId == null) {
                return@withTransaction AddToCartResult.VariantRequired
            }
            val variant = variantId?.let { catalogDao.getVariant(it) }
            if (variantId != null && (variant == null || !variant.isActive || variant.productId != productId)) {
                return@withTransaction AddToCartResult.OutOfStock
            }
            val unit = unitId?.let { inventoryDao.getUnit(it) }
            if (
                unitId != null &&
                (
                    businessType != BusinessType.WHOLESALE ||
                        unit == null ||
                        !unit.isActive ||
                        unit.productId != productId
                    )
            ) {
                return@withTransaction AddToCartResult.OutOfStock
            }
            val factor = unit?.factorToBase ?: 1
            val lineId = lineId(productId, variantId, unitId)
            val current = cartDao.getLine(lineId)
            val newQuantity = (current?.quantity ?: 0) + 1
            val newBaseQuantity = InventoryRules.toBaseQuantity(newQuantity, factor)
            val available = when {
                variant != null -> variant.stock
                product.stockTrackingEnabled -> product.stock
                else -> null
            }
            if (
                newQuantity > MoneyMath.MAX_QUANTITY ||
                available != null && newBaseQuantity > available
            ) {
                return@withTransaction AddToCartResult.OutOfStock
            }
            cartDao.upsertLine(
                CartLineEntity(
                    id = lineId,
                    productId = productId,
                    variantId = variantId,
                    quantity = newQuantity,
                    updatedAt = clock(),
                ),
            )
            touchDraft()
            AddToCartResult.Added
        }

    suspend fun setQuantity(lineId: String, quantity: Int): Boolean = database.withTransaction {
        if (cartDao.getDraft()?.completedSaleId != null) return@withTransaction false
        val line = cartDao.getLine(lineId) ?: return@withTransaction false
        if (quantity <= 0) {
            cartDao.deleteLine(lineId)
            if (businessType == BusinessType.CULINARY) {
                culinaryDao.deleteCartLineNote(lineId)
                culinaryDao.deleteCartLineToppingsByLine(lineId)
            }
            touchDraft()
            return@withTransaction true
        }
        if (quantity > MoneyMath.MAX_QUANTITY) return@withTransaction false
        val product = catalogDao.getProduct(line.productId) ?: return@withTransaction false
        val variant = line.variantId?.let { catalogDao.getVariant(it) }
        val factor = parseLineId(line.id).unitId
            ?.let { inventoryDao.getUnit(it) }
            ?.factorToBase
            ?: 1
        val baseQuantity = InventoryRules.toBaseQuantity(quantity, factor)
        val available = when {
            variant != null -> variant.stock
            product.stockTrackingEnabled -> product.stock
            else -> null
        }
        if (available != null && baseQuantity > available) return@withTransaction false
        cartDao.upsertLine(line.copy(quantity = quantity, updatedAt = clock()))
        touchDraft()
        true
    }

    suspend fun incrementQuantity(lineId: String, delta: Int): Boolean = database.withTransaction {
        if (cartDao.getDraft()?.completedSaleId != null) return@withTransaction false
        val line = cartDao.getLine(lineId) ?: return@withTransaction false
        val targetQuantity = line.quantity + delta
        setQuantity(lineId, targetQuantity)
    }

    suspend fun completeSale(request: CheckoutRequest): CheckoutResult =
        checkoutMutex.withLock {
            try {
                val saleId = database.withTransaction {
                    val existing = cartDao.getDraft()?.completedSaleId
                    if (existing != null) return@withTransaction existing
                    val activeShift = database.shiftDao().getOpenShift()
                    require(activeShift != null || ownerSession?.isUnlocked == true) {
                        "Buka shift terlebih dahulu sebelum menerima transaksi"
                    }

                    val lines = cartDao.getLines()
                    require(lines.isNotEmpty()) { "Keranjang masih kosong" }

                    val combinedBaseQuantities = mutableMapOf<Pair<Long, Long?>, Int>()
                    for (line in lines) {
                        val parsed = parseLineId(line.id)
                        val unit = parsed.unitId?.let { inventoryDao.getUnit(it) }
                        val factor = unit?.factorToBase ?: 1
                        val baseQuantity = InventoryRules.toBaseQuantity(line.quantity, factor)
                        val key = Pair(line.productId, line.variantId)
                        combinedBaseQuantities[key] = (combinedBaseQuantities[key] ?: 0) + baseQuantity
                    }

                    val snapshots = lines.map { line ->
                        val product = requireNotNull(catalogDao.getProduct(line.productId)) {
                            "Produk sudah tidak tersedia"
                        }
                        require(product.isActive) { "Produk ${product.name} sudah tidak aktif" }
                        val variant = line.variantId?.let {
                            requireNotNull(catalogDao.getVariant(it)) {
                                "Varian sudah tidak tersedia"
                            }
                        }
                        require(variant == null || variant.isActive) {
                            "Varian ${variant?.label} sudah tidak aktif"
                        }
                        require(variant == null || variant.productId == product.id) {
                            "Varian tidak sesuai produk"
                        }
                        val parsed = parseLineId(line.id)
                        val unit = parsed.unitId?.let {
                            requireNotNull(inventoryDao.getUnit(it)) {
                                "Satuan penjualan sudah tidak tersedia"
                            }
                        }
                        require(unit == null || unit.productId == product.id && unit.isActive) {
                            "Satuan penjualan sudah tidak aktif"
                        }
                        val factor = unit?.factorToBase ?: 1
                        val baseQuantity = InventoryRules.toBaseQuantity(line.quantity, factor)
                        val combinedBaseQuantity = combinedBaseQuantities[Pair(product.id, variant?.id)] ?: baseQuantity
                        val available = when {
                            variant != null -> variant.stock
                            product.stockTrackingEnabled -> product.stock
                            else -> null
                        }
                        require(available == null || baseQuantity <= available) {
                            "Stok ${product.name} tidak cukup"
                        }
                        val category = catalogDao.getCategory(product.categoryId)
                        val basePrice = variant?.priceOverride ?: product.basePrice
                        val applicableTier = if (businessType == BusinessType.WHOLESALE) {
                            inventoryDao.getPriceTiers(product.id)
                                .filter { combinedBaseQuantity >= it.minimumBaseQuantity }
                                .maxByOrNull { it.minimumBaseQuantity }
                        } else {
                            null
                        }
                        val merchandiseUnitPrice = if (applicableTier != null) {
                            MoneyMath.multiply(applicableTier.unitPrice, factor)
                        } else {
                            unit?.salePrice ?: basePrice
                        }
                        val selectedToppings = if (businessType == BusinessType.CULINARY) {
                            culinaryDao.getCartLineToppings(line.id).map { selected ->
                                val topping = requireNotNull(culinaryDao.getTopping(selected.toppingId)) {
                                    "Topping sudah tidak tersedia"
                                }
                                require(topping.isActive && topping.productId == product.id) {
                                    "Topping tidak sesuai menu atau sudah tidak aktif"
                                }
                                ResolvedTopping(
                                    entity = topping,
                                    quantityPerItem = selected.quantity,
                                    subtotal = MoneyMath.multiply(
                                        MoneyMath.multiply(topping.price, selected.quantity),
                                        line.quantity,
                                    ),
                                )
                            }
                        } else {
                            emptyList()
                        }
                        val toppingUnitPrice = selectedToppings.fold(0L) { total, topping ->
                            Math.addExact(
                                total,
                                MoneyMath.multiply(
                                    topping.entity.price,
                                    topping.quantityPerItem,
                                ),
                            )
                        }
                        val unitPrice = Math.addExact(merchandiseUnitPrice, toppingUnitPrice)
                        CheckoutLine(
                            cart = line,
                            product = product,
                            variant = variant,
                            categoryName = category?.name.orEmpty(),
                            unitPrice = unitPrice,
                            subtotal = MoneyMath.multiply(unitPrice, line.quantity),
                            unitLabel = unit?.label ?: product.unitLabel,
                            baseQuantity = baseQuantity,
                            note = if (businessType == BusinessType.CULINARY) {
                                culinaryDao.getCartLineNote(line.id)?.note.orEmpty()
                            } else {
                                ""
                            },
                            toppings = selectedToppings,
                            recipe = if (businessType == BusinessType.CULINARY) {
                                culinaryDao.getRecipe(product.id)
                            } else {
                                emptyList()
                            },
                        )
                    }
                    val productDeductions = mutableMapOf<Long, Int>()
                    val variantDeductions = mutableMapOf<Long, Int>()
                    snapshots.forEach { item ->
                        when {
                            item.variant != null -> variantDeductions[item.variant.id] =
                                Math.addExact(
                                    variantDeductions[item.variant.id] ?: 0,
                                    item.baseQuantity,
                                )
                            item.product.stockTrackingEnabled -> productDeductions[item.product.id] =
                                Math.addExact(
                                    productDeductions[item.product.id] ?: 0,
                                    item.baseQuantity,
                                )
                        }
                        item.recipe.forEach { ingredient ->
                            val required = InventoryRules.toBaseQuantity(
                                item.cart.quantity,
                                ingredient.quantityPerMenu,
                            )
                            productDeductions[ingredient.ingredientProductId] =
                                Math.addExact(
                                    productDeductions[ingredient.ingredientProductId] ?: 0,
                                    required,
                                )
                        }
                    }
                    productDeductions.forEach { (productId, required) ->
                        val product = requireNotNull(catalogDao.getProduct(productId)) {
                            "Bahan resep sudah tidak tersedia"
                        }
                        require(product.stockTrackingEnabled && product.stock >= required) {
                            "Stok bahan ${product.name} tidak cukup"
                        }
                    }
                    variantDeductions.forEach { (variantId, required) ->
                        val variant = requireNotNull(catalogDao.getVariant(variantId)) {
                            "Varian sudah tidak tersedia"
                        }
                        require(variant.stock >= required) {
                            "Stok varian ${variant.label} tidak cukup"
                        }
                    }
                    val total = snapshots.fold(0L) { acc, item ->
                        Math.addExact(acc, item.subtotal).also { require(it <= MoneyMath.MAX_MONEY) }
                    }

                    val amountReceived = when (request.method) {
                        PaymentMethod.CASH -> {
                            require(request.amountReceived in 0..MoneyMath.MAX_MONEY) {
                                "Nominal uang tidak valid"
                            }
                            require(request.amountReceived >= total) {
                                "Uang kurang Rp${total - request.amountReceived}"
                            }
                            request.amountReceived
                        }
                        PaymentMethod.QRIS,
                        PaymentMethod.TRANSFER,
                        -> {
                            require(request.externalPaymentConfirmed) {
                                "Konfirmasi pembayaran sudah masuk"
                            }
                            total
                        }
                        PaymentMethod.CREDIT -> {
                            require(businessType != BusinessType.CULINARY) {
                                "Piutang pelanggan tidak aktif pada APK ini"
                            }
                            require(request.amountReceived in 0..total) {
                                "Pembayaran awal piutang tidak valid"
                            }
                            requireNotNull(request.customerId) {
                                "Pilih pelanggan untuk transaksi piutang"
                            }
                            request.amountReceived
                        }
                    }
                    val customer = request.customerId?.let {
                        requireNotNull(operationsDao.getParty(it)) { "Pelanggan tidak tersedia" }
                            .also { party ->
                                require(
                                    party.kind == PartyKind.CUSTOMER.name && party.isActive,
                                ) { "Pelanggan tidak aktif" }
                            }
                    }
                    val now = clock()
                    val receiptNumber = buildReceiptNumber(now)
                    val activeBusinessName = database.profileDao()
                        .getProfile()
                        ?.businessName
                        ?.trim()
                        ?.takeIf { it.isNotBlank() }
                        ?: businessName
                    val saleId = saleDao.insertSale(
                        SaleEntity(
                            receiptNumber = receiptNumber,
                            businessName = activeBusinessName,
                            createdAt = now,
                            paymentMethod = request.method.name,
                            total = total,
                            amountReceived = amountReceived,
                            changeAmount = MoneyMath.change(total, amountReceived),
                            customerId = customer?.id,
                            settlementStatus = if (amountReceived >= total) "PAID" else {
                                if (amountReceived > 0) "PARTIAL" else "UNPAID"
                            },
                            orderStatus = if (businessType == BusinessType.CULINARY) {
                                "NEW"
                            } else {
                                "COMPLETED"
                            },
                            updatedAt = now,
                            shiftId = activeShift?.id,
                        ),
                    )
                    val saleItemIds = saleDao.insertItems(
                        snapshots.map {
                            SaleItemEntity(
                                saleId = saleId,
                                productId = it.product.id,
                                variantId = it.variant?.id,
                                productName = it.product.name,
                                variantName = it.variant?.label,
                                categoryName = it.categoryName,
                                unitPrice = it.unitPrice,
                                quantity = it.cart.quantity,
                                subtotal = it.subtotal,
                                baseQuantity = it.baseQuantity,
                                unitLabel = it.unitLabel,
                                note = it.note,
                            )
                        },
                    )
                    culinaryDao.insertSaleItemToppings(
                        snapshots.flatMapIndexed { index, item ->
                            item.toppings.map { topping ->
                                SaleItemToppingEntity(
                                    saleItemId = saleItemIds[index],
                                    toppingId = topping.entity.id,
                                    toppingName = topping.entity.label,
                                    unitPrice = topping.entity.price,
                                    quantity = Math.multiplyExact(
                                        topping.quantityPerItem,
                                        item.cart.quantity,
                                    ),
                                    subtotal = topping.subtotal,
                                )
                            }
                        },
                    )
                    database.stockDao().insertMovements(
                        snapshots
                            .filter { it.variant != null || it.product.stockTrackingEnabled }
                            .map {
                                StockMovementEntity(
                                    productId = it.product.id,
                                    variantId = it.variant?.id,
                                    saleId = saleId,
                                    type = "SALE",
                                    quantityDelta = -it.cart.quantity,
                                    reason = "Penjualan ${receiptNumber}",
                                    createdAt = now,
                                    referenceType = "SALE",
                                    referenceId = saleId,
                                    unitLabel = it.unitLabel,
                                    baseQuantityDelta = -it.baseQuantity,
                                )
                            },
                    )
                    val ingredientMovements = snapshots.flatMap { item ->
                        item.recipe.map { ingredient ->
                            val required = InventoryRules.toBaseQuantity(
                                item.cart.quantity,
                                ingredient.quantityPerMenu,
                            )
                            StockMovementEntity(
                                productId = ingredient.ingredientProductId,
                                variantId = null,
                                saleId = saleId,
                                type = "INGREDIENT_USE",
                                quantityDelta = -required,
                                reason = "Bahan ${receiptNumber}",
                                createdAt = now,
                                referenceType = "SALE",
                                referenceId = saleId,
                                unitLabel = "satuan dasar",
                                baseQuantityDelta = -required,
                            )
                        }
                    }
                    if (ingredientMovements.isNotEmpty()) {
                        database.stockDao().insertMovements(ingredientMovements)
                    }
                    productDeductions.forEach { (productId, required) ->
                        val product = requireNotNull(catalogDao.getProduct(productId))
                        catalogDao.updateProduct(
                            product.copy(stock = product.stock - required, updatedAt = now),
                        )
                    }
                    variantDeductions.forEach { (variantId, required) ->
                        val variant = requireNotNull(catalogDao.getVariant(variantId))
                        catalogDao.updateVariant(
                            variant.copy(stock = variant.stock - required, updatedAt = now),
                        )
                    }
                    if (amountReceived > 0) {
                        operationsDao.insertCashEntry(
                            CashEntryEntity(
                            type = if (request.method == PaymentMethod.CREDIT) {
                                "RECEIVABLE_IN"
                            } else {
                                "SALE_IN"
                            },
                            amount = if (request.method == PaymentMethod.CREDIT) {
                                amountReceived
                            } else {
                                total
                            },
                            category = "Penjualan",
                            note = "Penjualan $receiptNumber",
                            paymentMethod = if (request.method == PaymentMethod.CREDIT) {
                                "CASH"
                            } else {
                                request.method.name
                            },
                            referenceType = "SALE",
                            referenceId = saleId,
                            createdAt = now,
                            shiftId = activeShift?.id,
                            ),
                        )
                    }
                    if (request.method == PaymentMethod.CREDIT && amountReceived < total) {
                        val debtId = operationsDao.insertDebt(
                            DebtEntity(
                                kind = DebtKind.RECEIVABLE.name,
                                partyId = requireNotNull(customer).id,
                                partyName = customer.name,
                                sourceType = "SALE",
                                sourceId = saleId,
                                originalAmount = total,
                                paidAmount = amountReceived,
                                settlementStatus = if (amountReceived > 0) "PARTIAL" else "UNPAID",
                                note = "Piutang penjualan $receiptNumber",
                                createdAt = now,
                                updatedAt = now,
                            ),
                        )
                        if (amountReceived > 0) {
                            operationsDao.insertDebtPayment(
                                DebtPaymentEntity(
                                    debtId = debtId,
                                    amount = amountReceived,
                                    paymentMethod = "CASH",
                                    note = "Pembayaran awal penjualan",
                                    paidAt = now,
                                ),
                            )
                        }
                    }
                    cartDao.upsertDraft(
                        DraftCartEntity(
                            completedSaleId = saleId,
                            updatedAt = now,
                        ),
                    )
                    saleId
                }
                CheckoutResult.Success(requireNotNull(loadReceipt(saleId)))
            } catch (error: IllegalArgumentException) {
                CheckoutResult.Error(error.message ?: "Transaksi tidak dapat disimpan")
            } catch (error: IllegalStateException) {
                CheckoutResult.Error(error.message ?: "Transaksi tidak dapat disimpan")
            } catch (_: ArithmeticException) {
                CheckoutResult.Error("Nilai transaksi terlalu besar")
            }
        }

    suspend fun loadCurrentReceipt(): Receipt? {
        val saleId = cartDao.getDraft()?.completedSaleId ?: return null
        return loadReceipt(saleId)
    }

    suspend fun newTransaction() {
        database.withTransaction {
            cartDao.clearLines()
            culinaryDao.clearCartLineNotes()
            culinaryDao.clearCartLineToppings()
            cartDao.upsertDraft(DraftCartEntity(updatedAt = clock()))
        }
    }

    private suspend fun loadReceipt(saleId: Long): Receipt? {
        val sale = saleDao.getSale(saleId) ?: return null
        return Receipt(
            saleId = sale.id,
            receiptNumber = sale.receiptNumber,
            businessName = sale.businessName,
            createdAt = sale.createdAt,
            paymentMethod = PaymentMethod.valueOf(sale.paymentMethod),
            total = sale.total,
            amountReceived = sale.amountReceived,
            changeAmount = sale.changeAmount,
            items = saleDao.getItems(saleId).map {
                ReceiptItem(
                    productName = it.productName,
                    variantName = it.variantName,
                    quantity = it.quantity,
                    unitPrice = it.unitPrice,
                    subtotal = it.subtotal,
                    unitLabel = it.unitLabel,
                    note = it.note,
                    toppingNames = culinaryDao.getSaleItemToppings(it.id).map { topping ->
                        topping.toppingName
                    },
                )
            },
        )
    }

    private suspend fun ensureDraft() {
        if (cartDao.getDraft() == null) {
            cartDao.upsertDraft(DraftCartEntity(updatedAt = clock()))
        }
    }

    private suspend fun ensureProfile() {
        if (database.profileDao().getProfile() != null) return
        val now = clock()
        database.profileDao().saveProfile(
            BusinessProfileEntity(
                businessUid = UUID.randomUUID().toString(),
                businessName = businessName,
                businessType = businessType.name,
                createdAt = now,
                updatedAt = now,
            ),
        )
    }

    private suspend fun touchDraft() {
        val current = cartDao.getDraft()
        cartDao.upsertDraft(
            DraftCartEntity(
                completedSaleId = current?.completedSaleId,
                updatedAt = clock(),
            ),
        )
    }

    private fun buildReceiptNumber(timestamp: Long): String {
        val date = SimpleDateFormat("yyyyMMdd-HHmmss-SSS", Locale.US).format(Date(timestamp))
        val prefix = when (businessType) {
            BusinessType.RETAIL -> "RTL"
            BusinessType.WHOLESALE -> "GRS"
            BusinessType.CULINARY -> "KUL"
        }
        return "$prefix-$date"
    }

    private fun lineId(
        productId: Long,
        variantId: Long?,
        unitId: Long?,
    ): String = "$productId:${variantId ?: 0L}:${unitId ?: 0L}"

    private fun parseLineId(value: String): ParsedLineId {
        val parts = value.split(':')
        return ParsedLineId(
            productId = parts.getOrNull(0)?.toLongOrNull() ?: 0,
            variantId = parts.getOrNull(1)?.toLongOrNull()?.takeIf { it != 0L },
            unitId = parts.getOrNull(2)?.toLongOrNull()?.takeIf { it != 0L },
        )
    }

    private data class CheckoutLine(
        val cart: CartLineEntity,
        val product: ProductEntity,
        val variant: ProductVariantEntity?,
        val categoryName: String,
        val unitPrice: Long,
        val subtotal: Long,
        val unitLabel: String,
        val baseQuantity: Int,
        val note: String,
        val toppings: List<ResolvedTopping>,
        val recipe: List<RecipeIngredientEntity>,
    )

    private data class ResolvedTopping(
        val entity: ToppingEntity,
        val quantityPerItem: Int,
        val subtotal: Long,
    )

    private data class CatalogData(
        val categories: List<CategoryEntity>,
        val products: List<ProductEntity>,
        val variants: List<ProductVariantEntity>,
    )

    private data class CartData(
        val lines: List<CartLineEntity>,
        val draft: DraftCartEntity?,
    )

    private data class PricingData(
        val units: List<UnitConversionEntity>,
        val tiers: List<PriceTierEntity>,
        val notes: List<CartLineNoteEntity>,
        val cartToppings: List<CartLineToppingEntity>,
        val toppings: List<ToppingEntity>,
    )

    private data class ParsedLineId(
        val productId: Long,
        val variantId: Long?,
        val unitId: Long?,
    )
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

private fun ProductVariantEntity.toDomain() = ProductVariant(
    id = id,
    productId = productId,
    label = label,
    priceOverride = priceOverride,
    stock = stock,
)
