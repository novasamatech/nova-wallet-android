package io.novafoundation.nova.feature_staking_impl.domain.bittensor

import io.novafoundation.nova.common.domain.ExtendedLoadingState
import io.novafoundation.nova.common.utils.isPositive
import io.novafoundation.nova.feature_currency_api.domain.model.Currency
import io.novafoundation.nova.feature_wallet_api.domain.model.HistoricalCoinRate
import java.math.BigDecimal
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
    period: BittensorSubnetChartPeriod,
    nowSeconds: Long = System.currentTimeMillis() / 1000
): List<BittensorSubnetPricePoint> {
    val points = matchBittensorSubnetHistory(alphaHistory, nativeHistory, period.source.samplingInterval())
    val cutoff = if (period == BittensorSubnetChartPeriod.ALL) Long.MIN_VALUE else nowSeconds - period.duration.inWholeSeconds
    return points.filter { it.timestamp in cutoff..nowSeconds }
}

/**
 * Netuids are recycled: market prices quoted before the current registration belong to a previous subnet.
 * Clipping them also makes a window that starts before the registration fail [hasBittensorSubnetHistoryCoverage] (as on iOS).
 */
internal fun clipBittensorSubnetHistory(points: List<BittensorSubnetPricePoint>, registrationEpochSeconds: Long?): List<BittensorSubnetPricePoint> {
    if (registrationEpochSeconds == null) return points
    return points.filter { it.timestamp >= registrationEpochSeconds }
}

internal fun bittensorRegistrationEpochSeconds(ageInDays: BigDecimal, nowSeconds: Long): Long =
    nowSeconds - (ageInDays * SECONDS_PER_DAY.toBigDecimal()).setScale(0, RoundingMode.HALF_UP).toLong()

private const val SECONDS_PER_DAY = 86_400L

/** A successful short/old response is not a return for the entire selected window. */
internal fun hasBittensorSubnetHistoryCoverage(
    points: List<BittensorSubnetPricePoint>,
    period: BittensorSubnetChartPeriod,
    nowSeconds: Long = System.currentTimeMillis() / 1000
): Boolean {
    if (points.size < 2) return false
    val first = points.first()
    val last = points.last()
    val tolerance = 2 * period.source.samplingInterval().inWholeSeconds
    if (first.timestamp > last.timestamp || last.timestamp > nowSeconds || nowSeconds - last.timestamp > tolerance) return false
    if (period == BittensorSubnetChartPeriod.ALL) return true
    val start = nowSeconds - period.duration.inWholeSeconds
    return first.timestamp in (start - tolerance)..(start + tolerance)
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
