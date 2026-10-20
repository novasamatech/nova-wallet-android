package io.novafoundation.nova.feature_staking_impl.domain.bittensor

import io.novafoundation.nova.feature_currency_api.domain.model.Currency
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorSubnetMarketRepository
import io.novafoundation.nova.feature_wallet_api.data.repository.CoinPriceRepository
import io.novafoundation.nova.feature_wallet_api.data.repository.PricePeriod
import java.math.BigDecimal
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withTimeout

data class BittensorSubnetPricePoint(val timestamp: Long, val fiat: BigDecimal, val tao: BigDecimal)

enum class BittensorSubnetChartPeriod(val source: PricePeriod, val duration: Duration) {
    DAY(PricePeriod.DAY, 1.days),
    WEEK(PricePeriod.MONTH, 7.days),
    MONTH(PricePeriod.MONTH, 30.days),
    YEAR(PricePeriod.MAX, 365.days),
    ALL(PricePeriod.MAX, Duration.INFINITE)
}

class BittensorSubnetDetailsInteractor(
    private val markets: BittensorSubnetMarketRepository,
    private val prices: CoinPriceRepository
) {

    suspend fun history(
        subnetId: BittensorNetuid.Subnet,
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
                convertBittensorSubnetHistory(alpha.await(), tao.await(), period)
            }
        }
}
