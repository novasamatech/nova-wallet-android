package io.novafoundation.nova.feature_staking_impl.domain.bittensor

import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.domain.model.requireAccountIdIn
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorPortfolioHistoryRepository
import io.novafoundation.nova.runtime.ext.addressOf
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import java.math.BigDecimal
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge

data class BittensorPortfolioChartHistory(
    val request: BittensorPortfolioHistoryRequest,
    val history: BittensorPortfolioHistory
)

class BittensorPortfolioChartInteractor(
    private val historyRepository: BittensorPortfolioHistoryRepository,
    private val calculator: BittensorPortfolioChartCalculator,
    private val accounts: AccountRepository,
    private val chainRegistry: ChainRegistry,
    private val positionUpdates: BittensorPositionUpdates
) {

    fun observeHistory(request: BittensorPortfolioHistoryRequest): Flow<BittensorPortfolioChartHistory> {
        val periodic = flow {
            while (true) {
                emit(Unit)
                delay(HISTORY_REFRESH_MILLIS)
            }
        }
        val confirmed = positionUpdates.updates
            .filter { it.metaId == request.metaId && it.chainId == request.chainId }
            .map { Unit }
        return merge(periodic, confirmed).map {
            val chain = chainRegistry.getChain(request.chainId)
            val account = accounts.getMetaAccount(request.metaId)
            val address = chain.addressOf(account.requireAccountIdIn(chain))
            BittensorPortfolioChartHistory(request, historyRepository.load(request, address))
        }
    }

    fun calculate(portfolio: BittensorPortfolio, history: BittensorPortfolioHistory): BittensorPortfolioValueSeries {
        val liveAnchor = liveAnchor(portfolio)
        return calculator.calculate(history, liveAnchor)
    }

    private fun liveAnchor(portfolio: BittensorPortfolio): BittensorPortfolioLiveAnchor? {
        if (portfolio.dataStatus !is BittensorDataStatus.Fresh) return null
        val totalStake = portfolio.positions?.totalStake ?: return null
        val token = portfolio.asset.token
        val amount = BigDecimal(totalStake, token.configuration.precision.value)
        return BittensorPortfolioLiveAnchor(amount, token.coinRate?.rate)
    }

    private companion object {

        const val HISTORY_REFRESH_MILLIS = 300_000L
    }
}
