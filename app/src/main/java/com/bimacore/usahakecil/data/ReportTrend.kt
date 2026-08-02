package com.bimacore.usahakecil.data

enum class ReportChartMode(
    val label: String,
) {
    CASH_FLOW("Arus kas"),
    SALES("Penjualan"),
    PRODUCT("Produk"),
}

enum class ReportChartGranularity(
    val label: String,
    val bucketCount: Int,
) {
    DAILY("Harian", 14),
    WEEKLY("Mingguan", 8),
    MONTHLY("Bulanan", 12),
    YEARLY("Tahunan", 5),
}

enum class ReportProductMeasure(
    val label: String,
) {
    SALES("Omzet"),
    QUANTITY("Terjual"),
}

data class ReportTrendPoint(
    val bucketStart: Long,
    val sales: Long = 0,
    val transactionCount: Int = 0,
    val quantity: Long = 0,
    val cashIn: Long = 0,
    val cashOut: Long = 0,
    val netCash: Long = 0,
)

data class ReportProductTrend(
    val productId: Long,
    val productName: String,
    val unitLabel: String,
    val points: List<ReportTrendPoint>,
)

data class ReportTrendReport(
    val granularity: ReportChartGranularity,
    val fromInclusive: Long,
    val toInclusive: Long,
    val points: List<ReportTrendPoint>,
    val products: List<ReportProductTrend>,
)
