package com.bimacore.usahakecil.ui

import com.bimacore.usahakecil.data.ReportSummary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class OwnerOverviewMetricsTest {
    @Test
    fun `overview maps protected report totals without using raw sale received amount`() {
        val summary = ReportSummary(
            fromInclusive = 1L,
            toInclusive = 2L,
            transactionCount = 3,
            totalSales = 125_000L,
            payments = emptyList(),
            cashIn = 80_000L,
            nonCashIn = 45_000L,
            cashOut = 10_000L,
            nonCashOut = 0L,
            expenses = 5_000L,
            netCash = 70_000L,
            outstandingPayables = 0L,
            outstandingReceivables = 0L,
        )

        val metrics = ownerOverviewMetrics(summary)

        assertEquals(125_000L, metrics?.totalSales)
        assertEquals(3, metrics?.transactionCount)
        assertEquals(80_000L, metrics?.cashIn)
    }

    @Test
    fun `overview stays empty while protected day report is unavailable`() {
        assertNull(ownerOverviewMetrics(null))
    }
}
