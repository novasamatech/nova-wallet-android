package io.novafoundation.nova.feature_staking_impl.data.bittensor

import io.novafoundation.nova.common.data.memory.ComputationalCache
import io.novafoundation.nova.common.data.memory.observeSharedFlow
import io.novafoundation.nova.common.utils.isNonNegative
import io.novafoundation.nova.feature_staking_impl.data.cache.ResponseCache
import io.novafoundation.nova.feature_staking_impl.data.cache.refreshOnForegroundOrInterval
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorPortfolioHistoryRequest
import io.novafoundation.nova.feature_wallet_api.data.repository.CoinPriceRepository
import io.novafoundation.nova.feature_wallet_api.domain.model.HistoricalCoinRate
import io.novasama.substrate_sdk_android.wsrpc.logging.Logger
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.withTimeoutOrNull

/** Public FX observations are independent of the wallet's portfolio history and survive offline restarts. */
class BittensorPortfolioFxSource(
    private val prices: CoinPriceRepository,
    private val cache: ResponseCache,
    private val computations: ComputationalCache,
    private val logger: Logger
) {

    private data class StoredRates(val items: List<HistoricalCoinRate>)

    fun observe(request: BittensorPortfolioHistoryRequest): Flow<List<HistoricalCoinRate>?> {
        if (request.currency.code.equals("USD", ignoreCase = true)) return flowOf(null)
        val key = key(request)
        return computations.observeSharedFlow(key) { observeRates(request, key) }
    }

    private fun observeRates(request: BittensorPortfolioHistoryRequest, key: String): Flow<List<HistoricalCoinRate>> = cache.foregroundRefreshes
        .refreshOnForegroundOrInterval(
            refreshInterval = 5.minutes,
            retryInterval = 30.seconds,
            refresh = { refresh(request, key) }
        )
        .onStart { emit(restored(key).orEmpty()) }

    private suspend fun restored(key: String): List<HistoricalCoinRate>? = cache.readSnapshot(
        key,
        StoredRates::class.java,
        metaAccountId = null,
        validate = ::validateRates
    )?.items

    private suspend fun refresh(request: BittensorPortfolioHistoryRequest, key: String): Result<List<HistoricalCoinRate>> = runCatching {
        val rates = withTimeoutOrNull(BITTENSOR_REQUEST_TIMEOUT) {
            prices.getLastHistoryForPeriod(requireNotNull(request.nativePriceId), request.currency, request.period.priceSource)
        } ?: throw BittensorTimeoutException("Portfolio FX")
        val stored = StoredRates(rates)
        validateRates(stored)
        cache.saveSnapshot(key, metaAccountId = null, value = stored)
        rates
    }
        .onFailure { failure ->
            if (failure is CancellationException) throw failure
            logger.log("[BittensorPortfolio] FX refresh failed; retaining saved observations")
            logger.log(failure)
        }

    private fun validateRates(stored: StoredRates) {
        require(stored.items.size <= MAX_OBSERVATIONS)
        stored.items.forEach { require(it.timestamp >= 0 && it.rate.isNonNegative) }
    }

    private fun key(request: BittensorPortfolioHistoryRequest): String = BittensorCacheKeys.portfolioFx(
        requireNotNull(request.nativePriceId),
        request.currency.code,
        request.period.name
    )

    private companion object {
        const val MAX_OBSERVATIONS = 10_000
    }
}
