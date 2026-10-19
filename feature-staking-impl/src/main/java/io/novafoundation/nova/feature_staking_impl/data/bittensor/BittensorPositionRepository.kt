package io.novafoundation.nova.feature_staking_impl.data.bittensor

import io.novafoundation.nova.common.data.memory.ComputationalCache
import io.novafoundation.nova.common.utils.flowOfAll
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorDataStatus
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorNetuid
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorPortfolioPositions
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorStakePositions
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novafoundation.nova.runtime.repository.ChainStateRepository
import io.novasama.substrate_sdk_android.extensions.toHexString
import io.novasama.substrate_sdk_android.hash.Hasher.blake2b256
import io.novasama.substrate_sdk_android.runtime.AccountId
import io.novasama.substrate_sdk_android.wsrpc.logging.Logger
import java.math.BigInteger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

data class BittensorPositionState(val positions: BittensorStakePositions?, val status: BittensorDataStatus)

data class BittensorPortfolioState(val positions: BittensorPortfolioPositions?, val status: BittensorDataStatus)

interface BittensorPositionRepository {

    fun observePositions(chainId: ChainId, coldKey: AccountId): Flow<BittensorPositionState>

    fun observePortfolio(chainId: ChainId, coldKey: AccountId): Flow<BittensorPortfolioState>
}

class RealBittensorPositionRepository(
    private val positionSource: BittensorPositionSource,
    private val portfolioLoader: BittensorPortfolioLoader,
    private val chainStateRepository: ChainStateRepository,
    private val computationalCache: ComputationalCache,
    private val logger: Logger
) : BittensorPositionRepository {

    private data class PortfolioPrices(
        val heldSubnets: Set<BittensorNetuid.Subnet>,
        val prices: Map<BittensorNetuid.Subnet, BigInteger>
    )

    override fun observePositions(chainId: ChainId, coldKey: AccountId): Flow<BittensorPositionState> = flow {
        coroutineScope {
            val key = cacheKey("Positions", chainId, coldKey)
            val positions = computationalCache.useSharedFlow(key, this) { refreshPositions(chainId, coldKey) }
            emitAll(positions)
        }
    }

    override fun observePortfolio(chainId: ChainId, coldKey: AccountId): Flow<BittensorPortfolioState> = flow {
        coroutineScope {
            val key = cacheKey("Portfolio", chainId, coldKey)
            val portfolio = computationalCache.useSharedFlow(key, this) { valuePositions(chainId, coldKey) }
            emitAll(portfolio)
        }
    }

    private fun cacheKey(component: String, chainId: ChainId, coldKey: AccountId): String {
        // ComputationalCache logs its keys; do not include the account identifier itself.
        val accountKey = coldKey.blake2b256()
            .toHexString()
        return "Bittensor.$component.$chainId.$accountKey"
    }

    private fun refreshPositions(chainId: ChainId, coldKey: AccountId): Flow<BittensorPositionState> = flow {
        var cachedPositions: BittensorStakePositions? = null
        emit(BittensorPositionState(null, BittensorDataStatus.Loading))

        flowOfAll { chainStateRepository.currentRemoteBlockNumberFlow(chainId) }
            .conflate()
            .map {
                val state = loadPositions(chainId, coldKey, cachedPositions)
                cachedPositions = state.positions
                state
            }
            .retryWhen { cause, _ ->
                if (cause is CancellationException) throw cause
                emit(stalePositions(cachedPositions, cause))
                delay(SUBSCRIPTION_RETRY_DELAY_MILLIS)
                true
            }
            .collect { emit(it) }
    }

    private suspend fun loadPositions(
        chainId: ChainId,
        coldKey: AccountId,
        cachedPositions: BittensorStakePositions?
    ): BittensorPositionState = try {
        val positions = withTimeout(POSITIONS_TIMEOUT_MILLIS) { positionSource.positions(chainId, coldKey) }
        BittensorPositionState(positions, BittensorDataStatus.Fresh)
    } catch (e: TimeoutCancellationException) {
        currentCoroutineContext().ensureActive()
        stalePositions(cachedPositions, e)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        stalePositions(cachedPositions, e)
    }

    private fun stalePositions(positions: BittensorStakePositions?, cause: Throwable): BittensorPositionState {
        logger.log("[BittensorPositions] Failed to refresh positions")
        logger.log(cause)
        return BittensorPositionState(positions, BittensorDataStatus.Stale(cause))
    }

    private fun valuePositions(chainId: ChainId, coldKey: AccountId): Flow<BittensorPortfolioState> = flow {
        coroutineScope {
            val positions = observePositions(chainId, coldKey)
                .shareIn(this, SharingStarted.Eagerly, replay = 1)
            val prices = observePrices(chainId, positions)
            emitAll(combine(positions, prices, ::valueLatestPositions))
        }
    }

    private fun observePrices(chainId: ChainId, positions: Flow<BittensorPositionState>): Flow<PortfolioPrices> = channelFlow {
        var requestedSubnets = emptySet<BittensorNetuid.Subnet>()
        var priceJob: Job? = null
        send(PortfolioPrices(emptySet(), emptyMap()))

        positions.collect { state ->
            val heldSubnets = portfolioLoader.heldSubnets(state.positions)
            if (heldSubnets != requestedSubnets) {
                priceJob?.cancel()
                priceJob = null
                requestedSubnets = heldSubnets
                send(PortfolioPrices(heldSubnets, emptyMap()))
            }

            // Keep an in-flight request for the same held set. Amount/status updates
            // reach the independent combine immediately, without waiting for prices.
            if (state.status is BittensorDataStatus.Fresh && heldSubnets.isNotEmpty() && priceJob?.isActive != true) {
                priceJob = launch {
                    val loaded = portfolioLoader.loadPrices(chainId, heldSubnets)
                    ensureActive()
                    send(PortfolioPrices(heldSubnets, loaded))
                }
            }
        }
    }

    private fun valueLatestPositions(state: BittensorPositionState, prices: PortfolioPrices): BittensorPortfolioState {
        val positions = state.positions?.let {
            val matchingPrices = if (portfolioLoader.heldSubnets(it) == prices.heldSubnets) prices.prices else emptyMap()
            portfolioLoader.value(it, matchingPrices)
        }
        return BittensorPortfolioState(positions, state.status)
    }

    private companion object {

        const val POSITIONS_TIMEOUT_MILLIS = 20_000L

        const val SUBSCRIPTION_RETRY_DELAY_MILLIS = 1_000L
    }
}
