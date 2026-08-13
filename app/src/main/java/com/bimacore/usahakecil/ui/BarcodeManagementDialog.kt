package com.bimacore.usahakecil.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.bimacore.usahakecil.data.ProductBarcodeEntity
import com.bimacore.usahakecil.data.ProductEntity
import com.bimacore.usahakecil.data.ProductVariantEntity
import com.bimacore.usahakecil.data.UnitConversionEntity

@Composable
fun BarcodeManagementSection(
    products: List<ProductEntity>,
    variants: List<ProductVariantEntity>,
    units: List<UnitConversionEntity>,
    mappings: List<ProductBarcodeEntity>,
    multiUnit: Boolean,
    prefill: String?,
    saveVersion: Long,
    onPrefillConsumed: () -> Unit,
    onSave: (Long?, String, Long, Long?, Long?) -> Unit,
    onSetActive: (Long, Boolean) -> Unit,
) {
    val activeProducts = products.filter(ProductEntity::isActive)
    var editingId by rememberSaveable { mutableStateOf<Long?>(null) }
    var barcode by rememberSaveable { mutableStateOf("") }
    var productIndex by rememberSaveable { mutableIntStateOf(0) }
    var variantIndex by rememberSaveable { mutableIntStateOf(0) }
    var unitIndex by rememberSaveable { mutableIntStateOf(0) }
    var showScanner by rememberSaveable { mutableStateOf(false) }
    var observedSaveVersion by remember { mutableStateOf(saveVersion) }

    LaunchedEffect(prefill) {
        val value = prefill ?: return@LaunchedEffect
        barcode = value
        editingId = null
        onPrefillConsumed()
    }
    LaunchedEffect(saveVersion) {
        if (saveVersion != observedSaveVersion) {
            observedSaveVersion = saveVersion
            editingId = null
            barcode = ""
            variantIndex = 0
            unitIndex = 0
        }
    }

    val selectedProduct = activeProducts.getOrNull(productIndex.coerceIn(0, activeProducts.lastIndex.coerceAtLeast(0)))
    val productVariants = selectedProduct?.let { product ->
        variants.filter { it.productId == product.id && it.isActive }
    }.orEmpty()
    val productUnits = selectedProduct?.let { product ->
        units.filter { it.productId == product.id && it.isActive }
    }.orEmpty()

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OwnerHeroCard(
            eyebrow = "Barcode offline",
            value = "${mappings.count(ProductBarcodeEntity::isActive)} aktif",
            supportingText = "Scan langsung masuk ke produk, varian, dan satuan yang dipilih.",
        )
        SectionTitle(if (editingId == null) "Daftarkan barcode" else "Ubah barcode")
        OutlinedTextField(
            value = barcode,
            onValueChange = { barcode = it },
            label = { Text("Nomor / kode barcode") },
            supportingText = { Text("Nol depan dan huruf besar-kecil tetap dipertahankan.") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("barcode-input"),
        )
        OutlinedButton(
            onClick = { showScanner = true },
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
        ) {
            Icon(Icons.Outlined.QrCodeScanner, contentDescription = null)
            Text(" Scan untuk isi kode")
        }
        CycleSelectionButton(
            label = "Produk",
            value = selectedProduct?.name ?: "Tambahkan produk dulu",
            enabled = activeProducts.isNotEmpty(),
        ) {
            productIndex = nextSelectionIndex(productIndex, activeProducts.size)
            variantIndex = 0
            unitIndex = 0
        }
        if (productVariants.isNotEmpty()) {
            CycleSelectionButton(
                label = "Varian",
                value = if (variantIndex == 0) "Pilih saat transaksi" else productVariants[variantIndex - 1].label,
            ) {
                variantIndex = nextSelectionIndex(variantIndex, productVariants.size + 1)
            }
        }
        if (multiUnit && productUnits.isNotEmpty()) {
            CycleSelectionButton(
                label = "Satuan",
                value = if (unitIndex == 0) {
                    "${selectedProduct?.unitLabel ?: "pcs"} (satuan dasar)"
                } else {
                    productUnits[unitIndex - 1].label
                },
            ) {
                unitIndex = nextSelectionIndex(unitIndex, productUnits.size + 1)
            }
        }
        Button(
            onClick = {
                val product = selectedProduct ?: return@Button
                onSave(
                    editingId,
                    barcode,
                    product.id,
                    productVariants.getOrNull(variantIndex - 1)?.id,
                    productUnits.getOrNull(unitIndex - 1)?.id,
                )
            },
            enabled = barcode.isNotBlank() && selectedProduct != null,
            modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp).testTag("save-barcode"),
        ) { Text(if (editingId == null) "Simpan Barcode" else "Simpan Perubahan") }
        if (editingId != null) {
            TextButton(
                onClick = {
                    editingId = null
                    barcode = ""
                    variantIndex = 0
                    unitIndex = 0
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Batal ubah") }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f))
        SectionTitle("Barcode terdaftar")
        if (mappings.isEmpty()) {
            OwnerEmptyState(
                title = "Belum ada barcode",
                message = "Daftarkan dari angka kemasan atau pakai kamera.",
                testTag = "barcode-empty-state",
            )
        } else {
            mappings.forEach { mapping ->
                val product = products.firstOrNull { it.id == mapping.productId }
                val variant = variants.firstOrNull { it.id == mapping.variantId }
                val unit = units.firstOrNull { it.id == mapping.unitId }
                Column(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(mapping.barcode, fontWeight = FontWeight.Bold)
                    Text(
                        listOfNotNull(product?.name, variant?.label, unit?.label).joinToString(" · "),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(
                            onClick = {
                                val selectedProductIndex = activeProducts.indexOfFirst { it.id == mapping.productId }
                                if (selectedProductIndex >= 0) productIndex = selectedProductIndex
                                val selectedVariants = variants.filter {
                                    it.productId == mapping.productId && it.isActive
                                }
                                variantIndex = selectedVariants.indexOfFirst { it.id == mapping.variantId }
                                    .let { if (it < 0) 0 else it + 1 }
                                val selectedUnits = units.filter {
                                    it.productId == mapping.productId && it.isActive
                                }
                                unitIndex = selectedUnits.indexOfFirst { it.id == mapping.unitId }
                                    .let { if (it < 0) 0 else it + 1 }
                                barcode = mapping.barcode
                                editingId = mapping.id
                            },
                            enabled = product?.isActive == true,
                        ) { Text("Ubah") }
                        TextButton(onClick = { onSetActive(mapping.id, !mapping.isActive) }) {
                            Text(if (mapping.isActive) "Nonaktifkan" else "Aktifkan")
                        }
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
            }
        }
    }

    if (showScanner) {
        Dialog(
            onDismissRequest = { showScanner = false },
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false,
            ),
        ) {
            BarcodeScannerScreen(
                feedback = null,
                onBarcode = {
                    barcode = it
                    showScanner = false
                },
                onMultipleBarcodes = {},
                onBack = { showScanner = false },
                singleScan = true,
            )
        }
    }
}

@Composable
private fun CycleSelectionButton(
    label: String,
    value: String,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
    ) {
        Text("$label: $value")
    }
}

private fun nextSelectionIndex(current: Int, size: Int): Int =
    if (size <= 0) 0 else (current + 1) % size
