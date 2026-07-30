package com.bimacore.usahakecil.ui

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.ShoppingCartCheckout
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.bimacore.usahakecil.domain.PaymentMethod
import com.bimacore.usahakecil.domain.Receipt
import com.bimacore.usahakecil.domain.ReceiptItem
import com.bimacore.usahakecil.ui.theme.BrandColors

@Composable
fun ReceiptScreen(
    receipt: Receipt,
    isSharing: Boolean,
    onShare: () -> Unit,
    onNewTransaction: () -> Unit,
    ownerUnlocked: Boolean,
    onOwnerAccess: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .testTag("receipt-list"),
        contentPadding = PaddingValues(top = 24.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        item {
            CashierFlowHeader(
                title = "Transaksi Selesai",
                activeStep = 4,
                ownerUnlocked = ownerUnlocked,
                onOwnerAccess = onOwnerAccess,
            )
        }
        item {
            Card(
                shape = CircleShape,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Check,
                    contentDescription = null,
                    modifier = Modifier
                        .padding(14.dp)
                        .size(32.dp),
                    tint = MaterialTheme.colorScheme.onPrimary,
                )
            }
            Spacer(Modifier.height(12.dp))
            Text(
                text = "Transaksi Berhasil",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            Text(
                text = receipt.receiptNumber,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                ),
                shape = RoundedCornerShape(16.dp),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = when (receipt.paymentMethod) {
                            PaymentMethod.CASH -> "Kembalian"
                            PaymentMethod.CREDIT -> "Piutang tercatat"
                            else -> "Pembayaran ${receipt.paymentMethod.displayName()} dicatat"
                        },
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    Text(
                        text = when (receipt.paymentMethod) {
                            PaymentMethod.CASH -> formatRupiah(receipt.changeAmount)
                            PaymentMethod.CREDIT ->
                                formatRupiah(receipt.total - receipt.amountReceived)
                            else -> formatRupiah(receipt.total)
                        },
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontFeatureSettings = "tnum",
                        ),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }
        }

        item {
            ReceiptHeader(receipt)
        }
        items(receipt.items) {
            ReceiptLine(it)
        }
        item {
            ReceiptTotals(receipt)
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedButton(
                    onClick = onShare,
                    enabled = !isSharing,
                    modifier = Modifier
                        .weight(1f)
                        .height(54.dp)
                        .testTag("share-receipt"),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Icon(Icons.Outlined.Share, contentDescription = null)
                    Spacer(Modifier.size(8.dp))
                    Text(if (isSharing) "Membuat PNG..." else "Bagikan Struk")
                }
                Button(
                    onClick = onNewTransaction,
                    modifier = Modifier
                        .weight(1f)
                        .height(54.dp)
                        .testTag("new-transaction"),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Icon(Icons.Outlined.ShoppingCartCheckout, contentDescription = null)
                    Spacer(Modifier.size(8.dp))
                    Text("Transaksi Baru", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun ReceiptHeader(receipt: Receipt) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(14.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Text(receipt.businessName, fontWeight = FontWeight.Bold)
            Text(
                formatReceiptDate(receipt.createdAt),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                "Metode: ${receipt.paymentMethod.displayName()}",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ReceiptLine(item: ReceiptItem) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Column(Modifier.weight(1f)) {
            Text(item.productName, fontWeight = FontWeight.SemiBold)
            if (!item.variantName.isNullOrBlank()) {
                Text(
                    item.variantName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Text(
                "${item.quantity} ${item.unitLabel} × ${formatRupiah(item.unitPrice)}",
                style = MaterialTheme.typography.bodySmall.copy(
                    fontFeatureSettings = "tnum",
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            item.toppingNames.forEach {
                Text(
                    "+ $it",
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
            formatRupiah(item.subtotal),
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontFeatureSettings = "tnum",
            ),
        )
    }
}

@Composable
private fun ReceiptTotals(receipt: Receipt) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(14.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SummaryRow("Total", formatRupiah(receipt.total), emphasized = true)
            if (receipt.paymentMethod == PaymentMethod.CASH) {
                SummaryRow("Uang diterima", formatRupiah(receipt.amountReceived))
                SummaryRow(
                    "Kembalian",
                    formatRupiah(receipt.changeAmount),
                    color = BrandColors.Success,
                )
            } else if (receipt.paymentMethod == PaymentMethod.CREDIT) {
                SummaryRow("Dibayar awal", formatRupiah(receipt.amountReceived))
                SummaryRow(
                    "Sisa piutang",
                    formatRupiah(receipt.total - receipt.amountReceived),
                )
            }
        }
    }
}

@Composable
private fun SummaryRow(
    label: String,
    value: String,
    emphasized: Boolean = false,
    color: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface,
) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(
            label,
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            value,
            color = color,
            fontWeight = if (emphasized) FontWeight.Bold else FontWeight.SemiBold,
            style = if (emphasized) {
                MaterialTheme.typography.titleMedium.copy(fontFeatureSettings = "tnum")
            } else {
                MaterialTheme.typography.bodyLarge.copy(fontFeatureSettings = "tnum")
            },
        )
    }
}
