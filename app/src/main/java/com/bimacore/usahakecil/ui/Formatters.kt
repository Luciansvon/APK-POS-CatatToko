package com.bimacore.usahakecil.ui

import com.bimacore.usahakecil.domain.PaymentMethod
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val IndonesianLocale = Locale.forLanguageTag("id-ID")

fun formatRupiah(value: Long): String {
    val formatter = NumberFormat.getNumberInstance(IndonesianLocale).apply {
        maximumFractionDigits = 0
        minimumFractionDigits = 0
    }
    return "Rp${formatter.format(value)}"
}

fun formatCompactRupiah(value: Long): String {
    val magnitude = when (value) {
        Long.MIN_VALUE -> Long.MAX_VALUE
        else -> kotlin.math.abs(value)
    }
    val (divisor, suffix) = when {
        magnitude >= 1_000_000_000L -> 1_000_000_000L to "M"
        magnitude >= 1_000_000L -> 1_000_000L to "jt"
        magnitude >= 1_000L -> 1_000L to "rb"
        else -> return formatRupiah(value)
    }
    val formatter = DecimalFormat("0.#", DecimalFormatSymbols(IndonesianLocale)).apply {
        roundingMode = RoundingMode.DOWN
        isGroupingUsed = false
    }
    return "Rp${formatter.format(value.toDouble() / divisor)} $suffix"
}

fun formatReceiptDate(timestamp: Long): String =
    SimpleDateFormat("dd MMM yyyy, HH:mm", IndonesianLocale).format(Date(timestamp))

fun PaymentMethod.displayName(): String = when (this) {
    PaymentMethod.CASH -> "Tunai"
    PaymentMethod.QRIS -> "QRIS"
    PaymentMethod.TRANSFER -> "Transfer"
    PaymentMethod.CREDIT -> "Piutang"
}

fun formatAttendanceStatus(status: String): String = when (status) {
    "PRESENT" -> "Hadir"
    "HALF_DAY" -> "Setengah Hari"
    "PERMIT" -> "Izin"
    "ABSENT" -> "Tidak Hadir"
    else -> status
}

fun formatWorkerScheme(scheme: String): String = when (scheme) {
    "DAILY" -> "Pekerja Harian"
    "FREELANCE" -> "Pekerja Panggilan"
    else -> scheme
}

fun formatOrderStatus(status: String): String = when (status) {
    "NEW" -> "Pesanan Baru"
    "PROCESSING" -> "Diproses"
    "READY" -> "Siap"
    "COMPLETED" -> "Selesai"
    "CANCELLED" -> "Dibatalkan"
    else -> status
}

fun formatStockMovementType(type: String): String = when (type) {
    "SALE_OUT" -> "Penjualan"
    "PURCHASE_IN" -> "Pembelian"
    "ADJUSTMENT_IN" -> "Penyesuaian (Tambah)"
    "ADJUSTMENT_OUT" -> "Penyesuaian (Kurang)"
    "DAMAGE_OUT" -> "Barang Rusak"
    "LOSS_OUT" -> "Barang Hilang"
    "RETURN_IN" -> "Retur Masuk"
    "RETURN_OUT" -> "Retur Keluar"
    else -> type
}

fun formatCashEntryType(type: String): String = when (type) {
    "SALE_IN" -> "Penjualan"
    "PURCHASE_OUT" -> "Pembelian"
    "CASH_IN" -> "Kas Masuk"
    "CASH_OUT" -> "Kas Keluar"
    "EXPENSE" -> "Pengeluaran Operasional"
    "PAYABLE_OUT" -> "Pembayaran Utang"
    "RECEIVABLE_IN" -> "Penerimaan Piutang"
    "WAGE_OUT" -> "Pembayaran Upah"
    else -> type
}

