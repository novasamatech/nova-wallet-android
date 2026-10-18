package io.novafoundation.nova.feature_staking_impl.domain.bittensor

import io.novafoundation.nova.common.domain.ExtendedLoadingState
import io.novafoundation.nova.feature_currency_api.domain.model.Currency
import io.novafoundation.nova.feature_wallet_api.data.repository.CoinPriceRepository
import io.novafoundation.nova.feature_wallet_api.data.repository.PricePeriod
import io.novafoundation.nova.feature_wallet_api.data.repository.duration
import io.novafoundation.nova.feature_wallet_api.domain.price.AssetPriceChart
import io.novasama.substrate_sdk_android.wsrpc.logging.Logger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withTimeout

/** Market prices of TAO, not the wallet's historical stake valuation. */
class BittensorPriceChartInteractor(private val prices: CoinPriceRepository, private val logger: Logger) {

    fun observe(priceId: String, currency: Currency, period: PricePeriod): Flow<AssetPriceChart> = flow {
        emit(AssetPriceChart(period, ExtendedLoadingState.Loading))
        while (true) {
            val result = try {
                val sourcePeriod = when (period) {
                    PricePeriod.WEEK -> PricePeriod.MONTH
                    PricePeriod.YEAR -> PricePeriod.MAX
                    else -> period
                }
                val points = withTimeout(20_000L) { prices.getLastHistoryForPeriod(priceId, currency, sourcePeriod) }
                val filtered = if (period == sourcePeriod) {
                    points
                } else {
                    val start = (points.lastOrNull()?.timestamp ?: 0L) - period.duration().inWholeSeconds
                    points.filter { it.timestamp > start }
                }
                ExtendedLoadingState.Loaded(filtered)
            } catch (e: TimeoutCancellationException) {
                logFailure(period, e)
                ExtendedLoadingState.Error(e)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                logFailure(period, e)
                ExtendedLoadingState.Error(e)
            }
            emit(AssetPriceChart(period, result))
            delay(300_000L)
        }
    }

    private fun logFailure(period: PricePeriod, cause: Throwable) {
        logger.log("[BittensorChart] Failed to load market prices for $period")
        logger.log(cause)
    }
}
