package com.bimacore.usahakecil.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.bimacore.usahakecil.data.ProductForecast
import com.bimacore.usahakecil.data.ProductForecastReport
import com.bimacore.usahakecil.domain.forecast.ForecastPoint
import com.bimacore.usahakecil.domain.forecast.SalesForecastModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.abs

@Composable
fun SalesForecastSection(
    report: ProductForecastReport?,
    isLoading: Boolean,
    error: String?,
    onLoadForecast: (() -> Unit)? = null,
) {
    if (report == null && !isLoading && error == null && onLoadForecast != null) {
        androidx.compose.runtime.LaunchedEffect(Unit) {
            onLoadForecast()
        }
    }
    SectionTitle("Perkiraan penjualan")
    Text(
        "Perkiraan 7 hari dari histori penjualan di HP ini. Hasilnya membantu rencana stok, bukan janji penjualan.",
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    when {
        isLoading -> Text("Menyiapkan perkiraan...")
        error != null -> InfoCard("Prediksi belum tersedia", error)
        report == null -> Text("Belum ada data perkiraan.")
        report.products.none { it.result != null } -> {
            InfoCard(
                "Data belum cukup",
                "Simpan penjualan pada beberapa hari berbeda agar perkiraan bisa dihitung.",
            )
        }
        else -> {
            report.products
                .filter { it.result != null }
                .sortedByDescending { item ->
                    item.result?.forecast?.sumOf(ForecastPoint::expectedQuantity) ?: 0.0
                }
                .take(MAX_VISIBLE_FORECASTS)
                .forEach { product -> ForecastProductCard(product) }

            val insufficient = report.products.count { it.result == null }
            if (insufficient > 0) {
                Text(
                    "$insufficient produk belum punya histori yang cukup untuk diperkirakan.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun ForecastProductCard(product: ProductForecast) {
    val result = requireNotNull(product.result)
    val total = result.forecast.sumOf(ForecastPoint::expectedQuantity)
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(product.productName, style = MaterialTheme.typography.titleMedium)
            Text(
                "Perkiraan 7 hari: ${formatForecastQuantity(total)} ${product.unitLabel}",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                "Dasar hitung: ${modelLabel(result.selectedCandidate.model)} - riwayat ${result.normalizedHistoryDays} hari - selisih rata-rata ${formatForecastQuantity(result.selectedCandidate.metrics.mae)}",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            ForecastBarChart(result.forecast, product.unitLabel)
        }
    }
}

@Composable
private fun ForecastBarChart(points: List<ForecastPoint>, unitLabel: String) {
    val maxValue = points.maxOfOrNull { it.expectedQuantity }?.takeIf { it > 0.0 } ?: 1.0
    val minValue = points.minOfOrNull { it.expectedQuantity } ?: 0.0
    Text(
        "Nilai per hari ($unitLabel) - skala ${formatForecastQuantity(minValue)} - ${formatForecastQuantity(maxValue)}",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Row(
        modifier = Modifier.fillMaxWidth().height(142.dp),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        points.forEach { point ->
            val fraction = (point.expectedQuantity / maxValue).toFloat().coerceIn(0f, 1f)
            Column(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom,
            ) {
                Text(
                    formatForecastQuantity(point.expectedQuantity),
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.labelSmall,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(3.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height((8f + fraction * 70f).dp)
                        .clip(RoundedCornerShape(topStart = 5.dp, topEnd = 5.dp))
                        .background(MaterialTheme.colorScheme.primary),
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    formatForecastDay(point.epochDay),
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.labelSmall,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

private fun modelLabel(model: SalesForecastModel): String = when (model) {
    SalesForecastModel.MOVING_AVERAGE -> "rata-rata bergerak"
    SalesForecastModel.SIMPLE_EXPONENTIAL_SMOOTHING -> "pemusatan penjualan"
    SalesForecastModel.HOLT_LINEAR -> "tren penjualan"
    SalesForecastModel.HOLT_WINTERS_ADDITIVE -> "tren dan pola mingguan"
    SalesForecastModel.CROSTON_SBA -> "penjualan jarang"
}

private fun formatForecastQuantity(value: Double): String {
    val rounded = if (abs(value - value.toLong()) < 0.05) {
        value.toLong().toString()
    } else {
        String.format(Locale.US, "%.1f", value)
    }
    return rounded
}

private fun formatForecastDay(epochDay: Long): String {
    val date = Date(epochDay * MILLIS_PER_DAY)
    return SimpleDateFormat("dd/MM", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }.format(date)
}

private const val MAX_VISIBLE_FORECASTS = 5
private const val MILLIS_PER_DAY = 86_400_000L
