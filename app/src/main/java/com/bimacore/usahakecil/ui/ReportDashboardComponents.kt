package com.bimacore.usahakecil.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material.icons.automirrored.outlined.TrendingDown
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bimacore.usahakecil.data.ReportChartGranularity
import com.bimacore.usahakecil.data.ReportChartMode
import com.bimacore.usahakecil.data.ReportProductMeasure
import com.bimacore.usahakecil.data.ReportPeriod
import com.bimacore.usahakecil.data.ReportProductTrend
import com.bimacore.usahakecil.data.ReportSummary
import com.bimacore.usahakecil.data.ReportTrendPoint
import com.bimacore.usahakecil.data.ReportTrendReport
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun ReportPeriodPicker(
    periods: List<ReportPeriod>,
    selected: ReportPeriod,
    onSelected: (ReportPeriod) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box(Modifier.fillMaxWidth()) {
        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .testTag("report-period-selector"),
            shape = RoundedCornerShape(18.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        ) {
            Icon(Icons.Outlined.CalendarMonth, contentDescription = null)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.Start) {
                Text(selected.label, style = MaterialTheme.typography.titleSmall)
                Text(
                    formatReportRange(selected),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(Icons.Outlined.ExpandMore, contentDescription = "Pilih periode laporan")
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            periods.forEach { period ->
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(period.label, fontWeight = FontWeight.SemiBold)
                            Text(
                                formatReportRange(period),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    },
                    onClick = {
                        expanded = false
                        onSelected(period)
                    },
                    modifier = Modifier.testTag("report-period-option-${period.name}"),
                )
            }
        }
    }
}

@Composable
fun ReportOverviewCard(
    summary: ReportSummary,
    previous: ReportSummary?,
    period: ReportPeriod,
) {
    val comparison = reportComparison(summary.totalSales, previous?.totalSales, period)
    OwnerHeroCard(
        eyebrow = "Omzet ${period.label.lowercase(Locale.forLanguageTag("id-ID"))}",
        value = formatRupiah(summary.totalSales),
        supportingText = comparison.text,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                imageVector = when (comparison.direction) {
                    MetricDirection.POSITIVE -> Icons.AutoMirrored.Outlined.TrendingUp
                    MetricDirection.NEGATIVE -> Icons.AutoMirrored.Outlined.TrendingDown
                    MetricDirection.NEUTRAL -> Icons.Outlined.Remove
                },
                contentDescription = null,
                tint = comparison.color(),
            )
            Text(
                "Dibandingkan pada waktu yang sama",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        Text(
            "Total nilai penjualan, bukan laba.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall,
        )
        OwnerMetricStrip(
            listOf(
                summary.transactionCount.toString() to "Transaksi",
                formatRupiah(summary.cashIn) to "Uang masuk",
                formatRupiah(summary.expenses) to "Pengeluaran",
            ),
        )
    }
}

@Composable
fun ReportSalesMovementCard(
    trend: ReportTrendReport?,
    period: ReportPeriod,
    error: String?,
) {
    val range = remember(period, trend) { period.range() }
    val visiblePoints = remember(trend, range) {
        trend?.points.orEmpty().filter { it.bucketStart in range }.takeLast(12)
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                "Pergerakan omzet ${period.label.lowercase(Locale.forLanguageTag("id-ID"))}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            when {
                error != null -> Text(
                    error,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                )
                trend == null -> Text(
                    "Grafik sedang dimuat.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                visiblePoints.isEmpty() -> Text(
                    "Belum ada penjualan pada periode ini.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                else -> SalesLineChart(visiblePoints, trend.granularity)
            }
        }
    }
}

