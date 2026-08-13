package com.bimacore.usahakecil.domain

private const val MAX_BARCODE_LENGTH = 128

fun normalizeBarcode(raw: String): String {
    val value = raw.trim()
    require(value.isNotEmpty()) { "Barcode wajib diisi" }
    require(value.length <= MAX_BARCODE_LENGTH) { "Barcode terlalu panjang" }
    require(value.none(Char::isISOControl)) { "Barcode berisi karakter yang tidak didukung" }
    return value
}

data class BarcodeTarget(
    val barcode: String,
    val product: Product,
    val variantId: Long?,
    val unitId: Long?,
)

sealed interface BarcodeLookupResult {
    data class Found(val target: BarcodeTarget) : BarcodeLookupResult
    data object NotFound : BarcodeLookupResult
    data object Inactive : BarcodeLookupResult
    data object Unsupported : BarcodeLookupResult
}
