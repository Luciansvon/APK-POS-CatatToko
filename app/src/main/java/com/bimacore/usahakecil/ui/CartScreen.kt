package com.bimacore.usahakecil.ui

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.bimacore.usahakecil.domain.CartItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CartScreen(
    items: List<CartItem>,
    compact: Boolean,
    onBack: () -> Unit,
    ownerUnlocked: Boolean,
    onOwnerAccess: () -> Unit,
    onQuantityChange: (String, Int) -> Unit,
    onContinue: () -> Unit,
    onCustomize: ((CartItem) -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val total = items.sumOf { it.subtotal }
    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            CashierFlowHeader(
                title = "Keranjang",
                activeStep = 2,
                ownerUnlocked = ownerUnlocked,
                onOwnerAccess = onOwnerAccess,
                onBack = if (compact) onBack else null,
            )
        },
        bottomBar = {
            CartCheckoutBar(
                total = total,
                enabled = items.isNotEmpty(),
                onContinue = onContinue,
            )
        },
    ) { innerPadding ->
        if (items.isEmpty()) {
            EmptyCart(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(items, key = { it.lineId }) { item ->
                    CartLineCard(
                        item = item,
                        onCustomize = onCustomize,
                        onQuantityChange = { quantity ->
                            onQuantityChange(item.lineId, quantity)
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun CartLineCard(
    item: CartItem,
    onCustomize: ((CartItem) -> Unit)?,
    onQuantityChange: (Int) -> Unit,
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = item.productName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (!item.variantName.isNullOrBlank()) {
                        Text(
                            text = item.variantName,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                    Text(
                        text = "${formatRupiah(item.unitPrice)} × ${item.quantity} ${item.unitLabel}",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFeatureSettings = "tnum",
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (item.toppings.isNotEmpty()) {
                        Text(
                            item.toppings.joinToString { it.name },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                    if (item.note.isNotBlank()) {
                        Text(
                            "Catatan: ${item.note}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Text(
                    text = formatRupiah(item.subtotal),
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontFeatureSettings = "tnum",
                    ),
                    fontWeight = FontWeight.Bold,
                )
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(
                    onClick = { onQuantityChange(0) },
                    modifier = Modifier
                        .size(36.dp)
                        .semantics {
                            contentDescription = "Hapus ${item.productName}"
                        },
                ) {
                    Icon(
                        Icons.Outlined.DeleteOutline,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp),
                    )
                }
                if (onCustomize != null) {
                    TextButton(
                        onClick = { onCustomize(item) },
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                    ) {
                        Text(
                            "Catatan/Topping",
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                }
                Spacer(Modifier.weight(1f))
                FilledTonalIconButton(
                    onClick = { onQuantityChange(item.quantity - 1) },
                    modifier = Modifier.size(36.dp),
                ) {
                    Icon(
                        Icons.Outlined.Remove,
                        contentDescription = "Kurangi jumlah",
                        modifier = Modifier.size(18.dp),
                    )
                }
                Text(
                    text = item.quantity.toString(),
                    modifier = Modifier
                        .padding(horizontal = 8.dp)
                        .semantics { contentDescription = "Jumlah ${item.quantity}" },
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFeatureSettings = "tnum",
                    ),
                    fontWeight = FontWeight.Bold,
                )
                FilledTonalIconButton(
                    onClick = { onQuantityChange(item.quantity + 1) },
                    enabled = item.availableStock == null || item.quantity < item.availableStock,
                    modifier = Modifier.size(36.dp),
                ) {
                    Icon(
                        Icons.Outlined.Add,
                        contentDescription = "Tambah jumlah",
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun CartCheckoutBar(
    total: Long,
    enabled: Boolean,
    onContinue: () -> Unit,
) {
    Card(
        shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = "Total",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = formatRupiah(total),
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontFeatureSettings = "tnum",
                    ),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Button(
                onClick = onContinue,
                enabled = enabled,
                modifier = Modifier
                    .height(52.dp)
                    .testTag("continue-payment"),
                shape = RoundedCornerShape(12.dp),
            ) {
                Text("Lanjut Pembayaran", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun EmptyCart(modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Outlined.ShoppingCart,
                contentDescription = null,
                modifier = Modifier.size(52.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = "Keranjang masih kosong",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "Pilih barang dari katalog untuk mulai.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
