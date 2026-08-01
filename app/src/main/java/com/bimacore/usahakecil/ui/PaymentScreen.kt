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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Backspace
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bimacore.usahakecil.domain.MoneyMath
import com.bimacore.usahakecil.domain.PaymentMethod
import com.bimacore.usahakecil.data.PartyEntity
import com.bimacore.usahakecil.ui.theme.BrandColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentScreen(
    total: Long,
    method: PaymentMethod,
    cashInput: String,
    externalConfirmed: Boolean,
    allowCredit: Boolean,
    customers: List<PartyEntity>,
    selectedCustomerId: Long?,
    isSaving: Boolean,
    onBack: () -> Unit,
    ownerUnlocked: Boolean,
    onOwnerAccess: () -> Unit,
    onMethodSelected: (PaymentMethod) -> Unit,
    onCashDigit: (Char) -> Unit,
    onCashDelete: () -> Unit,
    onCashAmount: (Long) -> Unit,
    onExternalConfirmed: (Boolean) -> Unit,
    onCustomerSelected: (Long?) -> Unit,
    onComplete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val amountReceived = cashInput.toLongOrNull() ?: 0L
    val shortage = MoneyMath.shortage(total, amountReceived)
    val change = MoneyMath.change(total, amountReceived)
    val canComplete = !isSaving && when (method) {
        PaymentMethod.CASH -> amountReceived >= total && total > 0
        PaymentMethod.QRIS,
        PaymentMethod.TRANSFER,
        -> externalConfirmed && total > 0
        PaymentMethod.CREDIT ->
            selectedCustomerId != null && amountReceived in 0..total && total > 0
    }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            CashierFlowHeader(
                title = "Pembayaran",
                ownerUnlocked = ownerUnlocked,
                onOwnerAccess = onOwnerAccess,
                onBack = if (isSaving) null else onBack,
            )
        },
        bottomBar = {
            Button(
                onClick = onComplete,
                enabled = canComplete,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .height(56.dp)
                    .testTag("complete-sale"),
                shape = RoundedCornerShape(14.dp),
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                } else {
                    Icon(Icons.Outlined.CheckCircle, contentDescription = null)
                    Spacer(Modifier.size(8.dp))
                    Text("Bayar & Selesai", fontWeight = FontWeight.Bold)
                }
            }
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag("payment-list"),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                TotalPaymentCard(total)
            }
            item {
                Text(
                    text = "Metode Pembayaran",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    PaymentMethod.entries.filter {
                        it != PaymentMethod.CREDIT || allowCredit
                    }.forEach { option ->
                        FilterChip(
                            selected = method == option,
                            onClick = { onMethodSelected(option) },
                            label = { Text(option.displayName()) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
            if (method == PaymentMethod.CASH) {
                item {
                    CashAmountCard(
                        total = total,
                        amountReceived = amountReceived,
                        shortage = shortage,
                        change = change,
                    )
                }
                item {
                    QuickCashAmounts(
                        total = total,
                        amountReceived = amountReceived,
                        onAmount = onCashAmount,
                    )
                }
                item { NumericKeypad(onCashDigit, onCashDelete) }
            } else if (method == PaymentMethod.CREDIT) {
                item {
                    CustomerSelection(
                        customers = customers,
                        selectedCustomerId = selectedCustomerId,
                        onCustomerSelected = onCustomerSelected,
                    )
                }
                item { CreditAmountCard(total, amountReceived) }
                item { QuickCreditAmounts(total, amountReceived, onCashAmount) }
                item { NumericKeypad(onCashDigit, onCashDelete) }
            } else {
                item {
                    ExternalPaymentConfirmation(
                        method = method,
                        confirmed = externalConfirmed,
                        onConfirmed = onExternalConfirmed,
                    )
                }
            }
        }
    }
}

@Composable
private fun CustomerSelection(
    customers: List<PartyEntity>,
    selectedCustomerId: Long?,
    onCustomerSelected: (Long?) -> Unit,
) {
    val activeCustomers = remember(customers) { customers.filter { it.isActive } }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Pilih pelanggan", fontWeight = FontWeight.Bold)
        if (activeCustomers.isEmpty()) {
            Text(
                "Belum ada pelanggan aktif. Tambahkan atau aktifkan dari Keuangan > Utang & Piutang.",
                color = MaterialTheme.colorScheme.error,
            )
        } else {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(activeCustomers) { customer ->
                    FilterChip(
                        selected = selectedCustomerId == customer.id,
                        onClick = { onCustomerSelected(customer.id) },
                        label = { Text(customer.name) },
                    )
                }
            }
        }
    }
}

