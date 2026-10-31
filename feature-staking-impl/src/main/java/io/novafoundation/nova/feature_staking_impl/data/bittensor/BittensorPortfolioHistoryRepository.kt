package io.novafoundation.nova.feature_staking_impl.data.bittensor

import io.novafoundation.nova.feature_wallet_api.domain.model.HistoricalCoinRate
import kotlinx.coroutines.flow.Flow

import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorPortfolioHistory
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorPortfolioHistoryRequest
import io.novafoundation.nova.feature_wallet_api.data.repository.CoinPriceRepository
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withTimeout

class BittensorPortfolioHistoryRepository(
    private val api: BittensorPortfolioHistoryApi,
    private val coinPriceRepository: CoinPriceRepository,
    private val cache: BittensorResponseCache
) {

    suspend fun load(request: BittensorPortfolioHistoryRequest, accountSubject: String): BittensorPortfolioHistory {
        require(accountSubject.length in 1..128 && accountSubject.all { it in '!'..'~' }) { "Invalid account subject" }
        return try {
            withTimeout(BITTENSOR_REQUEST_TIMEOUT) {
                loadHistory(request, accountSubject)
            }
        } catch (e: TimeoutCancellationException) {
            currentCoroutineContext().ensureActive()
            throw BittensorTimeoutException("Portfolio history", e)
        }
    }

    fun observe(request: BittensorPortfolioHistoryRequest, accountSubject: String): Flow<BittensorPortfolioHistory> {
        require(accountSubject.length in 1..128 && accountSubject.all { it in '!'..'~' }) { "Invalid account subject" }
        return cache.observe(
            path = "portfolio/history/search",
            type = BittensorPortfolioHistoryRemote::class.java,
            account = accountSubject,
            query = "period=${request.period.name}",
            validate = { mapBittensorPortfolioHistory(it, request.period) },
            fetch = { endpoint -> api.history(endpoint, accountSubject, request.period.name) }
        ).map { snapshot ->
            val nativePrices = withTimeout(BITTENSOR_REQUEST_TIMEOUT) { nativePrices(request) }
            mapHistory(snapshot, request, nativePrices)
        }
    }

    private suspend fun nativePrices(request: BittensorPortfolioHistoryRequest) = if (request.currency.code.equals("USD", ignoreCase = true)) {
        null
    } else {
        val priceId = requireNotNull(request.nativePriceId) { "Missing native FX price identifier" }
        coinPriceRepository.getLastHistoryForPeriod(priceId, request.currency, request.period.priceSource)
    }

    private fun mapHistory(
        snapshot: BittensorCachedResponse<BittensorPortfolioHistoryRemote>,
        request: BittensorPortfolioHistoryRequest,
        nativePrices: List<HistoricalCoinRate>?
    ): BittensorPortfolioHistory {
        val mapped = mapBittensorPortfolioHistory(snapshot.value, request.period)
        val account = if (snapshot.stale) mapped.copy(status = mapped.status.copy(stale = true)) else mapped
        return BittensorPortfolioHistory(account, nativePrices)
    }

    private suspend fun loadHistory(request: BittensorPortfolioHistoryRequest, accountSubject: String): BittensorPortfolioHistory = coroutineScope {
        val nativePrices = async { nativePrices(request) }
        val snapshot = cache.get(
            path = "portfolio/history/search",
            type = BittensorPortfolioHistoryRemote::class.java,
            account = accountSubject,
            query = "period=${request.period.name}",
            allowStale = true,
            validate = { mapBittensorPortfolioHistory(it, request.period) },
            fetch = { endpoint -> api.history(endpoint, accountSubject, request.period.name) }
        )
        mapHistory(snapshot, request, nativePrices.await())
    }
}
