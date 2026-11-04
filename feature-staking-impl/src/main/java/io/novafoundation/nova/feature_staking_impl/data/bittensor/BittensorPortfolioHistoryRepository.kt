package io.novafoundation.nova.feature_staking_impl.data.bittensor

import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorPortfolioHistory
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorPortfolioHistoryRequest
import io.novafoundation.nova.feature_wallet_api.domain.model.HistoricalCoinRate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class BittensorPortfolioHistoryRepository(
    private val fxSource: BittensorPortfolioFxSource,
    private val historySource: BittensorPortfolioHistorySource
) {

    fun observe(request: BittensorPortfolioHistoryRequest, accountSubject: String): Flow<BittensorPortfolioHistory> {
        require(accountSubject.length in 1..128 && accountSubject.all { it in '!'..'~' }) { "Invalid account subject" }
        return combine(
            historySource.observePortfolioHistory(request, accountSubject),
            fxSource.observe(request)
        ) { snapshot, nativePrices -> mapHistory(snapshot, request, nativePrices) }
    }

    private fun mapHistory(
        snapshot: BittensorPortfolioHistoryRemote,
        request: BittensorPortfolioHistoryRequest,
        nativePrices: List<HistoricalCoinRate>?
    ): BittensorPortfolioHistory {
        val account = mapBittensorPortfolioHistory(snapshot, request.period)
        return BittensorPortfolioHistory(account, nativePrices)
    }
}
