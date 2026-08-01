package com.bimacore.usahakecil.domain.forecast

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class SalesForecastEngineTest {
    @Test
    fun `normalizes duplicate dates and missing days`() {
        val result = SalesForecastEngine.forecast(
            history = listOf(
                DailySales(epochDay = 1, quantity = 2),
                DailySales(epochDay = 1, quantity = 3),
                DailySales(epochDay = 3, quantity = 4),
                DailySales(epochDay = 5, quantity = 6),
                DailySales(epochDay = 7, quantity = 8),
                DailySales(epochDay = 9, quantity = 10),
                DailySales(epochDay = 11, quantity = 12),
            ),
            config = compactConfig(horizonDays = 3),
        )

        assertEquals(11, result.normalizedHistoryDays)
        assertEquals(1, result.historyStartEpochDay)
        assertEquals(11, result.historyEndEpochDay)
        assertEquals(listOf(12L, 13L, 14L), result.forecast.map { it.epochDay })
    }

    @Test
    fun `stable demand produces stable non-negative forecast`() {
        val result = SalesForecastEngine.forecast(
            history = (1L..35L).map { DailySales(it, 10) },
            config = SalesForecastConfig(horizonDays = 7),
        )

        assertTrue(result.forecast.all { it.expectedQuantity >= 0.0 })
        assertTrue(result.forecast.all { kotlin.math.abs(it.expectedQuantity - 10.0) < 0.000001 })
        assertEquals(0.0, result.selectedCandidate.metrics.mae, 0.000001)
    }

    @Test
    fun `trend model can continue increasing demand`() {
        val result = SalesForecastEngine.forecast(
            history = (1L..40L).map { day -> DailySales(day, day) },
            config = SalesForecastConfig(
                horizonDays = 3,
                seasonLengthDays = 7,
                minTrainingDays = 14,
            ),
        )

        assertTrue(result.forecast.first().expectedQuantity > 40.0)
        assertTrue(result.forecast.zipWithNext().all { (left, right) ->
            right.expectedQuantity >= left.expectedQuantity
        })
        assertTrue(result.rankedCandidates.any { it.model == SalesForecastModel.HOLT_LINEAR })
    }

    @Test
    fun `intermittent demand includes Croston candidate with positive estimate`() {
        val history = (1L..42L).map { day ->
            DailySales(day, if (day % 7L == 0L) 7 else 0)
        }
        val result = SalesForecastEngine.forecast(
            history = history,
            config = SalesForecastConfig(horizonDays = 5),
        )

        val croston = result.rankedCandidates.filter {
            it.model == SalesForecastModel.CROSTON_SBA
        }
        assertTrue(croston.isNotEmpty())
        assertTrue(result.forecast.all { it.expectedQuantity >= 0.0 })
    }

    @Test
    fun `weekly seasonality includes Holt Winters candidates`() {
        val weekly = listOf(5L, 5L, 5L, 5L, 5L, 15L, 25L)
        val history = (0 until 8).flatMap { week ->
            weekly.mapIndexed { index, quantity ->
                DailySales(epochDay = (week * 7 + index + 1).toLong(), quantity = quantity)
            }
        }
        val result = SalesForecastEngine.forecast(
            history = history,
            config = SalesForecastConfig(horizonDays = 7, seasonLengthDays = 7),
        )

        assertTrue(result.rankedCandidates.any {
            it.model == SalesForecastModel.HOLT_WINTERS_ADDITIVE
        })
        assertTrue(result.forecast.all { it.expectedQuantity.isFinite() && it.expectedQuantity >= 0.0 })
    }

    @Test
    fun `all-zero demand remains zero and metrics are finite`() {
        val result = SalesForecastEngine.forecast(
            history = (1L..30L).map { DailySales(it, 0) },
            config = SalesForecastConfig(horizonDays = 4),
        )

        assertTrue(result.forecast.all { it.expectedQuantity == 0.0 })
        assertTrue(result.rankedCandidates.all {
            it.metrics.mae.isFinite() &&
                it.metrics.rmse.isFinite() &&
                it.metrics.smapePercent.isFinite() &&
                it.metrics.wapePercent.isFinite()
        })
    }

    @Test
    fun `rejects negative demand`() {
        try {
            SalesForecastEngine.forecast(
                history = (1L..30L).map { day ->
                    DailySales(day, if (day == 10L) -1 else 1)
                },
            )
            fail("Negative demand should be rejected")
        } catch (expected: IllegalArgumentException) {
            assertTrue(expected.message.orEmpty().contains("negatif"))
        }
    }

    @Test
    fun `rejects insufficient history`() {
        try {
            SalesForecastEngine.forecast(
                history = (1L..10L).map { DailySales(it, 1) },
            )
            fail("Short history should be rejected")
        } catch (expected: IllegalArgumentException) {
            assertTrue(expected.message.orEmpty().contains("Minimal diperlukan"))
        }
    }

    private fun compactConfig(horizonDays: Int) = SalesForecastConfig(
        horizonDays = horizonDays,
        seasonLengthDays = 3,
        minTrainingDays = 5,
        minEvaluationPoints = 3,
        maxBacktestPoints = 5,
        maxHistoryDays = 30,
        movingAverageWindows = listOf(2, 3),
        smoothingAlphas = listOf(0.3, 0.6),
        trendBetas = listOf(0.3),
        seasonGammas = listOf(0.3),
    )
}
