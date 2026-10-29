package io.novafoundation.nova.feature_staking_impl.domain.bittensor

import io.novafoundation.nova.common.utils.isPositive
import io.novafoundation.nova.feature_wallet_api.domain.model.HistoricalCoinRate
import java.math.BigDecimal
import java.math.RoundingMode

/** Current fully priced holdings; a missing market rate is not a zero price. */
data class BittensorPortfolioLiveAnchor(val tao: BigDecimal, val currentNativePrice: BigDecimal?)

/**
 * Reported account history drawn on the server window grid: zero before the first completed point, the last value
 * carried across gaps, and an optional final point supplied by current chain holdings.
 */
class BittensorPortfolioChartCalculator {

    /** Calculation-local FX index: filter and sort once, then reuse for completed and carried points. */
    private sealed interface PreparedFiatHistory {

        object ReportedUsd : PreparedFiatHistory

        class Converted(val prices: List<HistoricalCoinRate>) : PreparedFiatHistory {

            val timestamps = prices.map { it.timestamp }
        }
    }

    fun calculate(
        history: BittensorPortfolioHistory,
        liveAnchor: BittensorPortfolioLiveAnchor?,
        now: Long = System.currentTimeMillis() / 1000
    ): BittensorPortfolioValueSeries {
        val fx = history.fiatHistory.prepare()
        val historical = gridPoints(history.account, completedPoints(history, fx), fx, now)
        val livePoint = livePoint(history, liveAnchor, historical.lastOrNull(), now)
        val points = if (livePoint == null) historical else historical + livePoint
        return BittensorPortfolioValueSeries(points, periodChange(points, history.account))
    }

    private fun completedPoints(history: BittensorPortfolioHistory, fx: PreparedFiatHistory): List<BittensorPortfolioValuePoint> {
        return history.account.points
            .filter { it.completed }
            .mapNotNull { it.toValuePoint(fx, history.account.period.fxToleranceSeconds) }
            .sortedBy { it.timestamp }
    }

    /**
     * Walks the server window with the period step. Each instant takes the newest completed point at or before it
     * that was not taken yet, carries the previous value when there is none, and is zero before the first completed
     * point. Instants at or after [now] are left to the live point.
     */
    private fun gridPoints(
        account: BittensorAccountHistory,
        completed: List<BittensorPortfolioValuePoint>,
        fx: PreparedFiatHistory,
        now: Long
    ): List<BittensorPortfolioValuePoint> {
        if (account.windowStart > account.windowEnd) return completed

        val tolerance = account.period.fxToleranceSeconds
        val points = mutableListOf<BittensorPortfolioValuePoint>()
        var nextIndex = 0
        var last: BittensorPortfolioValuePoint? = null
        var instant = account.windowStart

        while (instant <= account.windowEnd && instant < now) {
            var matched: BittensorPortfolioValuePoint? = null
            while (nextIndex < completed.size && completed[nextIndex].timestamp <= instant) {
                matched = completed[nextIndex]
                nextIndex++
            }

            val point = when {
                matched != null -> matched
                last != null -> carriedPoint(last, instant, fx, tolerance)
                else -> BittensorPortfolioValuePoint(instant, BigDecimal.ZERO, BigDecimal.ZERO)
            }
            points += point
            if (matched != null) last = matched

            instant += account.period.sampleStepSeconds
        }

        return points
    }

    /** Repeats the last known TAO total at a later instant, repriced with the FX history when it covers that instant. */
    private fun carriedPoint(
        last: BittensorPortfolioValuePoint,
        instant: Long,
        fx: PreparedFiatHistory,
        tolerance: Long
    ): BittensorPortfolioValuePoint {
        val price = when (fx) {
            PreparedFiatHistory.ReportedUsd -> null
            is PreparedFiatHistory.Converted -> historicalPrice(fx, instant, tolerance)
        }
        val fiat = if (price == null) last.fiat else last.tao * price
        return BittensorPortfolioValuePoint(instant, last.tao, fiat)
    }

    private fun BittensorPortfolioFiatHistory.prepare(): PreparedFiatHistory {
        return when (this) {
            BittensorPortfolioFiatHistory.ReportedUsd -> PreparedFiatHistory.ReportedUsd
            is BittensorPortfolioFiatHistory.Converted -> PreparedFiatHistory.Converted(
                nativePrices
                    .filter { it.rate.isPositive }
                    .sortedBy { it.timestamp }
            )
        }
    }

    private fun BittensorPortfolioHistoryPoint.toValuePoint(
        fx: PreparedFiatHistory,
        tolerance: Long
    ): BittensorPortfolioValuePoint? {
        val fiat = when (fx) {
            PreparedFiatHistory.ReportedUsd -> usd
            is PreparedFiatHistory.Converted -> {
                val price = historicalPrice(fx, timestamp, tolerance) ?: return null
                tao * price
            }
        }
        return BittensorPortfolioValuePoint(timestamp, tao, fiat)
    }

    private fun historicalPrice(fx: PreparedFiatHistory.Converted, timestamp: Long, tolerance: Long): BigDecimal? {
        val index = nearestBittensorPriceIndex(fx.timestamps, timestamp, tolerance) ?: return null
        return fx.prices[index].rate
    }

    private fun livePoint(
        history: BittensorPortfolioHistory,
        anchor: BittensorPortfolioLiveAnchor?,
        lastHistorical: BittensorPortfolioValuePoint?,
        now: Long
    ): BittensorPortfolioValuePoint? {
        if (anchor == null) return null
        if (lastHistorical != null && lastHistorical.timestamp >= now) return null

        val price = livePrice(history, anchor, now) ?: return null
        return BittensorPortfolioValuePoint(now, anchor.tao, anchor.tao * price)
    }

    private fun livePrice(history: BittensorPortfolioHistory, anchor: BittensorPortfolioLiveAnchor, now: Long): BigDecimal? {
        if (history.fiatHistory is BittensorPortfolioFiatHistory.ReportedUsd) {
            reportedUsdPrice(history.account, now)?.let { return it }
        }
        return anchor.currentNativePrice?.takeIf { it.isPositive }
    }

    private fun reportedUsdPrice(history: BittensorAccountHistory, now: Long): BigDecimal? {
        // A recent bucket timestamp cannot override the server's stale verdict.
        if (history.status.stale) return null

        val point = history.points.lastOrNull {
            it.isUsableUsdPrice(now, history.period.fxToleranceSeconds)
        } ?: return null
        return point.usd.divide(point.tao, CALCULATION_SCALE, RoundingMode.HALF_EVEN)
    }

    private fun BittensorPortfolioHistoryPoint.isUsableUsdPrice(now: Long, tolerance: Long): Boolean {
        val isRecent = now - timestamp <= tolerance
        return isRecent && tao.isPositive && usd.isPositive
    }

    private fun periodChange(points: List<BittensorPortfolioValuePoint>, history: BittensorAccountHistory): BigDecimal? {
        if (points.size < 2) return null
        val first = points.first()
        if (!first.fiat.isPositive) return null
        if (!hasWindowCoverage(first, history)) return null

        return (points.last().fiat - first.fiat).divide(first.fiat, CALCULATION_SCALE, RoundingMode.HALF_EVEN)
    }

    private fun hasWindowCoverage(first: BittensorPortfolioValuePoint, history: BittensorAccountHistory): Boolean {
        val coverageTolerance = 2 * history.period.sampleStepSeconds
        return first.timestamp - history.windowStart <= coverageTolerance
    }

    private companion object {

        const val CALCULATION_SCALE = 18
    }
}
