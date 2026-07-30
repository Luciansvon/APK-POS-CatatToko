package com.bimacore.usahakecil.domain

data class BusinessCapabilities(
    val inventory: Boolean = true,
    val purchasing: Boolean = true,
    val cashLedger: Boolean = true,
    val supplierPayables: Boolean = true,
    val customerReceivables: Boolean,
    val reports: Boolean = true,
    val workforce: Boolean = true,
    val backupRestore: Boolean = true,
    val multiUnit: Boolean,
    val tierPricing: Boolean,
    val culinaryOrders: Boolean,
    val recipes: Boolean,
) {
    companion object {
        fun forType(type: BusinessType): BusinessCapabilities = when (type) {
            BusinessType.RETAIL -> BusinessCapabilities(
                customerReceivables = true,
                multiUnit = false,
                tierPricing = false,
                culinaryOrders = false,
                recipes = false,
            )
            BusinessType.WHOLESALE -> BusinessCapabilities(
                customerReceivables = true,
                multiUnit = true,
                tierPricing = true,
                culinaryOrders = false,
                recipes = false,
            )
            BusinessType.CULINARY -> BusinessCapabilities(
                customerReceivables = false,
                multiUnit = false,
                tierPricing = false,
                culinaryOrders = true,
                recipes = true,
            )
        }
    }
}
