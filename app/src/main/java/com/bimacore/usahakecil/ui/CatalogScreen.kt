package com.bimacore.usahakecil.ui

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Calculate
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.Checkroom
import androidx.compose.material.icons.outlined.Fastfood
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.LocalCafe
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material3.Badge
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bimacore.usahakecil.data.CatalogSnapshot
import com.bimacore.usahakecil.domain.Product
import com.bimacore.usahakecil.ui.theme.BrandColors
import java.io.FileNotFoundException

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CatalogScreen(
    businessLabel: String,
    snapshot: CatalogSnapshot,
    search: String,
    selectedCategoryId: Long?,
    compact: Boolean,
    onSearchChange: (String) -> Unit,
    onCategorySelected: (Long?) -> Unit,
    onProductClick: (Product) -> Unit,
    onQuantityChange: (String, Int) -> Unit,
    onCartClick: () -> Unit,
    onCalculatorClick: () -> Unit,
    ownerUnlocked: Boolean,
    onOwnerAccess: () -> Unit,
    modifier: Modifier = Modifier.fillMaxSize(),
) {
    val filteredProducts = snapshot.products.filter { product ->
        val matchesCategory = selectedCategoryId == null ||
            selectedCategoryId == 1L ||
            product.categoryId == selectedCategoryId
        val matchesSearch = search.isBlank() ||
            product.name.contains(search.trim(), ignoreCase = true)
        matchesCategory && matchesSearch
    }
    val quantities = snapshot.cartItems.groupingBy { it.productId }
        .fold(0) { total, item -> total + item.quantity }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Column {
                CashierFlowHeader(
                    title = "Pilih Produk",
                    activeStep = 1,
                    ownerUnlocked = ownerUnlocked,
                    onOwnerAccess = onOwnerAccess,
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.End,
                ) {
                    IconButton(onClick = onCalculatorClick) {
                        Icon(Icons.Outlined.Calculate, contentDescription = "Buka kalkulator")
                    }
                }
            }
        },
        bottomBar = {
            if (compact && snapshot.cartItems.isNotEmpty()) {
                CartSummaryBar(
                    itemCount = snapshot.cartItems.sumOf { it.quantity },
                    total = snapshot.cartItems.sumOf { it.subtotal },
                    onClick = onCartClick,
                )
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            OutlinedTextField(
                value = search,
                onValueChange = onSearchChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .semantics { contentDescription = "Cari produk" },
                singleLine = true,
                leadingIcon = {
                    Icon(Icons.Outlined.Search, contentDescription = null)
                },
                placeholder = { Text("Cari nama barang atau menu") },
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.55f),
                ),
            )
            Spacer(Modifier.height(12.dp))
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(snapshot.categories, key = { it.id }) { category ->
                    FilterChip(
                        selected = selectedCategoryId == category.id,
                        onClick = { onCategorySelected(category.id) },
                        label = { Text(category.name) },
                        leadingIcon = {
                            Icon(
                                imageVector = categoryIcon(category.iconKey),
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                        },
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            if (filteredProducts.isEmpty()) {
                EmptyCatalog(
                    hasSearch = search.isNotBlank(),
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 8.dp,
                        bottom = if (compact && snapshot.cartItems.isNotEmpty()) 16.dp else 24.dp,
                    ),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(filteredProducts, key = { it.id }) { product ->
                        val variantStock = snapshot.variants
                            .filter { it.productId == product.id }
                            .sumOf { it.stock }
                        val availableStock = when {
                            product.hasVariants -> variantStock
                            product.stockTrackingEnabled -> product.stock
                            else -> null
                        }
                        ProductCard(
                            product = product,
                            cartItems = snapshot.cartItems.filter { it.productId == product.id },
                            quantityInCart = quantities[product.id] ?: 0,
                            availableStock = availableStock,
                            onClick = { onProductClick(product) },
                            onQuantityChange = onQuantityChange,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProductCard(
    product: Product,
    cartItems: List<com.bimacore.usahakecil.domain.CartItem>,
    quantityInCart: Int,
    availableStock: Int?,
    onClick: () -> Unit,
    onQuantityChange: (String, Int) -> Unit,
) {
    val outOfStock = availableStock != null && availableStock <= 0
    val lowStock = availableStock != null && availableStock in 1..product.lowStockThreshold

    Card(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("product-${product.id}")
            .semantics {
                contentDescription = buildString {
                    append(product.name)
                    append(", ")
                    append(formatRupiah(product.basePrice))
                    if (availableStock != null) append(", stok $availableStock")
                }
            },
    ) {
        Box {
            ProductVisual(
                imageUri = product.imageUri,
                icon = categoryIconForProduct(product.categoryId),
            )
            if (quantityInCart > 0) {
                Badge(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(10.dp),
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ) {
                    Text(
                        text = quantityInCart.toString(),
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                    )
                }
            }
        }
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Text(
                text = product.name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                minLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = formatRupiah(product.basePrice),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontFeatureSettings = "tnum",
                ),
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = when {
                    outOfStock -> "Habis"
                    availableStock == null -> "Selalu tersedia"
                    lowStock -> "Stok menipis · $availableStock"
                    else -> "Stok $availableStock"
                },
                style = MaterialTheme.typography.labelMedium,
                color = when {
                    outOfStock -> MaterialTheme.colorScheme.error
                    lowStock -> BrandColors.Warning
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                },
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun ProductVisual(
    imageUri: String?,
    icon: ImageVector,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val bitmap by produceState<ImageBitmap?>(initialValue = null, imageUri) {
        value = if (imageUri.isNullOrBlank()) {
            null
        } else {
            try {
                context.contentResolver.openInputStream(Uri.parse(imageUri))?.use {
                    BitmapFactory.decodeStream(it)?.asImageBitmap()
                }
            } catch (_: FileNotFoundException) {
                null
            } catch (_: SecurityException) {
                null
            }
        }
    }

    Box(
        modifier = modifier
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        if (bitmap != null) {
            Image(
                bitmap = requireNotNull(bitmap),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        } else {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(34.dp),
            )
        }
    }
}

@Composable
private fun EmptyCatalog(
    hasSearch: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Outlined.Inventory2,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = if (hasSearch) "Produk tidak ditemukan" else "Belum ada produk",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = if (hasSearch) "Coba kata kunci atau kategori lain." else "Tambahkan produk untuk mulai berjualan.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
fun CartSummaryBar(
    itemCount: Int,
    total: Long,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("cart-summary"),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Outlined.ShoppingCart,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = "$itemCount barang",
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.82f),
                    style = MaterialTheme.typography.labelLarge,
                )
                Text(
                    text = formatRupiah(total),
                    color = MaterialTheme.colorScheme.onPrimary,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFeatureSettings = "tnum",
                    ),
                    fontWeight = FontWeight.Bold,
                )
            }
            Text(
                text = "Lihat Keranjang",
                color = MaterialTheme.colorScheme.onPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
            )
        }
    }
}

private fun categoryIcon(iconKey: String): ImageVector = when (iconKey) {
    "drink" -> Icons.Outlined.LocalCafe
    "meal", "snack" -> Icons.Outlined.Fastfood
    "shirt" -> Icons.Outlined.Checkroom
    "store" -> Icons.Outlined.Storefront
    "box" -> Icons.Outlined.Inventory2
    else -> Icons.Outlined.Category
}

private fun categoryIconForProduct(categoryId: Long): ImageVector = when (categoryId) {
    2L -> Icons.Outlined.Fastfood
    3L -> Icons.Outlined.LocalCafe
    4L -> Icons.Outlined.Checkroom
    else -> Icons.Outlined.Inventory2
}
