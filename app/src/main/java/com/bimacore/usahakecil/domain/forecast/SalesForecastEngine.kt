package com.bimacore.usahakecil.domain.forecast

import java.util.Locale
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * One daily sales observation. Duplicate dates are aggregated with overflow-safe arithmetic.
 */
data class DailySales(
    val epochDay: Long,
    val quantity: Long,
)

enum class SalesForecastModel {
    MOVING_AVERAGE,
    SIMPLE_EXPONENTIAL_SMOOTHING,
    HOLT_LINEAR,
    HOLT_WINTERS_ADDITIVE,
    CROSTON_SBA,
}

data class ForecastMetrics(
    val mae: Double,
    val rmse: Double,
    val smapePercent: Double,
    val wapePercent: Double,
    val bias: Double,
)

data class ForecastCandidate(
    val model: SalesForecastModel,
    val parameters: Map<String, Double>,
    val metrics: ForecastMetrics,
    val evaluationPoints: Int,
)

data class ForecastPoint(
    val epochDay: Long,
    val expectedQuantity: Double,
)

data class SalesForecastResult(
    val selectedCandidate: ForecastCandidate,
    val rankedCandidates: List<ForecastCandidate>,
    val forecast: List<ForecastPoint>,
    val normalizedHistoryDays: Int,
    val historyStartEpochDay: Long,
    val historyEndEpochDay: Long,
)

data class SalesForecastConfig(
    val horizonDays: Int = 7,
    val seasonLengthDays: Int = 7,
    val minTrainingDays: Int = 14,
    val minEvaluationPoints: Int = 5,
    val maxBacktestPoints: Int = 60,
    val maxHistoryDays: Int = 730,
    val movingAverageWindows: List<Int> = listOf(3, 7, 14),
    val smoothingAlphas: List<Double> = listOf(0.2, 0.4, 0.6, 0.8),
    val trendBetas: List<Double> = listOf(0.2, 0.4),
    val seasonGammas: List<Double> = listOf(0.2, 0.4),
)

/**
 * Pure Kotlin forecasting engine. It has no Android, Room, network, or UI dependency.
 *
 * Model selection uses rolling-origin one-step backtesting over a common validation range.
 * Lower MAE wins; RMSE, sMAPE, model complexity, and stable parameter ordering break ties.
 */
object SalesForecastEngine {
    fun forecast(
        history: List<DailySales>,
        config: SalesForecastConfig = SalesForecastConfig(),
    ): SalesForecastResult {
        validateConfig(config)
        val normalized = normalize(history, config.maxHistoryDays)
        val values = normalized.values

        val minimumRequired = config.minTrainingDays + config.minEvaluationPoints
        require(values.size >= minimumRequired) {
            "Minimal diperlukan $minimumRequired hari histori setelah tanggal kosong diisi nol"
        }

        val specs = buildSpecs(values, config)
        require(specs.isNotEmpty()) { "Tidak ada model forecasting yang memenuhi syarat data" }

        val commonStart = maxOf(
            config.minTrainingDays,
            specs.maxOf { it.minimumTrainingSize },
            values.size - config.maxBacktestPoints,
        )
        require(values.size - commonStart >= config.minEvaluationPoints) {
            "Titik evaluasi backtesting tidak mencukupi"
        }

        val candidates = specs.mapNotNull { spec ->
            backtest(spec, values, commonStart)
        }
        require(candidates.isNotEmpty()) {
            "Semua kandidat model gagal menghasilkan backtesting yang valid"
        }

        val ranked = candidates.sortedWith(candidateComparator())
        val selected = ranked.first()
        val selectedSpec = specs.first {
            it.model == selected.model && it.parameters == selected.parameters
        }
        val future = selectedSpec.forecast(values, config.horizonDays)
        require(future.size == config.horizonDays && future.all(Double::isFinite)) {
            "Model terpilih menghasilkan forecast yang tidak valid"
        }

        return SalesForecastResult(
            selectedCandidate = selected,
            rankedCandidates = ranked,
            forecast = future.mapIndexed { index, quantity ->
                ForecastPoint(
                    epochDay = Math.addExact(normalized.endEpochDay, index.toLong() + 1L),
                    expectedQuantity = quantity.sanitizeForecast(),
                )
            },
            normalizedHistoryDays = values.size,
            historyStartEpochDay = normalized.startEpochDay,
            historyEndEpochDay = normalized.endEpochDay,
        )
    }

