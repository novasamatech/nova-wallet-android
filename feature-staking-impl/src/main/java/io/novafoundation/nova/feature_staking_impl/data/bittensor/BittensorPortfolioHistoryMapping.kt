package io.novafoundation.nova.feature_staking_impl.data.bittensor

import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorAccountHistory
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorPortfolioHistoryPoint
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorPortfolioHistoryStatus
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorPortfolioPeriod
import java.math.BigDecimal

// Bittensor portfolio-history and shared reported-decimal wire limits.
private const val MAX_PORTFOLIO_HISTORY_POINTS = 512
private const val MAX_REPORTED_DECIMAL_LENGTH = 1024

private val PORTFOLIO_DECIMAL = Regex("-?(?:0|[1-9][0-9]*)(?:\\.[0-9]+)?")

private data class PortfolioHistoryWindow(val start: BigDecimal, val end: BigDecimal)

internal fun mapBittensorPortfolioHistory(remote: BittensorPortfolioHistoryRemote, period: BittensorPortfolioPeriod): BittensorAccountHistory {
    require(remote.period == period.name) { "Portfolio history period mismatch" }
    require(remote.historyScope == "TAO_APP_PARTIAL") { "Unsupported portfolio history scope" }

    val window = mapPortfolioHistoryWindow(remote.window)
    val status = mapPortfolioHistoryStatus(remote.meta)
    val points = mapPortfolioHistoryPoints(remote.points, window)
    return BittensorAccountHistory(
        period = period,
        windowStart = window.start.toLong(),
        windowEnd = window.end.toLong(),
        points = points,
        status = status
    )
}

private fun mapPortfolioHistoryWindow(remote: BittensorPortfolioHistoryRemote.Window?): PortfolioHistoryWindow {
    val window = requireNotNull(remote)
    val start = parseBittensorTimestamp(requireNotNull(window.start))
    val end = parseBittensorTimestamp(requireNotNull(window.end))
    require(start <= end) { "Invalid portfolio history window" }
    return PortfolioHistoryWindow(start, end)
}

private fun mapPortfolioHistoryStatus(remote: BittensorPortfolioHistoryRemote.Meta?): BittensorPortfolioHistoryStatus {
    val metadata = requireNotNull(remote)
    require(metadata.completeness == "COMPLETE")
    val component = requireNotNull(metadata.components?.portfolioHistory)
    require(component.availability == "AVAILABLE")
    require(component.valueQuality == "DERIVED")
    require(component.sourceClass == "PRIMARY")

    val stale = when (component.freshness) {
        "FRESH" -> false
        "STALE" -> true
        else -> error("Unknown portfolio history freshness")
    }
    val asOf = parseBittensorTimestamp(requireNotNull(component.asOf))
    return BittensorPortfolioHistoryStatus(asOf, stale)
}

private fun mapPortfolioHistoryPoints(
    remote: List<BittensorPortfolioHistoryRemote.Point>?,
    window: PortfolioHistoryWindow
): List<BittensorPortfolioHistoryPoint> {
    val points = requireNotNull(remote)
    require(points.size <= MAX_PORTFOLIO_HISTORY_POINTS) { "Too many portfolio history points" }
    var previousTimestamp: BigDecimal? = null
    return points.map { point ->
        val timestamp = parseBittensorTimestamp(requireNotNull(point.timestamp))
        require(timestamp >= window.start && timestamp <= window.end) { "Portfolio history point outside window" }
        require(previousTimestamp?.let { timestamp > it } != false) { "Portfolio history points not strictly ascending" }
        previousTimestamp = timestamp
        mapPortfolioHistoryPoint(point, timestamp)
    }
}

private fun mapPortfolioHistoryPoint(point: BittensorPortfolioHistoryRemote.Point, timestamp: BigDecimal): BittensorPortfolioHistoryPoint {
    return BittensorPortfolioHistoryPoint(
        timestamp = timestamp.toLong(),
        tao = mapPortfolioDecimal(point.reportedValueTao),
        usd = mapPortfolioDecimal(point.reportedValueUsd),
        completed = requireNotNull(point.completed)
    )
}

private fun mapPortfolioDecimal(value: String?): BigDecimal {
    requireNotNull(value) { "Missing reported portfolio decimal" }
    require(value.length in 1..MAX_REPORTED_DECIMAL_LENGTH) { "Invalid reported portfolio decimal length" }
    require(PORTFOLIO_DECIMAL.matches(value)) { "Invalid reported portfolio decimal" }
    return value.toBigDecimal()
}
