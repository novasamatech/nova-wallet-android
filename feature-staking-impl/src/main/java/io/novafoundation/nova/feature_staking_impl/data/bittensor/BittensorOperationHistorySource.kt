package io.novafoundation.nova.feature_staking_impl.data.bittensor

import io.novafoundation.nova.feature_staking_impl.data.cache.CachedResponse
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorReportedOperation
import okhttp3.HttpUrl.Companion.toHttpUrl
import retrofit2.HttpException
import kotlinx.coroutines.flow.channelFlow

import io.novafoundation.nova.common.data.memory.ComputationalCache
import io.novafoundation.nova.common.data.memory.observeSharedFlow
import io.novafoundation.nova.common.data.storage.Preferences
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorOperationHistory
import io.novafoundation.nova.infrastructure.InfrastructureUrls
import io.novasama.substrate_sdk_android.extensions.toHexString
import io.novasama.substrate_sdk_android.hash.Hasher.blake2b256
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.launch
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.HttpUrl

class BittensorOperationHistorySource(
    private val client: BittensorBackendClient,
    private val estimatesApi: BittensorEstimatesApi,
    private val portfolioHistorySource: BittensorPortfolioHistorySource,
    private val infrastructureUrls: InfrastructureUrls,
    private val computationalCache: ComputationalCache,
    private val preferences: Preferences,
    private val spacingMillis: Long,
    private val nowNanos: () -> Long = System::nanoTime,
    private val nowMillis: () -> Long = System::currentTimeMillis
) {

    private data class CachedHistory(val history: BittensorOperationHistory, val validUntilNanos: Long)

    private class AccountHistory(initialGeneration: Long) {
        val generation = MutableStateFlow(initialGeneration)

        var seedBlockedUntilNanos: Long? = null

        var latestFlight = 0L

        var settledGeneration = initialGeneration

        var users = 0
    }

    private data class CacheAdmission(val mayCache: Boolean, val flight: Long)

    private data class HistoryKey(val baseUrl: HttpUrl, val address: String, val metaAccountId: Long)

    private val lock = Any()

    private val histories = linkedMapOf<HistoryKey, AccountHistory>()

    private var nextGeneration = 0L

    private val cachedHistories = linkedMapOf<HistoryKey, CachedHistory>()

    internal data class Snapshot(
        val request: BittensorHistoryRequest,
        val result: Result<BittensorOperationHistory>?,
        val isSettling: Boolean = false
    )

    /** Capture before submission so an origin/account switch cannot invalidate another wallet's cache. */
    suspend fun request(address: String, metaAccountId: Long): BittensorHistoryRequest? {
        val origin = infrastructureUrls.baseUrl() ?: return null
        return synchronized(lock) {
            val state = accountHistory(HistoryKey(origin, address, metaAccountId))
            BittensorHistoryRequest(address, origin, state.generation.value, metaAccountId)
        }
    }

    /** Explicit cache command after a submitted own trade (success or unknown), never on signing. */
    suspend fun ownTradeSubmitted(request: BittensorHistoryRequest) {
        val key = HistoryKey(request.baseUrl, request.address, request.metaAccountId)
        synchronized(lock) {
            val state = accountHistory(key)
            cachedHistories.remove(key)
            state.seedBlockedUntilNanos = nowNanos() + SETTLEMENT_NANOS
            preferences.putLong(settlementField(key), nowMillis() + SETTLEMENT_MILLIS)
        }
        val operationsResult = runCatching {
            client.invalidateAccountResponses(request.baseUrl, request.address, request.metaAccountId, OPERATIONS_PATH)
        }
        val portfolioResult = if (operationsResult.exceptionOrNull() is CancellationException) {
            operationsResult
        } else {
            runCatching { portfolioHistorySource.invalidate(request.baseUrl, request.address, request.metaAccountId) }
        }
        // Complete the generation transition before rethrowing failures, including cancellation.
        synchronized(lock) {
            // Publish only after invalidation, so replacement requests get the new cache revision.
            cachedHistories.remove(key)
            accountHistory(key).generation.value = ++nextGeneration
        }
        val operationsError = operationsResult.exceptionOrNull()
        val portfolioError = portfolioResult.exceptionOrNull()
        if (operationsError is CancellationException) throw operationsError
        if (portfolioError is CancellationException) throw portfolioError
        if (operationsError != null && portfolioError != null && operationsError !== portfolioError) {
            operationsError.addSuppressed(portfolioError)
        }
        operationsResult.getOrThrow()
        portfolioResult.getOrThrow()
    }

    /** Shared while observed. Null means loading, never an unavailable or empty ledger. */
    fun observe(address: String, metaAccountId: Long): Flow<Result<BittensorOperationHistory>?> = observeState(address, metaAccountId).map { it.result }

    fun observeState(address: String, metaAccountId: Long): Flow<BittensorHistoryState> = flow {
        while (true) {
            val request = request(address, metaAccountId)
            if (request == null) {
                emit(BittensorHistoryState(Result.failure(IllegalStateException("Cost basis origin is unavailable")), false))
                delay(REFRESH_INTERVAL_MILLIS)
                continue
            }
            useHistory(request) { state ->
                // ComputationalCache logs keys. Neither the account nor the origin belongs in those logs.
                val identity = identity(HistoryKey(request.baseUrl, address, metaAccountId))
                val shared = computationalCache.observeSharedFlow("Bittensor.History.$identity") {
                    observeHistory(request)
                }
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

    internal fun observeHistory(request: BittensorHistoryRequest): Flow<Snapshot> = flow {
        useHistory(request) { state ->
            emitAll(state.generation.flatMapLatest { generation -> refreshHistory(request.copy(generation = generation)) })
        }
    }

    private fun refreshHistory(request: BittensorHistoryRequest): Flow<Snapshot> = channelFlow {
        val foreground = Channel<Unit>(Channel.CONFLATED)
        val observer = launch { client.foregroundRefreshes.collect { foreground.trySend(Unit) } }
        checkOrigin(request)
        val restored = if (!isSettling(request)) cachedHistory(request.address, request.metaAccountId) else null
        var lastResult: Result<BittensorOperationHistory>? = restored?.let {
            Result.success(
                BittensorOperationHistory(it.items, it.allPagesRead, stale = it.stale)
            )
        }
        send(Snapshot(request, lastResult, isSettling(request)))
        try {
            while (true) {
                val startedDuringSettlement = synchronized(lock) {
                    val key = HistoryKey(request.baseUrl, request.address, request.metaAccountId)
                    val deadline = histories.getValue(key).seedBlockedUntilNanos
                    deadline != null && nowNanos() < deadline
                }
                val result = try {
                    Result.success(history(request))
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    lastResult?.map { it.copy(stale = true) } ?: Result.failure(e)
                }
                lastResult = result.takeIf { it.isSuccess } ?: lastResult
                send(Snapshot(request, result, startedDuringSettlement || isSettling(request)))
                // A shared stream never migrates to another origin under its original cache key.
                if (result.exceptionOrNull() is OriginChanged) awaitCancellation()
                // Delay after completion: a ten-page request is not cancelled by a refresh tick.
                withTimeoutOrNull(refreshDelayMillis(request)) { foreground.receive() }
            }
        } finally {
            observer.cancel()
            foreground.close()
        }
    }

    suspend fun history(request: BittensorHistoryRequest): BittensorOperationHistory = useHistory(request) {
        loadHistory(request)
    }

    private suspend fun loadHistory(request: BittensorHistoryRequest): BittensorOperationHistory {
        checkOrigin(request)
        val key = HistoryKey(request.baseUrl, request.address, request.metaAccountId)
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
        val pages = loadHistoryPages(request.baseUrl, request.address, request.metaAccountId, persist = admission.mayCache)
        currentCoroutineContext().ensureActive()
        checkOrigin(request)
        val history = BittensorOperationHistory(pages.items, pages.allPagesRead, stale = pages.stale)
        synchronized(lock) {
            val state = requireCurrent(key, request)
            if (admission.mayCache) {
                state.settledGeneration = request.generation
                if (state.seedBlockedUntilNanos != null) {
                    state.seedBlockedUntilNanos = null
                    preferences.removeField(settlementField(key))
                }
            }
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
        val state = histories.getValue(HistoryKey(request.baseUrl, request.address, request.metaAccountId))
        state.settledGeneration != state.generation.value
    }

    private fun refreshDelayMillis(request: BittensorHistoryRequest): Long = synchronized(lock) {
        val key = HistoryKey(request.baseUrl, request.address, request.metaAccountId)
        val cachedRemaining = cachedHistories[key]?.validUntilNanos?.minus(nowNanos()) ?: 0L
        val deadline = histories.getValue(key).seedBlockedUntilNanos
        val remaining = deadline?.minus(nowNanos()) ?: 0L
        if (remaining > 0) {
            ((remaining + 999_999L) / 1_000_000L).coerceAtMost(REFRESH_INTERVAL_MILLIS)
        } else {
            (cachedRemaining / NANOS_PER_MILLI).coerceIn(30_000L, 300_000L)
        }
    }

    private fun accountHistory(key: HistoryKey): AccountHistory {
        val state = histories.remove(key) ?: AccountHistory(++nextGeneration).apply {
            seedBlockedUntilNanos = restoredSettlementDeadline(key)
            if (seedBlockedUntilNanos != null) settledGeneration = -1L
        }
        histories[key] = state
        trimHistories()
        return state
    }

    private suspend fun <T> useHistory(request: BittensorHistoryRequest, action: suspend (AccountHistory) -> T): T {
        val key = HistoryKey(request.baseUrl, request.address, request.metaAccountId)
        val state = synchronized(lock) {
            accountHistory(key).also { it.users += 1 }
        }
        return try {
            action(state)
        } finally {
            synchronized(lock) {
                state.users -= 1
                trimHistories()
            }
        }
    }

    private fun trimHistories() {
        val inactive = histories.filterValues { it.users == 0 }.keys
        inactive.take((inactive.size - INACTIVE_HISTORY_LIMIT).coerceAtLeast(0)).forEach { key ->
            histories.remove(key)
            cachedHistories.remove(key)
        }
    }

    private fun restoredSettlementDeadline(key: HistoryKey): Long? {
        val field = settlementField(key)
        if (!preferences.contains(field)) {
            // Carry forward an outstanding pre-migration trade window before switching to owner-scoped keys.
            val legacyIdentity = (key.baseUrl.toString() + "|" + key.address).encodeToByteArray().blake2b256().toHexString()
            val legacyField = "bittensor_history_settlement_$legacyIdentity"
            if (!preferences.contains(legacyField)) return null
            val legacyDeadline = preferences.getLong(legacyField, 0L)
            if (legacyDeadline <= nowMillis()) {
                preferences.removeField(legacyField)
                return null
            }
            preferences.putLong(field, legacyDeadline)
        }
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

    private fun identity(key: HistoryKey) = (key.metaAccountId.toString() + "|" + key.baseUrl.toString() + "|" + key.address).encodeToByteArray()
        .blake2b256()
        .toHexString()

    private fun requireCurrent(key: HistoryKey, request: BittensorHistoryRequest): AccountHistory {
        val state = checkNotNull(histories[key]) { "Cost basis history request expired" }
        check(state.generation.value == request.generation) { "Cost basis history was invalidated" }
        return state
    }

    private suspend fun checkOrigin(request: BittensorHistoryRequest) {
        if (infrastructureUrls.baseUrl() != request.baseUrl) throw OriginChanged()
    }

    private class OriginChanged : IllegalStateException("Cost basis origin has changed")

    internal suspend fun loadHistoryPages(baseUrl: HttpUrl, address: String, metaAccountId: Long, persist: Boolean = true): HistoryPages =
        loadPages(
            HISTORY_PAGE_LIMIT,
            spacingMillis
        ) { page -> loadHistoryPage(baseUrl, address, page, metaAccountId, persist) }

    private suspend fun cachedHistory(address: String, metaAccountId: Long): HistoryPages? {
        val items = mutableListOf<BittensorReportedOperation>()
        var page: Int? = FIRST_PAGE
        var deadline = Long.MAX_VALUE
        var count = 0
        var stale = false
        while (page != null && count < HISTORY_PAGE_LIMIT) {
            val current = page
            val snapshot = cachedOperationHistory(address, current, metaAccountId) ?: return null
            items += requireNotNull(snapshot.value.items).map(::mapBittensorReportedOperation)
            deadline = minOf(deadline, snapshot.freshUntilNanos)
            stale = stale || isReportedStale(snapshot.value)
            page = nextPage(snapshot.value.pageInfo)
            count += 1
        }
        return HistoryPages(items, deadline, allPagesRead = page == null, stale = stale)
    }

    private suspend fun loadHistoryPage(
        baseUrl: HttpUrl,
        address: String,
        page: Int,
        metaAccountId: Long,
        persist: Boolean
    ): HistoryPage {
        if (!persist) {
            val body = uncachedOperationHistory(baseUrl, address, page)
            return historyPage(body, System.nanoTime())
        }
        val snapshot = operationHistory(baseUrl, address, page, metaAccountId)
        return historyPage(snapshot.value, snapshot.freshUntilNanos)
    }

    private fun historyPage(body: BittensorOperationsRemote, deadline: Long) = HistoryPage(
        requireNotNull(body.items).map(::mapBittensorReportedOperation),
        nextPage(body.pageInfo),
        deadline,
        stale = isReportedStale(body)
    )

    private suspend fun loadPages(
        pageLimit: Int,
        spacingMillis: Long,
        loadPage: suspend (Int) -> HistoryPage
    ): HistoryPages {
        val items = mutableListOf<BittensorReportedOperation>()
        var nextPage: Int? = FIRST_PAGE
        var validUntilNanos = Long.MAX_VALUE
        var pagesRead = 0
        var stale = false

        while (nextPage != null && pagesRead < pageLimit) {
            if (pagesRead > 0) delay(spacingMillis)
            val page = loadPage(nextPage)
            items += page.items
            validUntilNanos = minOf(validUntilNanos, page.validUntilNanos)
            stale = stale || page.stale
            nextPage = page.nextPage
            pagesRead += 1
        }

        return HistoryPages(items, validUntilNanos, allPagesRead = nextPage == null, stale = stale)
    }

    private fun nextPage(info: BittensorHistoryPageInfo?): Int? = requireNotNull(info).nextPage

    private fun isReportedStale(body: BittensorOperationsRemote): Boolean = body.meta?.components?.operationHistory?.freshness != "FRESH"

    private data class HistoryPage(
        val items: List<BittensorReportedOperation>,
        val nextPage: Int?,
        val validUntilNanos: Long,
        val stale: Boolean
    )

    internal data class HistoryPages(
        val items: List<BittensorReportedOperation>,
        val validUntilNanos: Long,
        val allPagesRead: Boolean,
        val stale: Boolean
    )

    private suspend fun operationHistory(
        base: HttpUrl,
        address: String,
        page: Int,
        metaAccountId: Long
    ): CachedResponse<BittensorOperationsRemote> = client.getWithLifetime(
        url = operationHistoryUrl(base, address, page),
        type = BittensorOperationsRemote::class.java,
        metaAccountId = metaAccountId,
        validate = { validateOperationHistory(it, page) },
        fetch = estimatesApi::operations
    )

    private suspend fun cachedOperationHistory(
        address: String,
        page: Int,
        metaAccountId: Long
    ): CachedResponse<BittensorOperationsRemote>? = client.peekWithLifetime(
        url = operationHistoryUrl(client.baseUrl(), address, page),
        type = BittensorOperationsRemote::class.java,
        metaAccountId = metaAccountId,
        validate = { validateOperationHistory(it, page) }
    )

    /** During indexer settlement reads must neither reuse nor seed the cache. */
    internal suspend fun uncachedOperationHistory(base: HttpUrl, address: String, page: Int): BittensorOperationsRemote {
        val response = withTimeoutOrNull(BITTENSOR_REQUEST_TIMEOUT) { estimatesApi.operations(operationHistoryUrl(base, address, page)) }
            ?: throw BittensorTimeoutException("Backend operations/search")
        if (!response.isSuccessful) throw HttpException(response)
        return requireNotNull(response.body()).also { validateOperationHistory(it, page) }
    }

    private fun validateOperationHistory(body: BittensorOperationsRemote, page: Int) {
        requireAvailableBittensorComponent(body.meta?.components?.operationHistory, "Operation history")
        require(body.historyScope == "TAO_APP_PARTIAL")
        val items = requireNotNull(body.items)
        val info = requireNotNull(body.pageInfo) { "Missing operation history pagination" }
        require(info.page == page && page in FIRST_HISTORY_PAGE..MAX_HISTORY_PAGE) { "Unexpected operation history page" }
        require(info.pageSize == HISTORY_PAGE_SIZE) { "Unexpected operation history page size" }
        require(items.size <= HISTORY_PAGE_SIZE) { "Operation history page exceeds its declared size" }
        require(info.nextPage == null || info.nextPage == page + 1 && info.nextPage <= MAX_HISTORY_PAGE)
        items.forEach { mapBittensorReportedOperation(it) }
    }

    private fun operationHistoryUrl(base: HttpUrl, address: String, page: Int): String =
        bittensorBackendUrl(base, OPERATIONS_PATH).toHttpUrl().newBuilder()
            .addQueryParameter("accountSubject", address)
            .addQueryParameter("page", page.toString())
            .build().toString()

    private companion object {
        const val FIRST_PAGE = 1
        const val HISTORY_PAGE_LIMIT = 10
        const val FIRST_HISTORY_PAGE = 1
        const val MAX_HISTORY_PAGE = 100
        const val HISTORY_PAGE_SIZE = 100
        const val OPERATIONS_PATH = "operations/search"
        const val HISTORY_CACHE_LIMIT = 8
        const val INACTIVE_HISTORY_LIMIT = 32
        const val SETTLEMENT_MILLIS = 300_000L
        const val NANOS_PER_MILLI = 1_000_000L
        const val SETTLEMENT_NANOS = SETTLEMENT_MILLIS * NANOS_PER_MILLI
        const val REFRESH_INTERVAL_MILLIS = 60_000L
    }
}