    private fun validateConfig(config: SalesForecastConfig) {
        require(config.horizonDays in 1..365) { "Horizon forecast harus 1 sampai 365 hari" }
        require(config.seasonLengthDays >= 2) { "Panjang musim minimal 2 hari" }
        require(config.minTrainingDays >= 2) { "Minimal training harus sekurangnya 2 hari" }
        require(config.minEvaluationPoints >= 1) { "Minimal titik evaluasi harus positif" }
        require(config.maxBacktestPoints >= config.minEvaluationPoints) {
            "Batas backtesting tidak boleh lebih kecil dari titik evaluasi minimum"
        }
        require(config.maxHistoryDays >= config.minTrainingDays + config.minEvaluationPoints) {
            "Batas histori terlalu kecil untuk training dan backtesting"
        }
        require(config.movingAverageWindows.isNotEmpty()) { "Window moving average tidak boleh kosong" }
        require(config.movingAverageWindows.all { it >= 1 }) { "Window moving average harus positif" }
        validateRates("alpha", config.smoothingAlphas)
        validateRates("beta", config.trendBetas)
        validateRates("gamma", config.seasonGammas)
    }

    private fun validateRates(name: String, values: List<Double>) {
        require(values.isNotEmpty()) { "Daftar $name tidak boleh kosong" }
        require(values.all { it.isFinite() && it > 0.0 && it <= 1.0 }) {
            "$name harus lebih dari 0 dan maksimal 1"
        }
    }

    private fun normalize(history: List<DailySales>, maxHistoryDays: Int): NormalizedSeries {
        require(history.isNotEmpty()) { "Histori penjualan tidak boleh kosong" }
        val byDay = sortedMapOf<Long, Long>()
        history.forEach { point ->
            require(point.quantity >= 0L) { "Jumlah penjualan tidak boleh negatif" }
            byDay[point.epochDay] = Math.addExact(byDay[point.epochDay] ?: 0L, point.quantity)
        }

        val sourceStart = requireNotNull(byDay.firstKey())
        val end = requireNotNull(byDay.lastKey())
        val span = Math.addExact(Math.subtractExact(end, sourceStart), 1L)
        require(span > 0L) { "Rentang tanggal histori tidak valid" }

        val start = if (span > maxHistoryDays.toLong()) {
            Math.addExact(Math.subtractExact(end, maxHistoryDays.toLong()), 1L)
        } else {
            sourceStart
        }
        val normalizedSize = Math.addExact(Math.subtractExact(end, start), 1L)
        require(normalizedSize in 1..Int.MAX_VALUE.toLong()) { "Rentang histori terlalu besar" }

        val values = ArrayList<Double>(normalizedSize.toInt())
        var day = start
        while (day <= end) {
            values += (byDay[day] ?: 0L).toDouble()
            if (day == Long.MAX_VALUE) break
            day++
        }
        return NormalizedSeries(start, end, values)
    }

    private fun buildSpecs(
        values: List<Double>,
        config: SalesForecastConfig,
    ): List<ModelSpec> {
        val specs = mutableListOf<ModelSpec>()

        config.movingAverageWindows
            .distinct()
            .sorted()
            .filter { values.size >= it + config.minEvaluationPoints }
            .forEach { specs += MovingAverageSpec(it) }

        config.smoothingAlphas
            .distinct()
            .sorted()
            .forEach { alpha ->
                specs += SimpleExponentialSmoothingSpec(alpha)
                config.trendBetas.distinct().sorted().forEach { beta ->
                    specs += HoltLinearSpec(alpha, beta)
                }
            }

        val seasonLength = config.seasonLengthDays
        if (values.size >= seasonLength * 2 + config.minEvaluationPoints) {
            config.smoothingAlphas.distinct().sorted().forEach { alpha ->
                config.trendBetas.distinct().sorted().forEach { beta ->
                    config.seasonGammas.distinct().sorted().forEach { gamma ->
                        specs += HoltWintersAdditiveSpec(
                            seasonLength = seasonLength,
                            alpha = alpha,
                            beta = beta,
                            gamma = gamma,
                        )
                    }
                }
            }
        }

        if (values.count { it > 0.0 } >= 2) {
            config.smoothingAlphas.distinct().sorted().forEach { alpha ->
                specs += CrostonSbaSpec(alpha)
            }
        }
        return specs
    }

