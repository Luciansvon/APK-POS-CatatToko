package com.bimacore.usahakecil.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.PointOfSale
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.ViewInAr
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Owner landing surface for the Operasional destination.
 *
 * The screenshot reference is intentionally limited to this overview. Existing
 * management screens keep their established tabs and security flow.
 */
@Composable
fun OwnerOperationsOverview(
    viewModel: OperationsViewModel,
    onOpenOperations: (String) -> Unit,
    onOpenFinance: () -> Unit,
    onOpenReports: () -> Unit,
    onOpenMore: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val ownerDaySummary by viewModel.ownerDaySummary.collectAsState()
    val products by viewModel.products.collectAsState()
    val variants by viewModel.variants.collectAsState()
    val metrics = ownerOverviewMetrics(ownerDaySummary)

    LaunchedEffect(Unit) {
        viewModel.refreshOwnerDaySummary()
    }

    val stockAlerts = products.asSequence()
            .filter { it.isActive && it.stockTrackingEnabled }
            .map { product ->
                val stock = if (product.hasVariants) {
                    variants.filter { it.productId == product.id && it.isActive }.sumOf { it.stock }
                } else {
                    product.stock
                }
                product to stock
            }
            .filter { (product, stock) -> stock <= product.lowStockThreshold }
            .toList()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .testTag("owner-overview"),
    ) {
        OwnerOverviewHeader(onOpenMore = onOpenMore)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "Ringkasan usaha",
                    style = MaterialTheme.typography.titleLarge.copy(fontSize = 21.sp, lineHeight = 26.sp),
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.testTag("owner-overview-title"),
                )
                Text(
                    text = "Data tersimpan di perangkat",
                    style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp, lineHeight = 20.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val heroWeight = if (maxWidth < 360.dp) 1.25f else 1.4f
                val sideWeight = 1f
                Row(
                    modifier = Modifier.fillMaxWidth().height(206.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OverviewMetricCard(
                        modifier = Modifier
                            .weight(heroWeight)
                            .fillMaxHeight()
                            .testTag("overview-sales"),
                        icon = Icons.Outlined.BarChart,
                        iconDescription = "Omzet",
                        label = "Omzet hari ini",
                        value = metrics?.totalSales?.let(::formatCompactRupiah) ?: "Memuat…",
                        supportingText = "Total nilai penjualan",
                        emphasis = true,
                        onClick = onOpenReports,
                    )
                    Column(
                        modifier = Modifier.weight(sideWeight).fillMaxHeight(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        OverviewSmallMetricCard(
                            modifier = Modifier
                                .weight(1f)
                                .testTag("overview-transactions"),
                            icon = Icons.AutoMirrored.Outlined.ReceiptLong,
                            iconDescription = "Transaksi",
                            label = "Transaksi",
                            value = metrics?.transactionCount?.toString() ?: "…",
                            onClick = onOpenReports,
                        )
                        OverviewSmallMetricCard(
                            modifier = Modifier
                                .weight(1f)
                                .testTag("overview-cash-in"),
                            icon = Icons.Outlined.AccountBalanceWallet,
                            iconDescription = "Uang masuk",
                            label = "Uang masuk",
                            value = metrics?.cashIn?.let(::formatCompactRupiah) ?: "Memuat…",
                            onClick = onOpenFinance,
                        )
                    }
                }
            }

            OverviewStockCard(
                alertCount = stockAlerts.size,
                onClick = { onOpenOperations("Stok") },
                modifier = Modifier.testTag("overview-stock-alert"),
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OverviewShortcut(
                    modifier = Modifier.weight(1f).testTag("overview-shortcut-products"),
                    icon = Icons.Outlined.ViewInAr,
                    label = "Produk",
                    onClick = { onOpenOperations("Produk") },
                )
                OverviewShortcut(
                    modifier = Modifier.weight(1f).testTag("overview-shortcut-purchases"),
                    icon = Icons.Outlined.ShoppingCart,
                    label = "Pembelian",
                    onClick = { onOpenOperations("Pembelian") },
                )
                OverviewShortcut(
                    modifier = Modifier.weight(1f).testTag("overview-shortcut-cash"),
                    icon = Icons.Outlined.PointOfSale,
                    label = "Kas",
                    onClick = onOpenFinance,
                )
                OverviewShortcut(
                    modifier = Modifier.weight(1f).testTag("overview-shortcut-workers"),
                    icon = Icons.Outlined.Group,
                    label = "Pekerja",
                    onClick = { onOpenOperations("Pekerja") },
                )
            }

            OverviewLinkCard(
                title = "Lihat laporan",
                subtitle = "Omzet, pembayaran, kas, dan pengeluaran",
                icon = Icons.Outlined.Description,
                onClick = onOpenReports,
                modifier = Modifier.testTag("overview-report-link"),
            )
            OverviewLinkCard(
                title = "Backup & Keamanan",
                subtitle = "Pastikan data usaha memiliki salinan cadangan",
                icon = Icons.Outlined.Security,
                secondaryIcon = Icons.Outlined.Storage,
                onClick = onOpenMore,
                showArrow = false,
                modifier = Modifier.testTag("overview-backup-link"),
            )

        }
    }
}

