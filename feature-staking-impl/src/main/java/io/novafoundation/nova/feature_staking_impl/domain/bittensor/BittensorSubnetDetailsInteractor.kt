package io.novafoundation.nova.feature_staking_impl.domain.bittensor

import io.novafoundation.nova.common.utils.isNonPositive
import io.novafoundation.nova.common.utils.isPositive
import io.novafoundation.nova.feature_currency_api.domain.model.Currency
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorConfigRepository
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorSubnetMarketRepository
import io.novafoundation.nova.feature_wallet_api.data.repository.CoinPriceRepository
import io.novafoundation.nova.feature_wallet_api.data.repository.PricePeriod
import io.novafoundation.nova.feature_wallet_api.domain.model.amountFromPlanks
import io.novafoundation.nova.feature_wallet_api.domain.model.planksFromAmount
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novasama.substrate_sdk_android.hash.isPositive
import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode
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
    QUARTER(PricePeriod.MAX, 90.days),
    YEAR(PricePeriod.MAX, 365.days)
}

class BittensorSubnetDetailsInteractor(
    private val markets: BittensorSubnetMarketRepository,
    private val prices: CoinPriceRepository,
    private val configRepository: BittensorConfigRepository
) {

    suspend fun estimateAlpha(grossTao: BigDecimal, priceInRao: BigInteger?, nativeAsset: Chain.Asset): BigDecimal? {
        val spot = priceInRao?.takeIf { it.isPositive() }?.let { nativeAsset.amountFromPlanks(it) } ?: return null
        val planks = nativeAsset.planksFromAmount(grossTao)
        if (planks.isNonPositive) return null
        val netAmount = nativeAsset.amountFromPlanks(planks - configRepository.config().commission.feeFor(planks))
        return netAmount.divide(spot, BITTENSOR_ALPHA_DECIMALS, RoundingMode.DOWN)
    }

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
