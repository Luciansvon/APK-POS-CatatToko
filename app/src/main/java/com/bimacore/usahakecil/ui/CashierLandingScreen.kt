package com.bimacore.usahakecil.ui

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Assignment
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.bimacore.usahakecil.ui.theme.BrandColors

@Composable
fun CashierLandingScreen(
    businessLabel: String,
    activeTransactions: Int,
    lowStockCount: Int,
    outOfStockCount: Int,
    ownerUnlocked: Boolean,
    onOwnerAccess: () -> Unit,
    onStartTransaction: () -> Unit,
    onViewStock: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        CashierHeader(
            title = "Mode Kasir / Pekerja",
            subtitle = businessLabel,
            ownerUnlocked = ownerUnlocked,
            onOwnerAccess = onOwnerAccess,
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .size(142.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Storefront,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(76.dp),
                )
            }
            Spacer(Modifier.height(20.dp))
            Text(
                text = "Kasir bisa jualan, lihat stok,\ndan lihat total transaksi aktif.",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.height(22.dp))
            CashierMetric(
                icon = Icons.AutoMirrored.Outlined.Assignment,
                label = "Transaksi aktif hari ini",
                value = activeTransactions,
                valueColor = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(10.dp))
            CashierMetric(
                icon = Icons.Outlined.WarningAmber,
                label = "Produk stok menipis",
                value = lowStockCount,
                valueColor = BrandColors.Warning,
            )
            Spacer(Modifier.height(10.dp))
            CashierMetric(
                icon = Icons.Outlined.ErrorOutline,
                label = "Stok habis",
                value = outOfStockCount,
                valueColor = MaterialTheme.colorScheme.error,
                error = outOfStockCount > 0,
            )
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = onStartTransaction,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp)
                    .testTag("start-transaction"),
                shape = RoundedCornerShape(14.dp),
            ) {
                Text("Mulai Transaksi", fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(10.dp))
            OutlinedButton(
                onClick = onViewStock,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp)
                    .testTag("view-stock"),
                shape = RoundedCornerShape(14.dp),
            ) {
                Icon(Icons.Outlined.Inventory2, contentDescription = null)
                Spacer(Modifier.size(8.dp))
                Text("Lihat Stok", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun CashierHeader(
    title: String,
    subtitle: String? = "Mode Kasir / Pekerja",
    ownerUnlocked: Boolean,
    onOwnerAccess: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primary)
            .padding(horizontal = 20.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Outlined.Storefront,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.size(34.dp),
        )
        Spacer(Modifier.size(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                color = MaterialTheme.colorScheme.onPrimary,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.82f),
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
        OutlinedButton(
            onClick = onOwnerAccess,
            modifier = Modifier.testTag("owner-access"),
            shape = RoundedCornerShape(12.dp),
        ) {
            Icon(
                imageVector = if (ownerUnlocked) Icons.Outlined.LockOpen else Icons.Outlined.Shield,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary,
            )
            Spacer(Modifier.size(6.dp))
            Text(
                if (ownerUnlocked) "Mode Owner" else "Buka Mode Owner",
                color = MaterialTheme.colorScheme.onPrimary,
            )
        }
    }
}

@Composable
private fun CashierMetric(
    icon: ImageVector,
    label: String,
    value: Int,
    valueColor: Color,
    error: Boolean = false,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (error) {
                MaterialTheme.colorScheme.errorContainer
            } else {
                MaterialTheme.colorScheme.surface
            },
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .background(valueColor.copy(alpha = 0.12f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = valueColor)
            }
            Spacer(Modifier.size(14.dp))
            Text(
                label,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                value.toString(),
                color = valueColor,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
fun CashierFlowHeader(
    title: String,
    activeStep: Int,
    ownerUnlocked: Boolean,
    onOwnerAccess: () -> Unit,
    onBack: (() -> Unit)? = null,
) {
    Column {
        CashierHeader(
            title = title,
            ownerUnlocked = ownerUnlocked,
            onOwnerAccess = onOwnerAccess,
        )
        if (onBack != null) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.padding(start = 8.dp, top = 4.dp),
            ) {
                Icon(
                    Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = "Kembali",
                )
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 14.dp),
            verticalAlignment = Alignment.Top,
        ) {
            listOf("Pilih Produk", "Keranjang", "Pembayaran", "Selesai")
                .forEachIndexed { index, label ->
                    val step = index + 1
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .background(
                                    if (step == activeStep) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.surface
                                    },
                                    CircleShape,
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                step.toString(),
                                color = if (step == activeStep) {
                                    MaterialTheme.colorScheme.onPrimary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                fontWeight = FontWeight.Bold,
                            )
                        }
                        Spacer(Modifier.height(5.dp))
                        Text(
                            label,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (step == activeStep) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            fontWeight = if (step == activeStep) {
                                FontWeight.Bold
                            } else {
                                FontWeight.Normal
                            },
                            textAlign = TextAlign.Center,
                        )
                    }
                }
        }
    }
}
