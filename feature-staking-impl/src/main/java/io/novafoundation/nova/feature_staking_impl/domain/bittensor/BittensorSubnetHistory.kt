package io.novafoundation.nova.feature_staking_impl.domain.bittensor

import io.novafoundation.nova.common.domain.ExtendedLoadingState
import io.novafoundation.nova.common.utils.isPositive
import io.novafoundation.nova.feature_currency_api.domain.model.Currency
import io.novafoundation.nova.feature_wallet_api.domain.model.HistoricalCoinRate
import java.math.RoundingMode
import kotlin.math.abs
import kotlin.time.Duration.Companion.days

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
    val alphaPoints = alphaHistory.filter { it.rate.isPositive }.sortedBy { it.timestamp }
    val nativePoints = nativeHistory.filter { it.rate.isPositive }.sortedBy { it.timestamp }
    if (alphaPoints.isEmpty() || nativePoints.isEmpty()) return emptyList()
    val cutoff = alphaPoints.last().timestamp - period.duration.inWholeSeconds
    val maximumPriceDistance = 1.days.inWholeSeconds
    var nativeIndex = 0

    return alphaPoints.filter { it.timestamp > cutoff }.mapNotNull { alphaPoint ->
        while (nativeIndex + 1 < nativePoints.size && nativePoints[nativeIndex + 1].timestamp <= alphaPoint.timestamp) {
            nativeIndex++
        }
        val nativePoint = nativePoints[nativeIndex]
        if (abs(nativePoint.timestamp - alphaPoint.timestamp) > maximumPriceDistance) return@mapNotNull null
        BittensorSubnetPricePoint(
            timestamp = alphaPoint.timestamp,
            fiat = alphaPoint.rate,
            tao = alphaPoint.rate.divide(nativePoint.rate, 12, RoundingMode.HALF_EVEN)
        )
    }
}
