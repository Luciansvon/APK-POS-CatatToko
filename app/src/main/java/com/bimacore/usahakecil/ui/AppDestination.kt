package com.bimacore.usahakecil.ui

import com.bimacore.usahakecil.domain.BusinessCapabilities
import com.bimacore.usahakecil.domain.BusinessType

enum class AppDestination(
    val label: String,
) {
    POS("Kasir"),
    OPERATIONS("Operasional"),
    FINANCE("Keuangan"),
    REPORTS("Laporan"),
    MORE("Lainnya"),
}

data class NavigationPresentation(
    val operationsLabel: String,
    val operationsStartSection: String,
    val financeLabel: String,
    val financeStartTab: Int,
)

fun navigationPresentationFor(type: BusinessType): NavigationPresentation = when (type) {
    BusinessType.RETAIL -> NavigationPresentation(
        operationsLabel = "Operasional",
        operationsStartSection = "Produk",
        financeLabel = "Keuangan",
        financeStartTab = 1,
    )
    BusinessType.WHOLESALE -> NavigationPresentation(
        operationsLabel = "Grosir",
        operationsStartSection = "Grosir",
        financeLabel = "Keuangan",
        financeStartTab = 0,
    )
    BusinessType.CULINARY -> NavigationPresentation(
        operationsLabel = "Pesanan",
        operationsStartSection = "Kuliner",
        financeLabel = "Keuangan",
        financeStartTab = 0,
    )
}

fun availableDestinations(capabilities: BusinessCapabilities): List<AppDestination> = buildList {
    add(AppDestination.POS)
    if (capabilities.inventory || capabilities.purchasing || capabilities.workforce) {
        add(AppDestination.OPERATIONS)
    }
    if (
        capabilities.cashLedger ||
        capabilities.supplierPayables ||
        capabilities.customerReceivables
    ) {
        add(AppDestination.FINANCE)
    }
    if (capabilities.reports) {
        add(AppDestination.REPORTS)
    }
    add(AppDestination.MORE)
}

fun destinationsForAccess(
    capabilities: BusinessCapabilities,
    ownerUnlocked: Boolean,
): List<AppDestination> = if (ownerUnlocked) {
    availableDestinations(capabilities)
} else {
    listOf(AppDestination.POS)
}
