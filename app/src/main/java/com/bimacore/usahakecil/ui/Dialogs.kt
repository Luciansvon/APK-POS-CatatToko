package com.bimacore.usahakecil.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Backspace
import androidx.compose.material.icons.outlined.Calculate
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.bimacore.usahakecil.domain.MoneyMath
import com.bimacore.usahakecil.data.SaleUnitOption
import com.bimacore.usahakecil.data.ToppingEntity
import com.bimacore.usahakecil.domain.CartItem
import com.bimacore.usahakecil.domain.Product
import com.bimacore.usahakecil.domain.ProductVariant

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VariantPicker(
    product: Product,
    variants: List<ProductVariant>,
    onSelect: (ProductVariant) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, bottom = 28.dp),
        ) {
            Text(
                text = "Pilih Varian",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = product.name,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(16.dp))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(variants, key = { it.id }) { variant ->
                    Card(
                        onClick = { onSelect(variant) },
                        enabled = variant.stock > 0,
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        ),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                Icons.Outlined.Inventory2,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                            )
                            Spacer(Modifier.size(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(variant.label, fontWeight = FontWeight.SemiBold)
                                Text(
                                    if (variant.stock > 0) "Stok ${variant.stock}" else "Habis",
                                    color = if (variant.stock > 0) {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    } else {
                                        MaterialTheme.colorScheme.error
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                            Text(
                                formatRupiah(variant.priceOverride ?: product.basePrice),
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnitPicker(
    product: Product,
    units: List<SaleUnitOption>,
    onSelect: (SaleUnitOption) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("Pilih satuan", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(product.name, color = MaterialTheme.colorScheme.onSurfaceVariant)
            units.forEach { unit ->
                Card(
                    onClick = { onSelect(unit) },
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    ),
                ) {
                    Row(
                        Modifier.fillMaxWidth().padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(unit.label, fontWeight = FontWeight.SemiBold)
                            Text("${unit.factorToBase} satuan dasar")
                        }
                        Text(formatRupiah(unit.salePrice), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun CulinaryCustomizationDialog(
    item: CartItem,
    availableToppings: List<ToppingEntity>,
    onDismiss: () -> Unit,
    onSave: (String, Map<Long, Int>) -> Unit,
) {
    var note by remember(item.lineId) { mutableStateOf(item.note) }
    var quantities by remember(item.lineId, availableToppings) {
        mutableStateOf(
            availableToppings.associate { topping ->
                topping.id to (
                    item.toppings.firstOrNull { it.toppingId == topping.id }?.quantityPerItem
                        ?: 0
                    )
            },
        )
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Catatan & topping") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Catatan pesanan") },
                )
                availableToppings.forEach { topping ->
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(
                            checked = (quantities[topping.id] ?: 0) > 0,
                            onCheckedChange = { checked ->
                                quantities = quantities.toMutableMap().also {
                                    it[topping.id] = if (checked) 1 else 0
                                }
                            },
                        )
                        Text(
                            "${topping.label} • ${formatRupiah(topping.price)}",
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onSave(note, quantities) }) { Text("Simpan") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Batal") }
        },
    )
}

@Composable
fun CalculatorDialog(onDismiss: () -> Unit) {
    var display by remember { mutableStateOf("0") }
    var accumulator by remember { mutableStateOf<Long?>(null) }
    var operation by remember { mutableStateOf<Char?>(null) }
    var startNewNumber by remember { mutableStateOf(true) }

    fun calculate(left: Long, right: Long, operator: Char): Long = when (operator) {
        '+' -> Math.addExact(left, right)
        '-' -> Math.subtractExact(left, right)
        '×' -> Math.multiplyExact(left, right)
        '÷' -> if (right == 0L) left else left / right
        else -> right
    }.coerceIn(-MoneyMath.MAX_MONEY, MoneyMath.MAX_MONEY)

    fun pressDigit(digit: Char) {
        val next = if (startNewNumber || display == "0") digit.toString() else display + digit
        if (next.toLongOrNull()?.let { kotlin.math.abs(it) <= MoneyMath.MAX_MONEY } == true) {
            display = next
            startNewNumber = false
        }
    }

    fun pressOperation(nextOperation: Char) {
        val current = display.toLongOrNull() ?: 0L
        accumulator = try {
            if (accumulator != null && operation != null && !startNewNumber) {
                calculate(requireNotNull(accumulator), current, requireNotNull(operation))
            } else {
                current
            }
        } catch (_: ArithmeticException) {
            MoneyMath.MAX_MONEY
        }
        display = accumulator.toString()
        operation = nextOperation
        startNewNumber = true
    }

    fun pressEquals() {
        val left = accumulator ?: return
        val operator = operation ?: return
        val right = display.toLongOrNull() ?: return
        display = try {
            calculate(left, right, operator).toString()
        } catch (_: ArithmeticException) {
            "Error"
        }
        accumulator = null
        operation = null
        startNewNumber = true
    }

    fun pressPercent() {
        val current = display.toLongOrNull() ?: return
        display = try {
            val value = if (accumulator != null) {
                Math.multiplyExact(requireNotNull(accumulator), current) / 100L
            } else {
                current / 100L
            }
            value.coerceIn(-MoneyMath.MAX_MONEY, MoneyMath.MAX_MONEY).toString()
        } catch (_: ArithmeticException) {
            "Error"
        }
        startNewNumber = true
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Outlined.Calculate,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.size(10.dp))
                    Text(
                        "Kalkulator",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Outlined.Close, contentDescription = "Tutup kalkulator")
                    }
                }
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    ),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Text(
                        text = display,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        textAlign = TextAlign.End,
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontFeatureSettings = "tnum",
                        ),
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                    )
                }
                CalculatorRow(
                    labels = listOf("C", "⌫", "%", "÷"),
                    onPress = { key ->
                        when (key) {
                            "C" -> {
                                display = "0"
                                accumulator = null
                                operation = null
                                startNewNumber = true
                            }
                            "⌫" -> {
                                display = display.dropLast(1).ifEmpty { "0" }
                                startNewNumber = false
                            }
                            "%" -> pressPercent()
                            else -> pressOperation('÷')
                        }
                    },
                )
                listOf(
                    listOf("7", "8", "9", "×"),
                    listOf("4", "5", "6", "-"),
                    listOf("1", "2", "3", "+"),
                ).forEach { row ->
                    CalculatorRow(row) { key ->
                        if (key.length == 1 && key[0].isDigit()) pressDigit(key[0])
                        else pressOperation(key[0])
                    }
                }
                CalculatorRow(listOf("0", "=")) { key ->
                    if (key == "=") pressEquals() else pressDigit('0')
                }
                Text(
                    text = "Hasil kalkulator tidak mengubah keranjang atau pembayaran.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun CalculatorRow(
    labels: List<String>,
    onPress: (String) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        labels.forEach { label ->
            val primary = label in listOf("+", "-", "×", "÷", "=")
            if (primary) {
                Button(
                    onClick = { onPress(label) },
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text(label, style = MaterialTheme.typography.titleLarge)
                }
            } else {
                OutlinedButton(
                    onClick = { onPress(label) },
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    if (label == "⌫") {
                        Icon(
                            Icons.AutoMirrored.Outlined.Backspace,
                            contentDescription = "Hapus angka",
                        )
                    } else {
                        Text(label, style = MaterialTheme.typography.titleLarge)
                    }
                }
            }
        }
    }
}

@Composable
fun AboutDialog(
    businessLabel: String,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(businessLabel, fontWeight = FontWeight.Bold) },
        text = {
            Text(
                "Kasir offline. Semua data transaksi dan stok disimpan di perangkat ini. " +
                    "QRIS dan Transfer hanya dicatat manual.",
            )
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Tutup")
            }
        },
    )
}

