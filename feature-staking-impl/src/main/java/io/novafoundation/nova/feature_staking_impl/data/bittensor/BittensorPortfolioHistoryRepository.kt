package io.novafoundation.nova.feature_staking_impl.data.bittensor

import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorPortfolioHistory
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorPortfolioHistoryRequest
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorPortfolioFiatHistory
import io.novafoundation.nova.feature_wallet_api.data.repository.CoinPriceRepository
import io.novafoundation.nova.infrastructure.InfrastructureUrls
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
            withTimeout(BITTENSOR_REQUEST_TIMEOUT) {
                loadHistory(request, accountSubject)
            }
        } catch (e: TimeoutCancellationException) {
            currentCoroutineContext().ensureActive()
            throw BittensorTimeoutException("Portfolio history", e)
        }
    }

    private suspend fun loadHistory(request: BittensorPortfolioHistoryRequest, accountSubject: String): BittensorPortfolioHistory = coroutineScope {
        val fiatHistory = async { loadFiatHistory(request) }
        val url = urls.bittensorBackendUrl("portfolio/history/search")
        val response = api.history(url, accountSubject, request.period.name)
        val accountHistory = mapBittensorPortfolioHistory(response, request.period)
        BittensorPortfolioHistory(accountHistory, fiatHistory.await())
    }

    private suspend fun loadFiatHistory(request: BittensorPortfolioHistoryRequest): BittensorPortfolioFiatHistory {
        if (request.currency.code.equals("USD", ignoreCase = true)) return BittensorPortfolioFiatHistory.ReportedUsd

        val priceId = requireNotNull(request.nativePriceId) { "Missing native FX price identifier" }
        val nativePrices = coinPriceRepository.getLastHistoryForPeriod(priceId, request.currency, request.period.priceSource)
        return BittensorPortfolioFiatHistory.Converted(nativePrices)
    }
}