@Composable
private fun SalesLineChart(
    points: List<ReportTrendPoint>,
    granularity: ReportChartGranularity,
) {
    var selectedIndex by remember(points) { mutableIntStateOf(points.lastIndex) }
    val selected = points[selectedIndex.coerceIn(points.indices)]
    val maximum = points.maxOfOrNull { it.sales }?.coerceAtLeast(1L) ?: 1L
    val lineColor = MaterialTheme.colorScheme.primary
    val gridColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.28f)
    val surfaceColor = MaterialTheme.colorScheme.surface
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .testTag("report-sales-chart")
            .semantics {
                contentDescription = "Grafik omzet. ${formatTrendDate(selected.bucketStart, granularity)} ${formatRupiah(selected.sales)}"
            }
            .pointerInput(points) {
                detectTapGestures { position ->
                    val slot = size.width.toFloat() / points.size.coerceAtLeast(1)
                    selectedIndex = (position.x / slot).toInt().coerceIn(points.indices)
                }
            },
    ) {
        repeat(4) { index ->
            val y = size.height * index / 3f
            drawLine(gridColor, Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
        }
        val path = Path()
        points.forEachIndexed { index, point ->
            val x = if (points.size == 1) size.width / 2f else size.width * index / points.lastIndex
            val y = size.height - (point.sales.toFloat() / maximum * (size.height - 18f)) - 9f
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(path, lineColor, style = Stroke(width = 6f, cap = StrokeCap.Round))
        points.forEachIndexed { index, point ->
            val x = if (points.size == 1) size.width / 2f else size.width * index / points.lastIndex
            val y = size.height - (point.sales.toFloat() / maximum * (size.height - 18f)) - 9f
            drawCircle(
                color = if (index == selectedIndex) surfaceColor else lineColor,
                radius = if (index == selectedIndex) 12f else 8f,
                center = Offset(x, y),
            )
            if (index == selectedIndex) {
                drawCircle(lineColor, radius = 8f, center = Offset(x, y))
            }
        }
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(
            formatTrendDate(points.first().bucketStart, granularity),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            formatTrendDate(points.last().bucketStart, granularity),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    Text(
        "${formatTrendDate(selected.bucketStart, granularity)}: ${formatRupiah(selected.sales)}",
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
    )
    Text(
        "Ketuk grafik untuk melihat angka.",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
fun ReportMetricGrid(
    summary: ReportSummary,
    previous: ReportSummary?,
    includeReceivables: Boolean,
    periodLabel: String,
) {
    val metrics = buildList {
        add(ReportMetricData("Omzet $periodLabel", formatRupiah(summary.totalSales), summary.totalSales, previous?.totalSales, true))
        add(
            ReportMetricData(
                "Jumlah transaksi",
                summary.transactionCount.toString(),
                summary.transactionCount.toLong(),
                previous?.transactionCount?.toLong(),
                true,
            ),
        )
        add(ReportMetricData("Kas masuk tercatat", formatRupiah(summary.cashIn), summary.cashIn, previous?.cashIn, true))
        add(ReportMetricData("Kas keluar tercatat", formatRupiah(summary.cashOut), summary.cashOut, previous?.cashOut, false))
        add(ReportMetricData("Pengeluaran", formatRupiah(summary.expenses), summary.expenses, previous?.expenses, false))
        add(ReportMetricData("Selisih kas", formatRupiah(summary.netCash), summary.netCash, previous?.netCash, true))
        add(ReportMetricData("Saldo utang saat ini", formatRupiah(summary.outstandingPayables), null, null, null))
        if (includeReceivables) {
            add(ReportMetricData("Saldo piutang saat ini", formatRupiah(summary.outstandingReceivables), null, null, null))
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        metrics.chunked(2).forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                rowItems.forEach { metric ->
                    ReportMetricCard(metric, Modifier.weight(1f))
                }
                if (rowItems.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun ReportMetricCard(
    metric: ReportMetricData,
    modifier: Modifier,
) {
    val delta = metric.delta()
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = delta.containerColor()),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Text(
                metric.label,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelLarge,
                minLines = 2,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                metric.value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                delta.text,
                color = delta.accentColor(),
                style = MaterialTheme.typography.labelMedium,
                minLines = 2,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
fun ReportTrendSection(
    trend: ReportTrendReport?,
    mode: ReportChartMode,
    granularity: ReportChartGranularity,
    productMeasure: ReportProductMeasure,
    selectedProductId: Long?,
    error: String?,
    onModeSelected: (ReportChartMode) -> Unit,
    onGranularitySelected: (ReportChartGranularity) -> Unit,
    onProductMeasureSelected: (ReportProductMeasure) -> Unit,
    onProductSelected: (Long?) -> Unit,
) {
    SectionTitle("Grafik laporan")
    Text(
        "Pilih data yang ingin dipantau.",
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ReportChoiceMenu(
            label = "Tampilan",
            selected = mode,
            options = ReportChartMode.values().toList(),
            optionLabel = { it.label },
            onSelected = onModeSelected,
            testTag = "report-chart-mode",
            optionTagPrefix = "report-chart-mode-option",
            modifier = Modifier.weight(1f),
        )
        ReportChoiceMenu(
            label = "Rentang",
            selected = granularity,
            options = ReportChartGranularity.values().toList(),
            optionLabel = { it.label },
            onSelected = onGranularitySelected,
            testTag = "report-chart-granularity",
            optionTagPrefix = "report-chart-granularity-option",
            modifier = Modifier.weight(1f),
        )
    }

    val products = trend?.products.orEmpty()
    if (mode == ReportChartMode.PRODUCT) {
        val selectedProduct = selectedProductId?.let { id ->
            products.firstOrNull { it.productId == id }?.let {
                ProductOption.Specific(it.productId, it.productName)
            }
        } ?: ProductOption.All
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("report-product-selector"),
        ) {
            ReportProductSettingsMenu(
                selectedProduct = selectedProduct,
                products = products,
                measure = productMeasure,
                onProductSelected = onProductSelected,
                onMeasureSelected = onProductMeasureSelected,
            )
        }
    }

    when {
        error != null -> InfoCard("Grafik belum tersedia", error)
        trend == null -> InfoCard("Grafik belum tersedia", "Buka ulang Laporan untuk memuat grafik.")
        mode == ReportChartMode.CASH_FLOW -> CashFlowTrendCard(trend)
        mode == ReportChartMode.SALES -> SalesTrendCard(trend)
        else -> ProductTrendCard(trend, selectedProductId, productMeasure)
    }
}

@Composable
private fun <T> ReportChoiceMenu(
    label: String,
    selected: T,
    options: List<T>,
    optionLabel: (T) -> String,
    onSelected: (T) -> Unit,
    testTag: String,
    optionTagPrefix: String,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth().testTag(testTag),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
        ) {
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.Start) {
                Text(label, style = MaterialTheme.typography.labelSmall)
                Text(
                    optionLabel(selected),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.labelLarge,
                )
            }
            Icon(
                imageVector = Icons.Outlined.ExpandMore,
                contentDescription = "Buka pilihan $label",
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(optionLabel(option)) },
                    onClick = {
                        expanded = false
                        onSelected(option)
                    },
                    modifier = Modifier.testTag("$optionTagPrefix-${optionLabel(option)}"),
                )
            }
        }
    }
}

@Composable
private fun ReportProductSettingsMenu(
    selectedProduct: ProductOption,
    products: List<ReportProductTrend>,
    measure: ReportProductMeasure,
    onProductSelected: (Long?) -> Unit,
    onMeasureSelected: (ReportProductMeasure) -> Unit,
    testTag: String = "report-product-measure",
) {
    var expanded by remember { mutableStateOf(false) }
    val displayedMeasure = if (selectedProduct == ProductOption.All) ReportProductMeasure.SALES else measure
    val productOptions = listOf<ProductOption>(ProductOption.All) + products.map {
        ProductOption.Specific(it.productId, it.productName)
    }
    Box {
        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth().testTag(testTag),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
        ) {
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.Start) {
                Text("Produk", style = MaterialTheme.typography.labelSmall)
                Text(
                    "${selectedProduct.label()} / ${displayedMeasure.label}",
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.labelLarge,
                )
            }
            Icon(
                imageVector = Icons.Outlined.ExpandMore,
                contentDescription = "Buka pilihan produk dan ukuran",
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            Text(
                "Produk",
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            productOptions.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.label()) },
                    onClick = {
                        expanded = false
                        val productId = (option as? ProductOption.Specific)?.id
                        onProductSelected(productId)
                        if (productId == null && measure == ReportProductMeasure.QUANTITY) {
                            onMeasureSelected(ReportProductMeasure.SALES)
                        }
                    },
                    modifier = Modifier.testTag("report-product-selector-option-${option.label()}"),
                )
            }
            HorizontalDivider()
            Text(
                "Tampilkan sebagai",
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            ReportProductMeasure.values()
                .filter { selectedProduct != ProductOption.All || it != ReportProductMeasure.QUANTITY }
                .forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.label) },
                    onClick = {
                        expanded = false
                        onMeasureSelected(option)
                    },
                    modifier = Modifier.testTag("report-product-measure-option-${option.label}"),
                )
            }
        }
    }
}

