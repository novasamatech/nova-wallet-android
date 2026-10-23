package io.novafoundation.nova.feature_staking_impl.domain.bittensor

import io.novafoundation.nova.common.utils.formatting.parseDateISO_8601
import io.novafoundation.nova.common.utils.formatting.parseDateISO_8601_NoMs
import io.novafoundation.nova.common.utils.isPositive
import io.novafoundation.nova.common.utils.mapAsync
import io.novafoundation.nova.feature_currency_api.domain.interfaces.CurrencyRepository
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorRootValidatorSource
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorSubnetMarketRemote
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorSubnetMarketRepository
import io.novafoundation.nova.feature_wallet_api.data.repository.CoinPriceRepository
import io.novafoundation.nova.feature_wallet_api.data.repository.PricePeriod
import io.novafoundation.nova.feature_wallet_api.domain.model.HistoricalCoinRate
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.time.Duration.Companion.days
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withTimeout

class BittensorPortfolioRowInteractor(
    private val rootValidators: BittensorRootValidatorSource,
    private val markets: BittensorSubnetMarketRepository,
    private val prices: CoinPriceRepository,
    private val currencies: CurrencyRepository
) {

    suspend fun rootApy(chain: Chain, positions: List<BittensorRootPosition>): BigDecimal? {
        val rates = positions.mapAsync { position ->
            val validator = BittensorValidator(position.validatorHotkey, "")
            rootValidators.annualPercentageYield(chain, validator)
        }
        return weightedRootApy(positions, rates)
    }

    suspend fun weeklyChanges(nativePriceId: String, subnetIds: Set<BittensorNetuid.Subnet>): Map<BittensorNetuid.Subnet, BigDecimal> {
        return weeklySummaries(nativePriceId, subnetIds).mapNotNull { (subnet, summary) -> summary?.let { subnet to it.changePercent } }.toMap()
    }

    /** Every listed market of [subnetIds]; a null summary is listed without a computable change, as iOS `.unavailable`. */
    suspend fun weeklySummaries(nativePriceId: String, subnetIds: Set<BittensorNetuid.Subnet>): Map<BittensorNetuid.Subnet, BittensorWeeklyPriceSummary?> {
        if (subnetIds.isEmpty()) return emptyMap()
        return withTimeout(25_000L) {
            val usd = requireNotNull(currencies.getCurrency("usd")) { "USD currency unavailable" }
            coroutineScope {
                val marketRows = async { markets.weeklyMarkets() }
                val nativeHistory = prices.getLastHistoryForPeriod(nativePriceId, usd, PricePeriod.WEEK)
                val now = System.currentTimeMillis()
                marketRows.await().filterKeys { it in subnetIds }.mapValues { (_, market) ->
                    weeklyChangeInTao(market, nativeHistory, now)?.let { change ->
                        BittensorWeeklyPriceSummary(change, weeklySparklineInTao(market.weeklySparkline?.price.orEmpty(), nativeHistory))
                    }
                }
            }
        }
    }
}

data class BittensorWeeklyPriceSummary(val changePercent: BigDecimal, val sparkline: List<BigDecimal>)

/** Market sparklines are in USD; divide matched observations by the native USD price, like iOS. */
internal fun weeklySparklineInTao(alphaPrices: List<BigDecimal?>, nativeHistory: List<HistoricalCoinRate>): List<BigDecimal> {
    if (alphaPrices.size < 2) return emptyList()
    val nativePrices = nativeHistory.filter { it.rate.isPositive }.sortedBy { it.timestamp }
    val end = nativePrices.lastOrNull()?.timestamp ?: return emptyList()
    val timestamps = nativePrices.map { it.timestamp }
    val week = 7.days.inWholeSeconds
    val points = alphaPrices.mapIndexedNotNull { index, alpha ->
        if (alpha == null || !alpha.isPositive) return@mapIndexedNotNull null
        val timestamp = end - week + week * index / (alphaPrices.size - 1)
        val nativeIndex = nearestBittensorPriceIndex(timestamps, timestamp, PricePeriod.WEEK.samplingInterval().inWholeSeconds)
            ?: return@mapIndexedNotNull null
        alpha.divide(nativePrices[nativeIndex].rate, 18, RoundingMode.HALF_EVEN)
    }
    if (points.size < 2) return emptyList()
    val step = (points.size + MAX_SPARKLINE_POINTS - 1) / MAX_SPARKLINE_POINTS
    val firstIndex = points.lastIndex % step
    return (firstIndex..points.lastIndex step step).map { points[it] }
}

private const val MAX_SPARKLINE_POINTS = 48

internal fun weightedRootApy(positions: List<BittensorRootPosition>, rates: List<BigDecimal?>): BigDecimal? {
    if (positions.size != rates.size || rates.any { it == null }) return null
    val total = positions.sumOf { it.stakeInPlanks }.toBigDecimal()
    if (!total.isPositive) return null
    val weighted = positions.zip(rates).sumOf { (position, rate) -> position.stakeInPlanks.toBigDecimal() * requireNotNull(rate) }
    return weighted.divide(total, 12, RoundingMode.HALF_EVEN)
}

/** CoinGecko's market percentage is in USD; portfolio rows display the change relative to TAO. */
internal fun weeklyChangeInTao(market: BittensorSubnetMarketRemote, nativeHistory: List<HistoricalCoinRate>, nowMillis: Long): BigDecimal? {
    val updated = market.lastUpdated?.let { parseDateISO_8601(it) ?: parseDateISO_8601_NoMs(it) } ?: return null
    if (nowMillis - updated.time > 1.days.inWholeMilliseconds) return null
    val percent = market.weekChangePercent ?: return null
    val sorted = nativeHistory.filter { it.rate.isPositive }.sortedBy { it.timestamp }
    val end = sorted.lastOrNull() ?: return null
    val startTime = end.timestamp - 7.days.inWholeSeconds
    val startIndex = nearestBittensorPriceIndex(sorted.map { it.timestamp }, startTime, PricePeriod.WEEK.samplingInterval().inWholeSeconds)
        ?: return null
    val nativeGrowth = end.rate.divide(sorted[startIndex].rate, 18, RoundingMode.HALF_EVEN)
    if (!nativeGrowth.isPositive) return null
    return (BigDecimal.ONE + percent.movePointLeft(2))
        .divide(nativeGrowth, 18, RoundingMode.HALF_EVEN)
        .subtract(BigDecimal.ONE)
        .movePointRight(2)
}
