package com.bimacore.usahakecil.ui

import com.bimacore.usahakecil.domain.PaymentMethod
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

fun formatReceiptDate(timestamp: Long): String =
    SimpleDateFormat("dd MMM yyyy, HH:mm", IndonesianLocale).format(Date(timestamp))

fun PaymentMethod.displayName(): String = when (this) {
    PaymentMethod.CASH -> "Tunai"
    PaymentMethod.QRIS -> "QRIS"
    PaymentMethod.TRANSFER -> "Transfer"
    PaymentMethod.CREDIT -> "Piutang"
}
