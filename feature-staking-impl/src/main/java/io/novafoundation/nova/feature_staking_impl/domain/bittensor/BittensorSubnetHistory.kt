package io.novafoundation.nova.feature_staking_impl.domain.bittensor

import io.novafoundation.nova.common.domain.ExtendedLoadingState
import io.novafoundation.nova.common.utils.isPositive
import io.novafoundation.nova.feature_currency_api.domain.model.Currency
import io.novafoundation.nova.feature_wallet_api.domain.model.HistoricalCoinRate
import java.math.RoundingMode
import kotlin.time.Duration

data class BittensorSubnetChartRequest(val nativePriceId: String?, val currency: Currency, val period: BittensorSubnetChartPeriod)

data class BittensorSubnetChart(
    val period: BittensorSubnetChartPeriod,
    val points: ExtendedLoadingState<List<BittensorSubnetPricePoint>>
)

internal fun convertBittensorSubnetHistory(
    alphaHistory: List<HistoricalCoinRate>,
    nativeHistory: List<HistoricalCoinRate>,
    period: BittensorSubnetChartPeriod
): List<BittensorSubnetPricePoint> {
    val points = matchBittensorSubnetHistory(alphaHistory, nativeHistory, period.source.samplingInterval())
    val cutoff = (points.lastOrNull()?.timestamp ?: return emptyList()) - period.duration.inWholeSeconds
    return points.filter { it.timestamp >= cutoff }
}

internal fun matchBittensorSubnetHistory(
    alphaHistory: List<HistoricalCoinRate>,
    nativeHistory: List<HistoricalCoinRate>,
    samplingInterval: Duration
): List<BittensorSubnetPricePoint> {
    val alphaPoints = alphaHistory.filter { it.rate.isPositive }.sortedBy { it.timestamp }
    val nativePoints = nativeHistory.filter { it.rate.isPositive }.sortedBy { it.timestamp }
    if (alphaPoints.isEmpty() || nativePoints.isEmpty()) return emptyList()
    val nativeTimestamps = nativePoints.map { it.timestamp }

    return alphaPoints.mapNotNull { alphaPoint ->
        val nativeIndex = nearestBittensorPriceIndex(nativeTimestamps, alphaPoint.timestamp, samplingInterval.inWholeSeconds)
            ?: return@mapNotNull null
        val nativePoint = nativePoints[nativeIndex]
        BittensorSubnetPricePoint(
            timestamp = alphaPoint.timestamp,
            fiat = alphaPoint.rate,
            tao = alphaPoint.rate.divide(nativePoint.rate, 12, RoundingMode.HALF_EVEN)
        )
    }
}
