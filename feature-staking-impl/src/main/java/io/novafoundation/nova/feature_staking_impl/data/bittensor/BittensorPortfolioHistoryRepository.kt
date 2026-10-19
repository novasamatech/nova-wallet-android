package io.novafoundation.nova.feature_staking_impl.data.bittensor

import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorPortfolioHistory
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorPortfolioHistoryRequest
import io.novafoundation.nova.feature_wallet_api.data.repository.CoinPriceRepository
import io.novafoundation.nova.infrastructure.InfrastructureUrls
import io.novafoundation.nova.infrastructure.resolve
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withTimeout

class BittensorPortfolioHistoryRepository(
    private val api: BittensorPortfolioHistoryApi,
    private val urls: InfrastructureUrls,
    private val coinPriceRepository: CoinPriceRepository
) {

    suspend fun load(request: BittensorPortfolioHistoryRequest, accountSubject: String): BittensorPortfolioHistory {
        require(accountSubject.length in 1..128 && accountSubject.all { it in '!'..'~' }) { "Invalid account subject" }
        return try {
            withTimeout(REQUEST_TIMEOUT_MILLIS) {
                loadHistory(request, accountSubject)
            }
        } catch (e: TimeoutCancellationException) {
            currentCoroutineContext().ensureActive()
            throw IllegalStateException("Portfolio history timed out", e)
        }
    }

    private suspend fun loadHistory(request: BittensorPortfolioHistoryRequest, accountSubject: String): BittensorPortfolioHistory = coroutineScope {
        val nativePrices = async {
            if (request.currency.code.equals("USD", ignoreCase = true)) {
                null
            } else {
                val priceId = requireNotNull(request.nativePriceId) { "Missing native FX price identifier" }
                coinPriceRepository.getLastHistoryForPeriod(priceId, request.currency, request.period.priceSource)
            }
        }
        val url = urls.resolve("/v1/bittensor/portfolio/history/search").toString()
        val response = api.history(url, accountSubject, request.period.name)
        val accountHistory = mapBittensorPortfolioHistory(response, request.period)
        BittensorPortfolioHistory(accountHistory, nativePrices.await())
    }

    private companion object {

        const val REQUEST_TIMEOUT_MILLIS = 25_000L
    }
}
