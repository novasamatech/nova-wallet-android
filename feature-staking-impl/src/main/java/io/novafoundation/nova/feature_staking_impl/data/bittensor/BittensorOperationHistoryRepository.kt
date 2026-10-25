package io.novafoundation.nova.feature_staking_impl.data.bittensor

import io.novafoundation.nova.common.data.memory.ComputationalCache
import io.novafoundation.nova.common.data.storage.Preferences
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorOperationHistory
import io.novafoundation.nova.infrastructure.InfrastructureUrls
import io.novasama.substrate_sdk_android.extensions.toHexString
import io.novasama.substrate_sdk_android.hash.Hasher.blake2b256
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.takeWhile
import okhttp3.HttpUrl

data class BittensorHistoryRequest internal constructor(
    val address: String,
    val baseUrl: HttpUrl,
    internal val generation: Long
)

data class BittensorHistoryState(
    val result: Result<BittensorOperationHistory>?,
    val isSettling: Boolean
)

class BittensorOperationHistoryRepository(
    private val remoteSource: BittensorEstimatesRemoteSource,
    private val infrastructureUrls: InfrastructureUrls,
    private val computationalCache: ComputationalCache,
    private val preferences: Preferences,
    private val nowNanos: () -> Long = System::nanoTime,
    private val nowMillis: () -> Long = System::currentTimeMillis
) {

    private data class CachedHistory(val history: BittensorOperationHistory, val validUntilNanos: Long)

    private class AccountHistory {
        val generation = MutableStateFlow(0L)

        var seedBlockedUntilNanos: Long? = null

        var latestFlight = 0L

        var settledGeneration = 0L
    }

    private data class CacheAdmission(val mayCache: Boolean, val flight: Long)

    private data class HistoryKey(val baseUrl: HttpUrl, val address: String)

    private val lock = Any()

    private val histories = mutableMapOf<HistoryKey, AccountHistory>()

    private val cachedHistories = linkedMapOf<HistoryKey, CachedHistory>()

    internal data class Snapshot(
        val request: BittensorHistoryRequest,
        val result: Result<BittensorOperationHistory>?,
        val isSettling: Boolean = false
    )

    /** Capture before submission so an origin/account switch cannot invalidate another wallet's cache. */
    suspend fun request(address: String): BittensorHistoryRequest? {
        val origin = infrastructureUrls.baseUrl() ?: return null
        return synchronized(lock) {
            val state = accountHistory(HistoryKey(origin, address))
            BittensorHistoryRequest(address, origin, state.generation.value)
        }
    }

    /** Explicit cache command after a submitted own trade (success or unknown), never on signing. */
    fun ownTradeSubmitted(request: BittensorHistoryRequest) = synchronized(lock) {
        val key = HistoryKey(request.baseUrl, request.address)
        val state = accountHistory(key)
        cachedHistories.remove(key)
        state.seedBlockedUntilNanos = nowNanos() + SETTLEMENT_NANOS
        state.generation.value += 1
        // The indexer lag outlives the process, so a restart inside the window must still settle.
        preferences.putLong(settlementField(key), nowMillis() + SETTLEMENT_MILLIS)
    }

    /** Shared while observed. Null means loading, never an unavailable or empty ledger. */
    fun observe(address: String): Flow<Result<BittensorOperationHistory>?> = observeState(address).map { it.result }

    fun observeState(address: String): Flow<BittensorHistoryState> = flow {
        while (true) {
            val request = request(address)
            if (request == null) {
                emit(BittensorHistoryState(Result.failure(IllegalStateException("Cost basis origin is unavailable")), false))
                delay(REFRESH_INTERVAL_MILLIS)
                continue
            }
            coroutineScope {
                // ComputationalCache logs keys. Neither the account nor the origin belongs in those logs.
                val identity = identity(HistoryKey(request.baseUrl, address))
                val shared = computationalCache.useSharedFlow("Bittensor.History.$identity", this) {
                    observeHistory(request)
                }
                val state = synchronized(lock) { histories.getValue(HistoryKey(request.baseUrl, address)) }
                combine(shared, state.generation) { snapshot, generation ->
                    // Clear immediately even if a cancelled old transport is slow to finish.
                    if (snapshot.request.generation == generation) {
                        BittensorHistoryState(snapshot.result, snapshot.isSettling)
                    } else {
                        BittensorHistoryState(null, true)
                    }
                }
                    .takeWhile { infrastructureUrls.baseUrl() == request.baseUrl }
                    .collect { emit(it) }
            }
            emit(BittensorHistoryState(null, false))
        }
    }

    internal fun observeHistory(request: BittensorHistoryRequest): Flow<Snapshot> {
        val state = synchronized(lock) { histories.getValue(HistoryKey(request.baseUrl, request.address)) }
        return state.generation.flatMapLatest { generation ->
            refreshHistory(request.copy(generation = generation))
        }
    }

    private fun refreshHistory(request: BittensorHistoryRequest): Flow<Snapshot> = flow {
        emit(Snapshot(request, null, isSettling(request)))
        while (true) {
            val startedDuringSettlement = synchronized(lock) {
                val deadline = histories.getValue(HistoryKey(request.baseUrl, request.address)).seedBlockedUntilNanos
                deadline != null && nowNanos() < deadline
            }
            val result = try {
                Result.success(history(request))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Result.failure(e)
            }
            emit(Snapshot(request, result, startedDuringSettlement || isSettling(request)))
            // A shared stream never migrates to another origin under its original cache key.
            if (result.exceptionOrNull() is OriginChanged) awaitCancellation()
            // Delay after completion: a ten-page request is not cancelled by a refresh tick.
            delay(refreshDelayMillis(request))
        }
    }

    suspend fun history(request: BittensorHistoryRequest): BittensorOperationHistory {
        checkOrigin(request)
        val key = HistoryKey(request.baseUrl, request.address)
        val admission = synchronized(lock) {
            val state = requireCurrent(key, request)
            val now = nowNanos()
            val blockedUntil = state.seedBlockedUntilNanos
            val seedAllowed = blockedUntil == null || now >= blockedUntil
            val cached = cachedHistories[key]
            if (seedAllowed && cached != null && now < cached.validUntilNanos) {
                cachedHistories.remove(key)
                cachedHistories[key] = cached
                return cached.history
            }
            state.latestFlight += 1
            CacheAdmission(seedAllowed, state.latestFlight)
        }
        val pages = remoteSource.history(request.baseUrl, request.address)
        currentCoroutineContext().ensureActive()
        checkOrigin(request)
        val history = BittensorOperationHistory(pages.items, pages.allPagesRead)
        synchronized(lock) {
            val state = requireCurrent(key, request)
            if (admission.mayCache) state.settledGeneration = request.generation
            // Settlement responses may be displayed, but never seed a later subscription.
            // Admission uses eligibility at request start and the remote request-start TTL.
            if (admission.mayCache && state.latestFlight == admission.flight && nowNanos() < pages.validUntilNanos) {
                cachedHistories.remove(key)
                cachedHistories[key] = CachedHistory(history, pages.validUntilNanos)
                if (cachedHistories.size > HISTORY_CACHE_LIMIT) cachedHistories.remove(cachedHistories.keys.first())
            }
        }
        return history
    }

    private fun isSettling(request: BittensorHistoryRequest): Boolean = synchronized(lock) {
        val state = histories.getValue(HistoryKey(request.baseUrl, request.address))
        state.settledGeneration != state.generation.value
    }

    private fun refreshDelayMillis(request: BittensorHistoryRequest): Long = synchronized(lock) {
        val deadline = histories.getValue(HistoryKey(request.baseUrl, request.address)).seedBlockedUntilNanos
        val remaining = deadline?.minus(nowNanos()) ?: 0L
        if (remaining > 0) {
            ((remaining + 999_999L) / 1_000_000L).coerceAtMost(REFRESH_INTERVAL_MILLIS)
        } else {
            REFRESH_INTERVAL_MILLIS
        }
    }

    private fun accountHistory(key: HistoryKey): AccountHistory = histories.getOrPut(key) {
        AccountHistory().apply {
            val restoredDeadline = restoredSettlementDeadline(key) ?: return@apply
            // Resume an own-trade window started before a restart exactly as ownTradeSubmitted opened it.
            seedBlockedUntilNanos = restoredDeadline
            generation.value += 1
        }
    }

    private fun restoredSettlementDeadline(key: HistoryKey): Long? {
        val field = settlementField(key)
        if (!preferences.contains(field)) return null
        // Wall clock survives a restart; the cap keeps a clock moved back from extending the window.
        val remainingMillis = (preferences.getLong(field, 0L) - nowMillis()).coerceAtMost(SETTLEMENT_MILLIS)
        if (remainingMillis <= 0) {
            preferences.removeField(field)
            return null
        }
        return nowNanos() + remainingMillis * NANOS_PER_MILLI
    }

    // Preferences are plain text on disk, so the field name carries only the hashed identity.
    private fun settlementField(key: HistoryKey) = "bittensor_history_settlement_" + identity(key)

    private fun identity(key: HistoryKey) = (key.baseUrl.toString() + "|" + key.address).encodeToByteArray()
        .blake2b256()
        .toHexString()

    private fun requireCurrent(key: HistoryKey, request: BittensorHistoryRequest): AccountHistory {
        val state = histories.getValue(key)
        check(state.generation.value == request.generation) { "Cost basis history was invalidated" }
        return state
    }

    private suspend fun checkOrigin(request: BittensorHistoryRequest) {
        if (infrastructureUrls.baseUrl() != request.baseUrl) throw OriginChanged()
    }

    private class OriginChanged : IllegalStateException("Cost basis origin has changed")

    private companion object {
        const val HISTORY_CACHE_LIMIT = 8
        const val SETTLEMENT_MILLIS = 300_000L
        const val NANOS_PER_MILLI = 1_000_000L
        const val SETTLEMENT_NANOS = SETTLEMENT_MILLIS * NANOS_PER_MILLI
        const val REFRESH_INTERVAL_MILLIS = 60_000L
    }
}
