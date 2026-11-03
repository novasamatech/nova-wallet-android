package io.novafoundation.nova.feature_staking_impl.data.bittensor

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.common.data.memory.ComputationalCache
import io.novafoundation.nova.common.data.memory.observeSharedFlow
import io.novafoundation.nova.common.utils.flowOfAll
import io.novafoundation.nova.common.utils.isNonNegative
import io.novafoundation.nova.feature_staking_impl.data.cache.ResponseCache
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorDataStatus
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorNetuid
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorPositionUpdates
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorStakePositions
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novafoundation.nova.runtime.repository.ChainStateRepository
import io.novasama.substrate_sdk_android.extensions.toHexString
import io.novasama.substrate_sdk_android.runtime.AccountId
import io.novasama.substrate_sdk_android.wsrpc.logging.Logger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.withTimeout

data class BittensorPositionState(val positions: BittensorStakePositions?, val status: BittensorDataStatus)

/** Owns the shared position stream and its durable snapshots for an explicit wallet and chain account. */
class BittensorPositionSource(
    private val remoteSource: BittensorPositionRemoteSource,
    private val chainStateRepository: ChainStateRepository,
    private val computationalCache: ComputationalCache,
    private val logger: Logger,
    private val positionUpdates: BittensorPositionUpdates,
    private val cache: ResponseCache
) {

    fun observePositions(chainId: ChainId, coldKey: AccountId, metaAccountId: Long): Flow<BittensorPositionState> = computationalCache.observeSharedFlow(
        key = BittensorCacheKeys.sharedPositions(chainId, coldKey, metaAccountId)
    ) {
        refreshPositions(chainId, coldKey, metaAccountId)
    }

    suspend fun invalidate(chainId: ChainId, coldKey: AccountId, metaAccountId: Long) {
        val key = BittensorCacheKeys.positions(chainId, coldKey.toHexString())
        cache.invalidate(keepStale = true) { it.metaAccountId == metaAccountId && it.normalizedUrl == key }
    }

    private fun refreshPositions(chainId: ChainId, coldKey: AccountId, metaAccountId: Long): Flow<BittensorPositionState> = flow {
        val key = BittensorCacheKeys.positions(chainId, coldKey.toHexString())
        var cachedPositions = cache.readSnapshot(
            key,
            BittensorStakePositions::class.java,
            metaAccountId,
            validate = ::validatePositions
        )
        val initialStatus = if (cachedPositions == null) BittensorDataStatus.Loading else BittensorDataStatus.Stale(BittensorCacheStaleException())
        emit(BittensorPositionState(cachedPositions, initialStatus))

        val confirmed = positionUpdates.updates
            .filter { it.chainId == chainId && it.coldkey == AccountIdKey(coldKey) }
            .map { Unit }
        merge(
            flowOf(Unit),
            cache.foregroundRefreshes,
            flowOfAll { chainStateRepository.currentRemoteBlockNumberFlow(chainId) }.drop(1).map { Unit },
            confirmed
        )
            .conflate()
            .map {
                val state = loadPositions(chainId, coldKey, cachedPositions, metaAccountId)
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
        cachedPositions: BittensorStakePositions?,
        metaAccountId: Long
    ): BittensorPositionState = try {
        val key = BittensorCacheKeys.positions(chainId, coldKey.toHexString())
        val ticket = cache.snapshotTicket(key, metaAccountId)
        val positions = withTimeout(BITTENSOR_REQUEST_TIMEOUT) { remoteSource.positions(chainId, coldKey) }
        currentCoroutineContext().ensureActive()
        check(cache.saveSnapshot(key, metaAccountId, positions, ticket)) {
            "Position request was invalidated"
        }
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

    private fun validatePositions(stored: BittensorStakePositions) {
        stored.root.forEach {
            require(it.validatorHotkey.value.size == 32 && it.stakeInPlanks.isNonNegative)
        }
        stored.subnets.forEach {
            BittensorNetuid.Subnet(it.subnetId.value)
            require(it.validatorHotkey.value.size == 32 && it.alphaInPlanks.isNonNegative)
        }
    }

    private companion object {

        const val SUBSCRIPTION_RETRY_DELAY_MILLIS = 1_000L
    }
}
