package io.novafoundation.nova.feature_staking_impl.data.subtensor.network

import io.novafoundation.nova.feature_staking_impl.domain.subtensor.model.SubtensorStakingConstants
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Per-coldkey cache + in-flight dedup for the `StakeInfoRuntimeApi` call.
 *
 * The multistaking dashboard creates one sync service per (ChainAsset,
 * StakingType=SUBTENSOR), which on Bittensor means 129 services (1 TAO root +
 * 128 subnet alpha) firing concurrently when a wallet is selected. Each
 * service ultimately needs the same per-coldkey fetch — without this cache
 * they'd all hit the public RPC endpoint and most would be rate-limited.
 *
 * - In-flight dedup: the first caller starts the network round-trip, the
 *   other 128 await the same Deferred.
 * - 15 s result cache: subsequent polls within the TTL skip the network.
 * - Generation counter on `invalidate(coldkey)`: an in-flight task whose
 *   generation got bumped silently discards its own result rather than
 *   overwriting the cleared cache. We deliberately do NOT cancel the task
 *   because cancelling would propagate `CancellationException` to all 128
 *   awaiters and surface as transient errors on the dashboard.
 *
 * Mirrors the iOS `SubtensorPositionCache` actor.
 */
class SubtensorPositionCache(
    private val fetcher: SubtensorPositionFetcher,
    private val ttlMillis: Long = SubtensorStakingConstants.POSITION_CACHE_TTL_SECONDS * 1000,
    private val clock: () -> Long = System::currentTimeMillis,
) {

    private data class Key(val chainId: ChainId, val coldkey: List<Byte>)

    private data class CacheEntry(val positions: List<SubtensorRawPosition>, val expiry: Long)

    /** Sealed discriminator: cache hit, await another fetch, or own a new fetch. */
    private sealed interface Outcome {
        data class Cached(val positions: List<SubtensorRawPosition>) : Outcome
        data class Wait(val deferred: CompletableDeferred<List<SubtensorRawPosition>>) : Outcome
        data class Own(
            val deferred: CompletableDeferred<List<SubtensorRawPosition>>,
            val startedGeneration: Int,
        ) : Outcome
    }

    private val mutex = Mutex()
    private val cache = mutableMapOf<Key, CacheEntry>()
    private val inFlight = mutableMapOf<Key, CompletableDeferred<List<SubtensorRawPosition>>>()
    private val generation = mutableMapOf<Key, Int>()

    /**
     * Fires whenever [invalidate] is called. The dashboard updater races
     * this against its 30s polling delay so a fresh stake/unstake appears
     * on the dashboard within one Bittensor block of `inBlock` instead of
     * up to 30s later. Replay 0 because subscribers join long-lived; we
     * never want a buffered "kick" to retrigger a poll on subscription.
     */
    private val _invalidateKick = MutableSharedFlow<Unit>(replay = 0, extraBufferCapacity = 1)
    val invalidateKick: SharedFlow<Unit> = _invalidateKick.asSharedFlow()

    suspend fun positions(chainId: ChainId, coldkey: ByteArray): List<SubtensorRawPosition> {
        val key = Key(chainId, coldkey.toList())

        // Decide owner / waiter / cache-hit under the lock so the role is
        // unambiguous, then release the lock before any suspension.
        val outcome = mutex.withLock {
            val now = clock()
            val cached = cache[key]
            if (cached != null && cached.expiry > now) {
                return@withLock Outcome.Cached(cached.positions)
            }

            val pending = inFlight[key]
            if (pending != null) {
                return@withLock Outcome.Wait(pending)
            }

            val deferred = CompletableDeferred<List<SubtensorRawPosition>>()
            inFlight[key] = deferred
            Outcome.Own(deferred, generation.getOrDefault(key, 0))
        }

        return when (outcome) {
            is Outcome.Cached -> outcome.positions
            is Outcome.Wait -> outcome.deferred.await()
            is Outcome.Own -> runFetchAndPersist(chainId, coldkey, key, outcome.deferred, outcome.startedGeneration)
        }
    }

    private suspend fun runFetchAndPersist(
        chainId: ChainId,
        coldkey: ByteArray,
        key: Key,
        ourDeferred: CompletableDeferred<List<SubtensorRawPosition>>,
        startedGeneration: Int,
    ): List<SubtensorRawPosition> = coroutineScope {
        try {
            val result = fetcher.fetchPositions(chainId, coldkey)
            mutex.withLock {
                inFlight.remove(key)
                // Only cache if no `invalidate` fired during the fetch — otherwise
                // we'd repopulate the just-cleared cache with pre-stake data.
                if (generation.getOrDefault(key, 0) == startedGeneration) {
                    cache[key] = CacheEntry(result, clock() + ttlMillis)
                }
            }
            ourDeferred.complete(result)
            result
        } catch (e: Throwable) {
            mutex.withLock { inFlight.remove(key) }
            ourDeferred.completeExceptionally(e)
            throw e
        }
    }

    /**
     * Invalidates the cache for `coldkey` so the next request on any chain
     * re-fetches. Intended to be called from the stake confirm interactor
     * on `inBlock` so the dashboard catches up immediately.
     *
     * Does NOT cancel an in-flight fetch — see class doc for rationale.
     */
    suspend fun invalidate(coldkey: ByteArray) {
        mutex.withLock {
            val toRemove = cache.keys.filter { it.coldkey == coldkey.toList() }
            toRemove.forEach { key ->
                cache.remove(key)
                generation[key] = generation.getOrDefault(key, 0) + 1
            }
        }
        // Wake up the dashboard updater so the new stake appears within one
        // Bittensor block of `inBlock` instead of up to the 30s polling
        // interval later. tryEmit (extraBufferCapacity = 1) is non-suspending
        // and fine to drop if no subscriber is listening yet.
        _invalidateKick.tryEmit(Unit)
    }
}
