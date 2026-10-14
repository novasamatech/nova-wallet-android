package io.novafoundation.nova.feature_staking_impl.domain.bittensor

import io.novafoundation.nova.common.utils.isPositive
import io.novafoundation.nova.common.utils.isNonPositive
import io.novasama.substrate_sdk_android.hash.isPositive
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorSubnetMarketRepository
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorConfigRepository
import io.novafoundation.nova.feature_currency_api.domain.model.Currency
import io.novafoundation.nova.feature_wallet_api.data.repository.CoinPriceRepository
import io.novafoundation.nova.feature_wallet_api.data.repository.PricePeriod
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withTimeout
import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode

data class BittensorSubnetPricePoint(val timestamp: Long, val fiat: BigDecimal, val tao: BigDecimal)

enum class BittensorSubnetChartPeriod(val source: PricePeriod, val days: Int) {
    DAY(PricePeriod.DAY, 1),
    WEEK(PricePeriod.MONTH, 7),
    MONTH(PricePeriod.MONTH, 30),
    QUARTER(PricePeriod.MAX, 90),
    YEAR(PricePeriod.MAX, 365)
}

class BittensorSubnetDetailsInteractor(
    private val markets: BittensorSubnetMarketRepository,
    private val prices: CoinPriceRepository,
    private val configRepository: BittensorConfigRepository
) {
    suspend fun estimateAlpha(grossTao: BigDecimal, priceInRao: BigInteger?): BigDecimal? {
        val spot = priceInRao?.takeIf { it.isPositive() }?.let { BigDecimal(it, 9) } ?: return null
        val planks = grossTao.movePointRight(9).toBigInteger()
        if (planks.isNonPositive) return null
        return BigDecimal(planks - configRepository.config().commission.feeFor(planks), 9).divide(spot, 9, RoundingMode.DOWN)
    }

    suspend fun history(
        subnetId: BittensorSubnetId,
        taoPriceId: String,
        currency: Currency,
        period: BittensorSubnetChartPeriod
    ): List<BittensorSubnetPricePoint> =
        withTimeout(25_000L) {
            val alphaId = markets.priceId(subnetId) ?: return@withTimeout emptyList()
            coroutineScope {
                val sourcePeriod = period.source
                val alpha = async { prices.getLastHistoryForPeriod(alphaId, currency, sourcePeriod) }
                val tao = async { prices.getLastHistoryForPeriod(taoPriceId, currency, sourcePeriod) }
                val alphaPoints = alpha.await().sortedBy { it.timestamp }
                val taoPoints = tao.await().filter { it.rate.isPositive }.sortedBy { it.timestamp }
                if (taoPoints.isEmpty()) return@coroutineScope emptyList()
                val cutoff = (alphaPoints.lastOrNull()?.timestamp ?: 0L) - period.days * 86_400L
                var taoIndex = 0
                alphaPoints.filter { it.timestamp > cutoff && it.rate.isPositive }.mapNotNull { point ->
                    while (taoIndex + 1 < taoPoints.size && taoPoints[taoIndex + 1].timestamp <= point.timestamp) taoIndex++
                    val match = taoPoints[taoIndex]
                    // Do not divide historical alpha prices by today's TAO price or a distant point.
                    if (kotlin.math.abs(match.timestamp - point.timestamp) > 86_400L) return@mapNotNull null
                    BittensorSubnetPricePoint(point.timestamp, point.rate, point.rate.divide(match.rate, 12, RoundingMode.HALF_EVEN))
                }
            }
        }
}