@Composable
fun OwnerAccessDialog(
    hasPin: Boolean?,
    ownerUnlocked: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (String) -> Unit,
    onLock: () -> Unit,
) {
    var pin by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                when {
                    ownerUnlocked -> "Mode Owner aktif"
                    hasPin == false -> "Buat PIN Owner"
                    else -> "Buka Mode Owner"
                },
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            when {
                ownerUnlocked -> {
                    Text("Operasional, keuangan, laporan, dan pengaturan sedang terbuka.")
                }
                hasPin == null -> {
                    Text("Memuat akses Owner…")
                }
                else -> {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            if (hasPin == false) {
                                "Buat 4-8 angka. PIN ini dipakai owner untuk membuka semua data pengelolaan."
                            } else {
                                "Masukkan PIN Owner untuk membuka operasional, keuangan, laporan, dan pengaturan."
                            },
                        )
                        OutlinedTextField(
                            value = pin,
                            onValueChange = { value ->
                                if (value.length <= 8 && value.all(Char::isDigit)) pin = value
                            },
                            label = { Text("PIN Owner") },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.NumberPassword,
                            ),
                            visualTransformation = PasswordVisualTransformation(),
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("owner-pin-input"),
                        )
                    }
                }
            }
        },
        confirmButton = {
            when {
                ownerUnlocked -> {
                    Button(
                        onClick = onLock,
                        modifier = Modifier.testTag("owner-lock"),
                    ) {
                        Text("Kunci Mode Owner")
                    }
                }
                hasPin != null -> {
                    Button(
                        onClick = { onSubmit(pin) },
                        enabled = pin.length in 4..8,
                        modifier = Modifier.testTag("owner-submit"),
                    ) {
                        Text(if (hasPin == false) "Buat & Buka" else "Buka")
                    }
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        },
    )
}