@Composable
private fun CreditAmountCard(total: Long, amountReceived: Long) {
    Card {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Text("Pembayaran awal", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                formatRupiah(amountReceived),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Text(
                "Sisa piutang ${formatRupiah((total - amountReceived).coerceAtLeast(0))}",
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun QuickCreditAmounts(
    total: Long,
    amountReceived: Long,
    onAmount: (Long) -> Unit,
) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(listOf(0L, total / 2, total).distinct()) { amount ->
            FilterChip(
                selected = amountReceived == amount,
                onClick = { onAmount(amount) },
                label = {
                    Text(if (amount == 0L) "Belum bayar" else formatRupiah(amount))
                },
            )
        }
    }
}

@Composable
private fun TotalPaymentCard(total: Long) {
    Card(
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
                text = "Total Belanja",
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Text(
                text = formatRupiah(total),
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontFeatureSettings = "tnum",
                ),
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
    }
}

@Composable
private fun CashAmountCard(
    total: Long,
    amountReceived: Long,
    shortage: Long,
    change: Long,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(14.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            Text(
                text = "Uang Diterima",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = formatRupiah(amountReceived),
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontFeatureSettings = "tnum",
                ),
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = when {
                    amountReceived == 0L -> "Masukkan nominal uang pembeli"
                    shortage > 0L -> "Uang Kurang ${formatRupiah(shortage)}"
                    else -> "Kembalian ${formatRupiah(change)}"
                },
                color = when {
                    amountReceived == 0L -> MaterialTheme.colorScheme.onSurfaceVariant
                    shortage > 0L -> MaterialTheme.colorScheme.error
                    else -> BrandColors.Success
                },
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun QuickCashAmounts(
    total: Long,
    amountReceived: Long,
    onAmount: (Long) -> Unit,
) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(MoneyMath.quickCashAmounts(total)) { amount ->
            FilterChip(
                selected = amountReceived == amount,
                onClick = { onAmount(amount) },
                modifier = Modifier.testTag("quick-cash-$amount"),
                label = {
                    Text(if (amount == total) "Uang Pas" else formatRupiah(amount))
                },
            )
        }
    }
}

@Composable
private fun NumericKeypad(
    onDigit: (Char) -> Unit,
    onDelete: () -> Unit,
) {
    val rows = listOf(
        listOf('1', '2', '3'),
        listOf('4', '5', '6'),
        listOf('7', '8', '9'),
    )
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        rows.forEach { digits ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                digits.forEach { digit ->
                    OutlinedButton(
                        onClick = { onDigit(digit) },
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Text(digit.toString(), style = MaterialTheme.typography.titleLarge)
                    }
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Spacer(Modifier.weight(1f))
            OutlinedButton(
                onClick = { onDigit('0') },
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
            ) {
                Text("0", style = MaterialTheme.typography.titleLarge)
            }
            OutlinedButton(
                onClick = onDelete,
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp)
                    .semantics { contentDescription = "Hapus angka terakhir" },
                shape = RoundedCornerShape(12.dp),
            ) {
                Icon(Icons.AutoMirrored.Outlined.Backspace, contentDescription = null)
            }
        }
    }
}

@Composable
private fun ExternalPaymentConfirmation(
    method: PaymentMethod,
    confirmed: Boolean,
    onConfirmed: (Boolean) -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(14.dp),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                Icons.Outlined.Payments,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(34.dp),
            )
            Text(
                text = "${method.displayName()} dicatat manual",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "Pastikan uang benar-benar sudah masuk. Aplikasi ini tidak terhubung ke bank atau payment gateway.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics {
                        contentDescription = "Konfirmasi pembayaran sudah masuk"
                    },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(checked = confirmed, onCheckedChange = onConfirmed)
                Text(
                    text = "Pembayaran sudah masuk",
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}