    private fun backtest(
        spec: ModelSpec,
        values: List<Double>,
        validationStart: Int,
    ): ForecastCandidate? {
        val actual = ArrayList<Double>(values.size - validationStart)
        val predicted = ArrayList<Double>(values.size - validationStart)

        for (index in validationStart until values.size) {
            val training = values.subList(0, index)
            if (training.size < spec.minimumTrainingSize) return null
            val prediction = runCatching { spec.forecast(training, 1).single() }.getOrNull()
                ?: return null
            if (!prediction.isFinite()) return null
            actual += values[index]
            predicted += prediction.sanitizeForecast()
        }
        if (actual.isEmpty()) return null

        return ForecastCandidate(
            model = spec.model,
            parameters = spec.parameters,
            metrics = calculateMetrics(actual, predicted),
            evaluationPoints = actual.size,
        )
    }

    private fun calculateMetrics(
        actual: List<Double>,
        predicted: List<Double>,
    ): ForecastMetrics {
        require(actual.size == predicted.size && actual.isNotEmpty())
        var absoluteError = 0.0
        var squaredError = 0.0
        var signedError = 0.0
        var smape = 0.0
        var actualMagnitude = 0.0

        actual.indices.forEach { index ->
            val error = predicted[index] - actual[index]
            val absolute = abs(error)
            absoluteError += absolute
            squaredError += error.pow(2)
            signedError += error
            actualMagnitude += abs(actual[index])
            val denominator = abs(actual[index]) + abs(predicted[index])
            smape += if (denominator == 0.0) 0.0 else 200.0 * absolute / denominator
        }

        val size = actual.size.toDouble()
        return ForecastMetrics(
            mae = absoluteError / size,
            rmse = sqrt(squaredError / size),
            smapePercent = smape / size,
            wapePercent = if (actualMagnitude == 0.0) {
                if (absoluteError == 0.0) 0.0 else 100.0
            } else {
                100.0 * absoluteError / actualMagnitude
            },
            bias = signedError / size,
        )
    }

    private fun candidateComparator(): Comparator<ForecastCandidate> =
        compareBy<ForecastCandidate>(
            { it.metrics.mae.roundForRanking() },
            { it.metrics.rmse.roundForRanking() },
            { it.metrics.smapePercent.roundForRanking() },
            { modelComplexity(it.model) },
            { stableParameterKey(it.parameters) },
        )

    private fun modelComplexity(model: SalesForecastModel): Int = when (model) {
        SalesForecastModel.MOVING_AVERAGE -> 0
        SalesForecastModel.SIMPLE_EXPONENTIAL_SMOOTHING -> 1
        SalesForecastModel.CROSTON_SBA -> 2
        SalesForecastModel.HOLT_LINEAR -> 3
        SalesForecastModel.HOLT_WINTERS_ADDITIVE -> 4
    }

    private fun stableParameterKey(parameters: Map<String, Double>): String =
        parameters.entries
            .sortedBy { it.key }
            .joinToString(separator = "|") { (key, value) ->
                "$key=${String.format(Locale.ROOT, "%.8f", value)}"
            }

    private fun Double.sanitizeForecast(): Double = when {
        !isFinite() -> 0.0
        this < 0.0 -> 0.0
        else -> this
    }

    private fun Double.roundForRanking(): Double =
        if (isFinite()) kotlin.math.round(this * RANKING_PRECISION) / RANKING_PRECISION else this

    private data class NormalizedSeries(
        val startEpochDay: Long,
        val endEpochDay: Long,
        val values: List<Double>,
    )

    private interface ModelSpec {
        val model: SalesForecastModel
        val parameters: Map<String, Double>
        val minimumTrainingSize: Int
        fun forecast(values: List<Double>, horizon: Int): List<Double>
    }

    private data class MovingAverageSpec(
        val window: Int,
    ) : ModelSpec {
        override val model = SalesForecastModel.MOVING_AVERAGE
        override val parameters = mapOf("window" to window.toDouble())
        override val minimumTrainingSize = window

        override fun forecast(values: List<Double>, horizon: Int): List<Double> {
            require(values.size >= window)
            val working = values.toMutableList()
            return List(horizon) {
                val next = working.takeLast(window).average().sanitizeForecast()
                working += next
                next
            }
        }
    }

