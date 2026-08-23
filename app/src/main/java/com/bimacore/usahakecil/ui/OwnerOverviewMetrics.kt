package com.bimacore.usahakecil.ui

import com.bimacore.usahakecil.data.ReportSummary

/** Values intentionally sourced from the protected Owner day report. */
internal data class OwnerOverviewMetrics(
    val totalSales: Long,
    val transactionCount: Int,
    val cashIn: Long,
)

internal fun ownerOverviewMetrics(summary: ReportSummary?): OwnerOverviewMetrics? =
    summary?.let {
        OwnerOverviewMetrics(
            totalSales = it.totalSales,
            transactionCount = it.transactionCount,
            cashIn = it.cashIn,
        )
    }
