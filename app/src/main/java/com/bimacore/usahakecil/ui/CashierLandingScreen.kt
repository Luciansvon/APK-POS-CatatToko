package com.bimacore.usahakecil.ui

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.outlined.Calculate
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.LockOpen
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.bimacore.usahakecil.R
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
            title = "Mode Kasir",
            subtitle = businessLabel,
            ownerUnlocked = ownerUnlocked,
            onOwnerAccess = onOwnerAccess,
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_launcher_foreground_v2),
                    contentDescription = stringResource(R.string.brand_logo_description),
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .size(76.dp)
                        .testTag("catattoko-brand"),
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.brand_name),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Kasir bisa jualan, lihat stok,\ndan lihat total transaksi aktif.",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.height(12.dp))
            CashierMetric(
                icon = Icons.AutoMirrored.Outlined.Assignment,
                label = "Transaksi aktif hari ini",
                value = activeTransactions,
                valueColor = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(8.dp))
            CashierMetric(
                icon = Icons.Outlined.WarningAmber,
                label = "Produk stok menipis",
                value = lowStockCount,
                valueColor = BrandColors.Warning,
            )
            Spacer(Modifier.height(8.dp))
            CashierMetric(
                icon = Icons.Outlined.ErrorOutline,
                label = "Stok habis",
                value = outOfStockCount,
                valueColor = MaterialTheme.colorScheme.error,
                error = outOfStockCount > 0,
            )
            Spacer(Modifier.height(14.dp))
            Button(
                onClick = onStartTransaction,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("start-transaction"),
                shape = RoundedCornerShape(12.dp),
            ) {
                Text("Mulai Transaksi", fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = onViewStock,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("view-stock"),
                shape = RoundedCornerShape(12.dp),
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
    subtitle: String? = "Mode Kasir",
    ownerUnlocked: Boolean,
    onOwnerAccess: () -> Unit,
    onCalculatorClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primary)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Outlined.Storefront,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.size(24.dp),
        )
        Spacer(Modifier.size(8.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                color = MaterialTheme.colorScheme.onPrimary,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f),
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 1,
                )
            }
        }
        if (onCalculatorClick != null) {
            IconButton(
                onClick = onCalculatorClick,
                modifier = Modifier.testTag("calculator-button"),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Calculate,
                    contentDescription = "Buka kalkulator",
                    tint = MaterialTheme.colorScheme.onPrimary,
                )
            }
            Spacer(Modifier.size(4.dp))
        }
        OutlinedButton(
            onClick = onOwnerAccess,
            modifier = Modifier.testTag("owner-access"),
            shape = RoundedCornerShape(10.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp),
        ) {
            Icon(
                imageVector = if (ownerUnlocked) Icons.Outlined.LockOpen else Icons.Outlined.Lock,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.size(4.dp))
            Text(
                text = if (ownerUnlocked) "Mode Owner" else "Mode Kasir",
                color = MaterialTheme.colorScheme.onPrimary,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
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
    ownerUnlocked: Boolean,
    onOwnerAccess: () -> Unit,
    onBack: (() -> Unit)? = null,
    onCalculatorClick: (() -> Unit)? = null,
) {
    Column {
        CashierHeader(
            title = title,
            ownerUnlocked = ownerUnlocked,
            onOwnerAccess = onOwnerAccess,
            onCalculatorClick = onCalculatorClick,
        )
        if (onBack != null) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.padding(start = 8.dp, top = 2.dp),
            ) {
                Icon(
                    Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = "Kembali",
                )
            }
        }
    }
}
