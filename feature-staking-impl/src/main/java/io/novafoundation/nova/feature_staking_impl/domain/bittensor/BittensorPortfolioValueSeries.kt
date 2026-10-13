package io.novafoundation.nova.feature_staking_impl.domain.bittensor

import io.novafoundation.nova.feature_wallet_api.domain.model.HistoricalCoinRate
import java.math.BigDecimal
import kotlin.math.abs

data class BittensorPortfolioValuePoint(val timestamp: Long, val taoValue: BigDecimal, val fiatValue: BigDecimal)

/** The chart series in the selected currency; [changeCovered] is false when the history does not span the period. */
data class BittensorPortfolioValueSeries(val points: List<BittensorPortfolioValuePoint>, val changeCovered: Boolean)

/** The live chain total at the current rate; absent when the portfolio is not fully priced. */
data class BittensorPortfolioLivePoint(val timestamp: Long, val taoValue: BigDecimal, val fiatRate: BigDecimal)

object BittensorPortfolioValueSeriesCalculator {

    /**
     * Prices every completed sample with the nearest TAO fiat rate within the period tolerance (samples without a
     * rate are skipped, never interpolated), replaces the open bucket with [livePoint] when one exists, and appends
     * the live point when it is newer than the last sample.
     */
    fun calculate(
        history: BittensorPortfolioStakeHistory,
        taoFiatGrid: List<HistoricalCoinRate>,
        livePoint: BittensorPortfolioLivePoint?
    ): BittensorPortfolioValueSeries {
        val grid = taoFiatGrid.filter { it.rate.signum() > 0 }.sortedBy { it.timestamp }
        val tolerance = history.period.coverageTolerance.inWholeSeconds

        val points = history.points.mapNotNullTo(mutableListOf()) { point ->
            if (!point.completed && livePoint != null) return@mapNotNullTo null
            val rate = nearestRate(grid, point.timestamp, tolerance) ?: return@mapNotNullTo null
            BittensorPortfolioValuePoint(point.timestamp, point.taoValue, point.taoValue * rate)
        }

        if (livePoint != null && (points.lastOrNull()?.timestamp ?: Long.MIN_VALUE) < livePoint.timestamp) {
            points += BittensorPortfolioValuePoint(livePoint.timestamp, livePoint.taoValue, livePoint.taoValue * livePoint.fiatRate)
        }

        val first = points.firstOrNull()
        val covered = points.size > 1 && first != null && first.fiatValue.signum() > 0 && first.timestamp - history.windowStart <= tolerance

        return BittensorPortfolioValueSeries(points, covered)
    }

    private fun nearestRate(grid: List<HistoricalCoinRate>, timestamp: Long, tolerance: Long): BigDecimal? {
        if (grid.isEmpty()) return null
        val insertion = grid.binarySearch { it.timestamp.compareTo(timestamp) }
        if (insertion >= 0) return grid[insertion].rate

        val after = -insertion - 1
        val candidates = listOfNotNull(grid.getOrNull(after - 1), grid.getOrNull(after))
        val nearest = candidates.minByOrNull { abs(it.timestamp - timestamp) } ?: return null

        return nearest.rate.takeIf { abs(nearest.timestamp - timestamp) <= tolerance }
    }
}
