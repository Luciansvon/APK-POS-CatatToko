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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.bimacore.usahakecil.data.PaymentAggregate
import com.bimacore.usahakecil.domain.PaymentMethod

@Composable
fun PaymentMethodBarChart(
    payments: List<PaymentAggregate>,
    modifier: Modifier = Modifier,
) {
    val totalsByMethod = payments.associate { it.paymentMethod to it.total }
    val chartPayments = PaymentMethod.entries.map { method ->
        PaymentAggregate(
            paymentMethod = method.name,
            total = totalsByMethod[method.name] ?: 0L,
        )
    }
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("payment-method-chart"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text("Grafik penerimaan", style = MaterialTheme.typography.titleMedium)
            Text(
                "Perbandingan nominal berdasarkan metode pembayaran.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
            val maxValue = chartPayments.maxOf { it.total }.coerceAtLeast(1L)
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(176.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                chartPayments.forEach { payment ->
                    val fraction = (payment.total.toDouble() / maxValue).toFloat()
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Bottom,
                    ) {
                        Text(
                            formatRupiah(payment.total),
                            modifier = Modifier.width(76.dp),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        )
                        Spacer(Modifier.height(5.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height((8f + fraction * 102f).dp)
                                .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                                .background(MaterialTheme.colorScheme.primary),
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            paymentMethodLabel(payment.paymentMethod),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.labelMedium,
                        )
                    }
                }
            }
        }
    }
}

private fun paymentMethodLabel(value: String): String = runCatching {
    PaymentMethod.valueOf(value).displayName()
}.getOrDefault(value)
