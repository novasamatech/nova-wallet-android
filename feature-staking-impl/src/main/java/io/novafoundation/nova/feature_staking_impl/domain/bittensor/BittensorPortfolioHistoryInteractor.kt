package io.novafoundation.nova.feature_staking_impl.domain.bittensor

import io.novafoundation.nova.common.domain.ExtendedLoadingState
import io.novafoundation.nova.common.domain.map
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorPortfolioHistoryRepository
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorPositionUpdates
import io.novafoundation.nova.feature_wallet_api.data.repository.CoinPriceRepository
import io.novafoundation.nova.feature_wallet_api.domain.model.HistoricalCoinRate
import io.novafoundation.nova.feature_wallet_api.domain.model.amountFromPlanks
import io.novasama.substrate_sdk_android.wsrpc.logging.Logger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.withTimeout
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class BittensorPortfolioHistoryChart(
    val period: BittensorPortfolioHistoryPeriod,
    val series: ExtendedLoadingState<BittensorPortfolioValueSeries>
)

/**
 * The "Your Bittensor" total-stake chart: the coldkey's stake-value history from the Nova Bittensor service,
 * priced in the selected currency with the TAO fiat grid, with the live chain total as the newest point.
 */
class BittensorPortfolioHistoryInteractor(
    private val repository: BittensorPortfolioHistoryRepository,
    private val prices: CoinPriceRepository,
    private val positionUpdates: BittensorPositionUpdates,
    private val logger: Logger,
    private val clock: () -> Long = { System.currentTimeMillis() / 1000 },
    // The server keeps a series fresh for 5 minutes inside its hour-aligned window
    private val refreshInterval: Duration = 5.minutes,
    private val fetchTimeout: Duration = 20.seconds
) {

    private class Fetched(val history: BittensorPortfolioStakeHistory, val taoFiatGrid: List<HistoricalCoinRate>)

    /**
     * Refetches on every [BittensorPositionUpdates] revision (a confirmed operation) and every [refreshInterval];
     * a new [portfolio] emission only re-prices the live point. Loading is emitted once per wallet.
     */
    fun observe(portfolio: Flow<BittensorPortfolio>, period: BittensorPortfolioHistoryPeriod): Flow<BittensorPortfolioHistoryChart> {
        return portfolio.distinctUntilChangedBy { it.metaId to it.chain.id }.flatMapLatest { identity ->
            combine(fetchedFlow(identity, period), portfolio) { fetched, current ->
                val series = fetched.map { BittensorPortfolioValueSeriesCalculator.calculate(it.history, it.taoFiatGrid, livePointOf(current)) }
                BittensorPortfolioHistoryChart(period, series)
            }
        }
    }

    private fun fetchedFlow(identity: BittensorPortfolio, period: BittensorPortfolioHistoryPeriod): Flow<ExtendedLoadingState<Fetched>> = flow {
        emit(ExtendedLoadingState.Loading)

        val periodic = flow {
            while (true) {
                delay(refreshInterval)
                emit(Unit)
            }
        }

        merge(positionUpdates.updates.map { }, periodic).conflate().collect {
            val state = try {
                ExtendedLoadingState.Loaded(withTimeout(fetchTimeout) { fetch(identity, period) })
            } catch (e: TimeoutCancellationException) {
                // A CancellationException subclass: it must become an error here, or combine() swallows it and the chart never leaves loading
                logFailure(period, e)
                ExtendedLoadingState.Error(e)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                logFailure(period, e)
                ExtendedLoadingState.Error(e)
            }
            emit(state)
        }
    }

    private suspend fun fetch(identity: BittensorPortfolio, period: BittensorPortfolioHistoryPeriod): Fetched = coroutineScope {
        val token = identity.asset.token
        val priceId = checkNotNull(token.configuration.priceId) { "TAO price id is not configured" }

        val grid = async { prices.getLastHistoryForPeriod(priceId, token.currency, period.fiatGridPeriod) }
        val history = repository.history(identity.chain, identity.coldkey, period)

        Fetched(history, grid.await())
    }

    private fun livePointOf(portfolio: BittensorPortfolio): BittensorPortfolioLivePoint? {
        val token = portfolio.asset.token
        val totalStake = portfolio.positions?.totalStake ?: return null
        val rate = token.coinRate?.rate ?: return null

        return BittensorPortfolioLivePoint(clock(), token.amountFromPlanks(totalStake), rate)
    }

    private fun logFailure(period: BittensorPortfolioHistoryPeriod, cause: Throwable) {
        logger.log("[BittensorChart] Failed to load portfolio history for $period")
        logger.log(cause)
    }
}