@Composable
private fun OwnerOverviewHeader(onOpenMore: () -> Unit) {
    CatatTokoOwnerHeader(
        onOwnerAction = onOpenMore,
        testTag = "owner-overview-header",
        ownerActionTestTag = "owner-overview-owner-action",
    )
}

@Composable
private fun OverviewMetricCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconDescription: String,
    label: String,
    value: String,
    supportingText: String,
    emphasis: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BentoCard(
        modifier = modifier.clickable(onClick = onClick),
        containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.06f),
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            OverviewIconCircle(
                icon = icon,
                contentDescription = iconDescription,
                tint = MaterialTheme.colorScheme.primary,
                container = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                size = 44.dp,
            )
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp, lineHeight = 22.sp),
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = value,
                    style = if (emphasis) {
                        MaterialTheme.typography.displaySmall.copy(fontSize = 42.sp)
                    } else {
                        MaterialTheme.typography.headlineMedium
                    },
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = supportingText,
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp, lineHeight = 18.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun OverviewSmallMetricCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconDescription: String,
    label: String,
    value: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BentoCard(
        modifier = modifier.clickable(onClick = onClick),
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            OverviewIconCircle(
                icon = icon,
                contentDescription = iconDescription,
                tint = MaterialTheme.colorScheme.primary,
                container = MaterialTheme.colorScheme.primary.copy(alpha = 0.07f),
                size = 40.dp,
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp, lineHeight = 17.sp),
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleLarge.copy(fontSize = 21.sp, lineHeight = 24.sp),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun OverviewStockCard(
    alertCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val warning = Color(0xFFA15C00)
    BentoCard(
        modifier = modifier
            .fillMaxWidth()
            .height(95.dp)
            .clickable(onClick = onClick),
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            OverviewIconCircle(
                icon = Icons.Outlined.ViewInAr,
                contentDescription = "Stok",
                tint = warning,
                container = warning.copy(alpha = 0.12f),
                size = 54.dp,
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Stok perlu perhatian",
                    style = MaterialTheme.typography.titleMedium.copy(fontSize = 17.sp, lineHeight = 21.sp),
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = when (alertCount) {
                        0 -> "Belum ada stok menipis"
                        1 -> "1 produk perlu diisi ulang"
                        else -> "$alertCount produk perlu diisi ulang"
                    },
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp, lineHeight = 18.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun OverviewShortcut(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BentoCard(
        modifier = modifier.height(100.dp).clickable(onClick = onClick),
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 4.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(34.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp, lineHeight = 18.sp),
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun OverviewLinkCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    secondaryIcon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    onClick: () -> Unit,
    showArrow: Boolean = true,
    modifier: Modifier = Modifier,
) {
    BentoCard(
        modifier = modifier
            .fillMaxWidth()
            .height(76.dp)
            .clickable(onClick = onClick),
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            OverviewIconCircle(
                icon = icon,
                secondaryIcon = secondaryIcon,
                contentDescription = title,
                tint = MaterialTheme.colorScheme.primary,
                container = MaterialTheme.colorScheme.primary.copy(alpha = 0.07f),
                size = 46.dp,
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(fontSize = 17.sp, lineHeight = 20.sp),
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 14.sp, lineHeight = 17.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (showArrow) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                    contentDescription = "Buka $title",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp),
                )
            }
        }
    }
}

@Composable
private fun OverviewIconCircle(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    secondaryIcon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    contentDescription: String,
    tint: Color,
    container: Color,
    size: androidx.compose.ui.unit.Dp = 64.dp,
) {
    Box(
        modifier = Modifier.size(size).background(container, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            modifier = Modifier
                .align(Alignment.Center)
                .size(if (secondaryIcon == null) size * 0.56f else size * 0.52f),
            tint = tint,
        )
        if (secondaryIcon != null) {
            Icon(
                imageVector = secondaryIcon,
                contentDescription = null,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(size * 0.38f),
                tint = tint,
            )
        }
    }
}

@Composable
private fun BentoCard(
    modifier: Modifier,
    containerColor: Color,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .background(containerColor, RoundedCornerShape(11.dp))
            .border(
                BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.55f)),
                RoundedCornerShape(11.dp),
            ),
    ) {
        content()
    }
}