@Composable
private fun CashFlowTrendCard(trend: ReportTrendReport) {
    val points = trend.points
    var selectedIndex by remember(points) {
        mutableIntStateOf((points.size - 1).coerceAtLeast(0))
    }
    val maxValue = points.maxOfOrNull { maxOf(it.cashIn, it.cashOut) }?.coerceAtLeast(1L) ?: 1L
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Arus kas tercatat", style = MaterialTheme.typography.titleMedium)
            Text(
                "Kas masuk dan kas keluar per ${trend.granularity.label.lowercase(Locale.forLanguageTag("id-ID"))}.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
            TrendLegend()
            CashFlowBars(
                points = points,
                maxValue = maxValue,
                selectedIndex = selectedIndex,
                granularity = trend.granularity,
                onSelected = { selectedIndex = it },
            )
            val selected = points[selectedIndex]
            Text(
                "${formatTrendDate(selected.bucketStart, trend.granularity)}: masuk ${formatRupiah(selected.cashIn)} • keluar ${formatRupiah(selected.cashOut)} • bersih ${formatRupiah(selected.netCash)}",
                style = MaterialTheme.typography.labelMedium,
            )
            Text(
                "Ketuk batang untuk melihat angka periode tersebut.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }
}

@Composable
private fun SalesTrendCard(trend: ReportTrendReport) {
    val points = trend.points
    var selectedIndex by remember(points) {
        mutableIntStateOf((points.size - 1).coerceAtLeast(0))
    }
    val maxValue = points.maxOfOrNull { it.sales }?.coerceAtLeast(1L) ?: 1L
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Penjualan", style = MaterialTheme.typography.titleMedium)
            Text(
                "Omzet dan transaksi per ${trend.granularity.label.lowercase(Locale.forLanguageTag("id-ID"))}.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
            TrendBars(
                points = points,
                maxValue = maxValue,
                selectedIndex = selectedIndex,
                granularity = trend.granularity,
                value = { it.sales },
                barColor = Color(0xFF2E7D32),
                onSelected = { selectedIndex = it },
            )
            val selected = points[selectedIndex]
            Text(
                "${formatTrendDate(selected.bucketStart, trend.granularity)}: ${formatRupiah(selected.sales)} • ${selected.transactionCount} transaksi",
                style = MaterialTheme.typography.labelMedium,
            )
            Text(
                "Ketuk batang untuk melihat angka periode tersebut.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }
}

@Composable
private fun ProductTrendCard(
    trend: ReportTrendReport,
    selectedProductId: Long?,
    measure: ReportProductMeasure,
) {
    val selectedProduct = selectedProductId?.let { id -> trend.products.firstOrNull { it.productId == id } }
    val series = selectedProduct ?: aggregateProductTrend(trend.products, trend.points)
    val effectiveMeasure = if (selectedProduct == null) ReportProductMeasure.SALES else measure
    val points = series.points
    var selectedIndex by remember(points) {
        mutableIntStateOf((points.size - 1).coerceAtLeast(0))
    }
    val maxValue = points.maxOfOrNull { point ->
        if (effectiveMeasure == ReportProductMeasure.SALES) point.sales else point.quantity
    }?.coerceAtLeast(1L) ?: 1L
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Penjualan produk", style = MaterialTheme.typography.titleMedium)
            Text(
                "${series.productName} • ${effectiveMeasure.label} per ${trend.granularity.label.lowercase(Locale.forLanguageTag("id-ID"))}",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
            if (points.isNotEmpty()) {
                TrendBars(
                    points = points,
                    maxValue = maxValue,
                    selectedIndex = selectedIndex,
                    granularity = trend.granularity,
                    value = { point ->
                        if (effectiveMeasure == ReportProductMeasure.SALES) point.sales else point.quantity
                    },
                    barColor = Color(0xFF1565C0),
                    onSelected = { selectedIndex = it },
                )
                val selected = points[selectedIndex]
                val selectedValue = if (effectiveMeasure == ReportProductMeasure.SALES) {
                    formatRupiah(selected.sales)
                } else {
                    "${selected.quantity} ${series.unitLabel}"
                }
                Text(
                    "${formatTrendDate(selected.bucketStart, trend.granularity)}: $selectedValue",
                    style = MaterialTheme.typography.labelMedium,
                )
            } else {
                Text("Belum ada penjualan produk pada periode ini.")
            }
        }
    }
}

@Composable
private fun TrendBars(
    points: List<ReportTrendPoint>,
    maxValue: Long,
    selectedIndex: Int,
    granularity: ReportChartGranularity,
    value: (ReportTrendPoint) -> Long,
    barColor: Color,
    onSelected: (Int) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().height(176.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        points.forEachIndexed { index, point ->
            val amount = value(point)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clickable { onSelected(index) },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom,
            ) {
                if (index == selectedIndex) {
                    Text(
                        formatChartValue(amount),
                        maxLines = 1,
                        overflow = TextOverflow.Clip,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                        textAlign = TextAlign.Center,
                    )
                } else {
                    Spacer(Modifier.height(16.dp))
                }
                Spacer(Modifier.height(3.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height((8f + 104f * amount.toFloat().div(maxValue).coerceIn(0f, 1f)).dp)
                        .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                        .background(if (index == selectedIndex) barColor else barColor.copy(alpha = 0.55f)),
                )
                Spacer(Modifier.height(5.dp))
                Text(
                    formatTrendAxisDate(point.bucketStart, granularity),
                    maxLines = 1,
                    overflow = TextOverflow.Clip,
                    style = MaterialTheme.typography.labelSmall,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun CashFlowBars(
    points: List<ReportTrendPoint>,
    maxValue: Long,
    selectedIndex: Int,
    granularity: ReportChartGranularity,
    onSelected: (Int) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().height(176.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        points.forEachIndexed { index, point ->
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clickable { onSelected(index) },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom,
            ) {
                if (index == selectedIndex) {
                    Text(
                        formatChartValue(maxOf(point.cashIn, point.cashOut)),
                        maxLines = 1,
                        overflow = TextOverflow.Clip,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                        textAlign = TextAlign.Center,
                    )
                } else {
                    Spacer(Modifier.height(16.dp))
                }
                Spacer(Modifier.height(3.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().height(112.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment = Alignment.Bottom,
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height((8f + 104f * point.cashIn.toFloat().div(maxValue).coerceIn(0f, 1f)).dp)
                            .clip(RoundedCornerShape(topStart = 5.dp, topEnd = 5.dp))
                            .background(Color(0xFF2E7D32)),
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height((8f + 104f * point.cashOut.toFloat().div(maxValue).coerceIn(0f, 1f)).dp)
                            .clip(RoundedCornerShape(topStart = 5.dp, topEnd = 5.dp))
                            .background(Color(0xFFC62828)),
                    )
                }
                Spacer(Modifier.height(5.dp))
                Text(
                    formatTrendAxisDate(point.bucketStart, granularity),
                    maxLines = 1,
                    overflow = TextOverflow.Clip,
                    style = MaterialTheme.typography.labelSmall,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun TrendLegend() {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("● Masuk", color = Color(0xFF2E7D32), style = MaterialTheme.typography.labelSmall)
        Text("● Keluar", color = Color(0xFFC62828), style = MaterialTheme.typography.labelSmall)
    }
}

private fun aggregateProductTrend(
    products: List<ReportProductTrend>,
    fallbackPoints: List<ReportTrendPoint>,
): ReportProductTrend {
    val first = products.firstOrNull()
    if (first == null) {
        return ReportProductTrend(
            productId = -1L,
            productName = "Semua produk",
            unitLabel = "unit",
            points = fallbackPoints.map { point ->
                point.copy(sales = 0, quantity = 0, transactionCount = 0)
            },
        )
    }
    return ReportProductTrend(
        productId = -1L,
        productName = "Semua produk",
        unitLabel = first.unitLabel,
        points = first.points.indices.map { index ->
            ReportTrendPoint(
                bucketStart = first.points[index].bucketStart,
                sales = products.sumOf { it.points[index].sales },
                quantity = products.sumOf { it.points[index].quantity },
            )
        },
    )
}

private sealed interface ProductOption {
    data object All : ProductOption
    data class Specific(val id: Long, val name: String) : ProductOption

    fun label(): String = when (this) {
        All -> "Semua produk"
        is Specific -> name
    }
}

private data class ReportMetricData(
    val label: String,
    val value: String,
    val current: Long?,
    val previous: Long?,
    val higherIsBetter: Boolean?,
) {
    fun delta(): MetricDelta {
        if (current == null || previous == null) {
            return MetricDelta("Saldo berjalan", MetricDirection.NEUTRAL)
        }
        val difference = current - previous
        if (difference == 0L) return MetricDelta("Tidak berubah", MetricDirection.NEUTRAL)
        if (previous == 0L) {
            val direction = if (higherIsBetter == (difference > 0)) {
                MetricDirection.POSITIVE
            } else {
                MetricDirection.NEGATIVE
            }
            return MetricDelta(
                if (difference > 0) "Baru ada data" else "Turun ke Rp0",
                direction,
            )
        }
        val percent = kotlin.math.round(
            kotlin.math.abs(difference).toDouble() / kotlin.math.abs(previous).coerceAtLeast(1L) * 100,
        ).toInt()
        val improved = if (higherIsBetter == true) difference > 0 else difference < 0
        return MetricDelta(
            "${if (difference > 0) "↑" else "↓"} ${percent}% vs sebelumnya",
            if (improved) MetricDirection.POSITIVE else MetricDirection.NEGATIVE,
        )
    }
}

private data class MetricDelta(
    val text: String,
    val direction: MetricDirection,
)

private enum class MetricDirection {
    POSITIVE,
    NEGATIVE,
    NEUTRAL,
}

private fun MetricDelta.accentColor(): Color = when (direction) {
    MetricDirection.POSITIVE -> Color(0xFF2E7D32)
    MetricDirection.NEGATIVE -> Color(0xFFC62828)
    MetricDirection.NEUTRAL -> Color(0xFF5F6368)
}

@Composable
private fun MetricDelta.containerColor(): Color = when (direction) {
    MetricDirection.POSITIVE -> Color(0xFFE8F5E9)
    MetricDirection.NEGATIVE -> Color(0xFFFFEBEE)
    MetricDirection.NEUTRAL -> MaterialTheme.colorScheme.surfaceVariant
}

private fun formatChartValue(value: Long): String = when {
    value >= 1_000_000L -> "${value / 1_000_000}jt"
    value >= 1_000L -> (value / 1_000).toString()
    else -> value.toString()
}

private fun formatTrendDate(
    timestamp: Long,
    granularity: ReportChartGranularity,
): String = SimpleDateFormat(
    when (granularity) {
        ReportChartGranularity.DAILY,
        ReportChartGranularity.WEEKLY,
        -> "dd/MM"
        ReportChartGranularity.MONTHLY -> "MMM"
        ReportChartGranularity.YEARLY -> "yyyy"
    },
    Locale.forLanguageTag("id-ID"),
).format(Date(timestamp))

private fun formatTrendAxisDate(
    timestamp: Long,
    granularity: ReportChartGranularity,
): String = SimpleDateFormat(
    when (granularity) {
        ReportChartGranularity.DAILY -> "dd"
        ReportChartGranularity.WEEKLY -> "dd/MM"
        ReportChartGranularity.MONTHLY -> "MMM"
        ReportChartGranularity.YEARLY -> "yyyy"
    },
    Locale.forLanguageTag("id-ID"),
).format(Date(timestamp))

private fun formatReportRange(
    period: ReportPeriod,
    now: Long = System.currentTimeMillis(),
): String {
    val range = period.range(now)
    val start = Calendar.getInstance().apply { timeInMillis = range.first }
    val end = Calendar.getInstance().apply { timeInMillis = range.last }
    val locale = Locale.forLanguageTag("id-ID")
    if (period == ReportPeriod.DAY) {
        return SimpleDateFormat("d MMM yyyy", locale).format(start.time)
    }
    val sameMonth = start.get(Calendar.MONTH) == end.get(Calendar.MONTH) &&
        start.get(Calendar.YEAR) == end.get(Calendar.YEAR)
    return if (sameMonth) {
        "${SimpleDateFormat("d", locale).format(start.time)} - ${SimpleDateFormat("d MMM yyyy", locale).format(end.time)}"
    } else {
        "${SimpleDateFormat("d MMM", locale).format(start.time)} - ${SimpleDateFormat("d MMM yyyy", locale).format(end.time)}"
    }
}

private fun reportComparison(
    current: Long,
    previous: Long?,
    period: ReportPeriod,
): ReportComparison {
    if (previous == null) return ReportComparison("Perbandingan belum tersedia", MetricDirection.NEUTRAL)
    val previousLabel = when (period) {
        ReportPeriod.DAY -> "kemarin"
        ReportPeriod.WEEK -> "minggu lalu"
        ReportPeriod.MONTH -> "bulan lalu"
        ReportPeriod.YEAR -> "tahun lalu"
    }
    if (current == previous) {
        return ReportComparison("Sama dengan $previousLabel", MetricDirection.NEUTRAL)
    }
    if (previous == 0L) {
        return ReportComparison(
            if (current > 0L) "Baru ada penjualan" else "Belum ada penjualan",
            if (current > 0L) MetricDirection.POSITIVE else MetricDirection.NEUTRAL,
        )
    }
    val percent = kotlin.math.round(
        kotlin.math.abs(current - previous).toDouble() / kotlin.math.abs(previous).coerceAtLeast(1L) * 100,
    ).toInt()
    return ReportComparison(
        "${if (current > previous) "Naik" else "Turun"} $percent% dari $previousLabel",
        if (current > previous) MetricDirection.POSITIVE else MetricDirection.NEGATIVE,
    )
}

private data class ReportComparison(
    val text: String,
    val direction: MetricDirection,
)

@Composable
private fun ReportComparison.color(): Color = when (direction) {
    MetricDirection.POSITIVE -> Color(0xFF2E7D32)
    MetricDirection.NEGATIVE -> MaterialTheme.colorScheme.error
    MetricDirection.NEUTRAL -> MaterialTheme.colorScheme.onSurfaceVariant
}