    private data class SimpleExponentialSmoothingSpec(
        val alpha: Double,
    ) : ModelSpec {
        override val model = SalesForecastModel.SIMPLE_EXPONENTIAL_SMOOTHING
        override val parameters = mapOf("alpha" to alpha)
        override val minimumTrainingSize = 2

        override fun forecast(values: List<Double>, horizon: Int): List<Double> {
            require(values.size >= minimumTrainingSize)
            var level = values.first()
            values.drop(1).forEach { value ->
                level = alpha * value + (1.0 - alpha) * level
            }
            return List(horizon) { level.sanitizeForecast() }
        }
    }

    private data class HoltLinearSpec(
        val alpha: Double,
        val beta: Double,
    ) : ModelSpec {
        override val model = SalesForecastModel.HOLT_LINEAR
        override val parameters = mapOf("alpha" to alpha, "beta" to beta)
        override val minimumTrainingSize = 3

        override fun forecast(values: List<Double>, horizon: Int): List<Double> {
            require(values.size >= minimumTrainingSize)
            var level = values.first()
            var trend = values[1] - values[0]
            values.drop(1).forEach { value ->
                val previousLevel = level
                level = alpha * value + (1.0 - alpha) * (level + trend)
                trend = beta * (level - previousLevel) + (1.0 - beta) * trend
            }
            return List(horizon) { index ->
                (level + (index + 1) * trend).sanitizeForecast()
            }
        }
    }

    private data class HoltWintersAdditiveSpec(
        val seasonLength: Int,
        val alpha: Double,
        val beta: Double,
        val gamma: Double,
    ) : ModelSpec {
        override val model = SalesForecastModel.HOLT_WINTERS_ADDITIVE
        override val parameters = mapOf(
            "seasonLength" to seasonLength.toDouble(),
            "alpha" to alpha,
            "beta" to beta,
            "gamma" to gamma,
        )
        override val minimumTrainingSize = seasonLength * 2

        override fun forecast(values: List<Double>, horizon: Int): List<Double> {
            require(values.size >= minimumTrainingSize)
            val firstSeasonAverage = values.take(seasonLength).average()
            val secondSeasonAverage = values.drop(seasonLength).take(seasonLength).average()
            var level = firstSeasonAverage
            var trend = (secondSeasonAverage - firstSeasonAverage) / seasonLength
            val seasonal = MutableList(seasonLength) { index ->
                values[index] - firstSeasonAverage
            }

            for (index in seasonLength until values.size) {
                val seasonIndex = index % seasonLength
                val oldLevel = level
                val oldSeason = seasonal[seasonIndex]
                level = alpha * (values[index] - oldSeason) +
                    (1.0 - alpha) * (level + trend)
                trend = beta * (level - oldLevel) + (1.0 - beta) * trend
                seasonal[seasonIndex] = gamma * (values[index] - level) +
                    (1.0 - gamma) * oldSeason
            }

            return List(horizon) { index ->
                val step = index + 1
                val seasonIndex = (values.size + index) % seasonLength
                (level + step * trend + seasonal[seasonIndex]).sanitizeForecast()
            }
        }
    }

    private data class CrostonSbaSpec(
        val alpha: Double,
    ) : ModelSpec {
        override val model = SalesForecastModel.CROSTON_SBA
        override val parameters = mapOf("alpha" to alpha)
        override val minimumTrainingSize = 3

        override fun forecast(values: List<Double>, horizon: Int): List<Double> {
            require(values.size >= minimumTrainingSize)
            val firstDemandIndex = values.indexOfFirst { it > 0.0 }
            if (firstDemandIndex < 0) return List(horizon) { 0.0 }

            var demandLevel = values[firstDemandIndex]
            var intervalLevel = (firstDemandIndex + 1).toDouble()
            var intervalCounter = 1.0

            for (index in firstDemandIndex + 1 until values.size) {
                val value = values[index]
                if (value > 0.0) {
                    demandLevel += alpha * (value - demandLevel)
                    intervalLevel += alpha * (intervalCounter - intervalLevel)
                    intervalCounter = 1.0
                } else {
                    intervalCounter += 1.0
                }
            }

            val estimate = if (intervalLevel <= 0.0) {
                0.0
            } else {
                (1.0 - alpha / 2.0) * demandLevel / intervalLevel
            }.sanitizeForecast()
            return List(horizon) { estimate }
        }
    }

    private const val RANKING_PRECISION = 1_000_000_000.0
}
