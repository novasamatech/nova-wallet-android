package io.novafoundation.nova.feature_staking_impl.data.bittensor

import com.google.gson.Gson
import io.novafoundation.nova.common.data.memory.ComputationalCache
import io.novafoundation.nova.common.data.memory.observeSharedFlow
import io.novafoundation.nova.common.data.storage.Preferences
import io.novafoundation.nova.feature_staking_impl.data.cache.CachedResponse
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorReportedOperation
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorOperationHistory
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorNetuid
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorSettlementScope
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
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import retrofit2.HttpException
import retrofit2.Response

class BittensorOperationHistorySource(
    private val client: BittensorBackendClient,
    private val estimatesApi: BittensorEstimatesApi,
    private val portfolioHistorySource: BittensorPortfolioHistorySource,
    private val infrastructureUrls: InfrastructureUrls,
    private val computationalCache: ComputationalCache,
    private val preferences: Preferences,
    private val gson: Gson,
    private val spacingMillis: Long,
    private val nowNanos: () -> Long = System::nanoTime,
    private val nowMillis: () -> Long = System::currentTimeMillis
) {

    private class AccountHistory(initialGeneration: Long) {
        val generation = MutableStateFlow(initialGeneration)

        var seedBlockedUntilNanos: Long? = null

        var settledGeneration = initialGeneration

        val settlementDeadlines = mutableMapOf<BittensorSettlementScope, Long>()

        var users = 0
    }

    private data class HistoryKey(val baseUrl: HttpUrl, val address: String, val metaAccountId: Long)

    private val lock = Any()

    private val histories = linkedMapOf<HistoryKey, AccountHistory>()

    private var nextGeneration = 0L

    internal data class Snapshot(
        val request: BittensorHistoryRequest,
        val result: Result<BittensorOperationHistory>?,
        val settlingScopes: Set<BittensorSettlementScope> = emptySet()
    ) {
        val isSettling: Boolean get() = settlingScopes.isNotEmpty()
    }

    private data class StoredSettlement(val accountDeadlineMillis: Long?, val networkDeadlinesMillis: Map<Int, Long>)

    /** Capture before submission so an origin/account switch cannot invalidate another wallet's cache. */
    suspend fun request(address: String, metaAccountId: Long): BittensorHistoryRequest? {
        val origin = infrastructureUrls.baseUrl() ?: return null
        return synchronized(lock) {
            val state = accountHistory(HistoryKey(origin, address, metaAccountId))
            BittensorHistoryRequest(address, origin, state.generation.value, metaAccountId)
        }
    }

    /** Explicit cache command after a submitted own trade (success or unknown), never on signing. */
    suspend fun ownTradeSubmitted(request: BittensorHistoryRequest, scope: BittensorSettlementScope = BittensorSettlementScope.Account) {
        val key = HistoryKey(request.baseUrl, request.address, request.metaAccountId)
        synchronized(lock) {
            val state = accountHistory(key)
            state.seedBlockedUntilNanos = nowNanos() + SETTLEMENT_NANOS
            state.settlementDeadlines[scope] = requireNotNull(state.seedBlockedUntilNanos)
            preferences.putLong(settlementField(key), nowMillis() + SETTLEMENT_MILLIS)
            saveSettlementScopes(key, state)
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
    internal fun observe(address: String, metaAccountId: Long): Flow<Result<BittensorOperationHistory>?> =
        observeState(address, metaAccountId, BittensorNetuid.Root).map { it.result }

    fun observeState(address: String, metaAccountId: Long, netuid: BittensorNetuid): Flow<BittensorHistoryState> = flow {
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
                        BittensorHistoryState(snapshot.result, isSettling = snapshot.settlingScopes.affects(netuid))
                    } else {
                        val scopes = synchronized(lock) { state.settlementDeadlines.keys.toSet() }
                        BittensorHistoryState(null, isSettling = scopes.affects(netuid))
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

    private fun refreshHistory(request: BittensorHistoryRequest): Flow<Snapshot> = flow {
        val restoration = runCatching {
            checkOrigin(request)
            if (isSettling(request)) null else cachedHistory(request)
        }
        val restorationFailure = restoration.exceptionOrNull()
        if (restorationFailure is CancellationException) throw restorationFailure
        val restored = restoration.getOrNull()
        var lastResult = restored?.let { Result.success(it.toHistory()) }
        val initialResult = restorationFailure?.let { Result.failure<BittensorOperationHistory>(it) } ?: lastResult
        emit(Snapshot(request, initialResult, settlingScopes(request)))
        if (restorationFailure is OriginChanged) awaitCancellation()
        if (restored != null && nowNanos() < restored.validUntilNanos) {
            awaitRefresh(request, restored.validUntilNanos)
        }
        while (true) {
            val loaded = runCatching {
                val persist = mayPersist(request)
                loadCurrentHistory(request, persist)
            }
            val failure = loaded.exceptionOrNull()
            if (failure is CancellationException) throw failure
            val result = loaded.map(HistoryPages::toHistory).recoverCatching { error ->
                lastResult?.getOrNull()?.copy(stale = true) ?: throw error
            }
            lastResult = result.takeIf { it.isSuccess } ?: lastResult
            emit(Snapshot(request, result, settlingScopes(request)))
            // A shared stream never migrates to another origin under its original cache key.
            if (failure is OriginChanged) awaitCancellation()
            awaitRefresh(request, loaded.getOrNull()?.validUntilNanos)
        }
    }

    internal suspend fun history(request: BittensorHistoryRequest): BittensorOperationHistory = useHistory(request) {
        loadCurrentHistory(request, persist = mayPersist(request)).toHistory()
    }

    private suspend fun loadCurrentHistory(request: BittensorHistoryRequest, persist: Boolean): HistoryPages {
        checkOrigin(request)
        val startedAtNanos = nowNanos()
        val pages = loadHistoryPages(request.baseUrl, request.address, request.metaAccountId, persist)
        currentCoroutineContext().ensureActive()
        checkOrigin(request)
        synchronized(lock) {
            val key = HistoryKey(request.baseUrl, request.address, request.metaAccountId)
            val state = requireCurrent(key, request)
            // A completed fetch can settle A while a later trade in B still prevents account-wide cache reuse.
            // A request started before A's deadline cannot settle A, even if its response arrives afterwards.
            val settledNetworks = state.settlementDeadlines.entries.removeAll { it.value <= startedAtNanos }
            if (settledNetworks) saveSettlementScopes(key, state)
            if (persist) {
                state.settledGeneration = request.generation
                if (state.seedBlockedUntilNanos != null) {
                    state.seedBlockedUntilNanos = null
                    preferences.removeField(settlementField(key))
                }
            }
        }
        return pages
    }

    private fun mayPersist(request: BittensorHistoryRequest): Boolean = synchronized(lock) {
        val key = HistoryKey(request.baseUrl, request.address, request.metaAccountId)
        val deadline = requireCurrent(key, request).seedBlockedUntilNanos
        deadline == null || nowNanos() >= deadline
    }

    private fun isSettling(request: BittensorHistoryRequest): Boolean = synchronized(lock) {
        val state = histories.getValue(HistoryKey(request.baseUrl, request.address, request.metaAccountId))
        state.settledGeneration != state.generation.value
    }

    private fun settlingScopes(request: BittensorHistoryRequest): Set<BittensorSettlementScope> = synchronized(lock) {
        histories.getValue(HistoryKey(request.baseUrl, request.address, request.metaAccountId)).settlementDeadlines.keys.toSet()
    }

    private fun Set<BittensorSettlementScope>.affects(netuid: BittensorNetuid): Boolean =
        BittensorSettlementScope.Account in this || BittensorSettlementScope.Network(netuid) in this

    private suspend fun awaitRefresh(request: BittensorHistoryRequest, validUntilNanos: Long?) {
        val delayMillis = synchronized(lock) {
            val key = HistoryKey(request.baseUrl, request.address, request.metaAccountId)
            val settlementRemaining = histories.getValue(key).seedBlockedUntilNanos?.minus(nowNanos()) ?: 0L
            if (settlementRemaining > 0) {
                ((settlementRemaining + NANOS_PER_MILLI - 1) / NANOS_PER_MILLI).coerceAtMost(REFRESH_INTERVAL_MILLIS)
            } else {
                val cacheRemaining = validUntilNanos?.minus(nowNanos()) ?: 0L
                if (cacheRemaining > 0) {
                    ((cacheRemaining + NANOS_PER_MILLI - 1) / NANOS_PER_MILLI).coerceAtMost(300_000L)
                } else {
                    30_000L
                }
            }
        }
        // Subscribe only after loading: foreground during a request does not queue a duplicate refresh.
        withTimeoutOrNull(delayMillis) { client.foregroundRefreshes.firstOrNull() ?: awaitCancellation() }
    }

    private fun accountHistory(key: HistoryKey): AccountHistory {
        val state = histories.remove(key) ?: AccountHistory(++nextGeneration).apply {
            seedBlockedUntilNanos = restoredSettlementDeadline(key)
            val deadline = seedBlockedUntilNanos
            if (deadline != null) {
                settledGeneration = -1L
                settlementDeadlines.putAll(restoredSettlementScopes(key, deadline))
            } else {
                preferences.removeField(settlementScopesField(key))
            }
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

    private fun settlementScopesField(key: HistoryKey) = settlementField(key) + "_scopes"

    private fun saveSettlementScopes(key: HistoryKey, state: AccountHistory) {
        val field = settlementScopesField(key)
        if (state.settlementDeadlines.isEmpty()) {
            preferences.removeField(field)
            return
        }
        val now = nowNanos()
        val wallClock = nowMillis()

        fun wallDeadline(deadline: Long): Long = wallClock + (deadline - now) / NANOS_PER_MILLI

        val stored = StoredSettlement(
            accountDeadlineMillis = state.settlementDeadlines[BittensorSettlementScope.Account]?.let(::wallDeadline),
            networkDeadlinesMillis = state.settlementDeadlines.entries.mapNotNull { (scope, deadline) ->
                val network = scope as? BittensorSettlementScope.Network ?: return@mapNotNull null
                network.netuid.value to wallDeadline(deadline)
            }.toMap()
        )
        preferences.putString(field, gson.toJson(stored))
    }

    private fun restoredSettlementScopes(key: HistoryKey, legacyDeadline: Long): Map<BittensorSettlementScope, Long> {
        val restored = runCatching<Map<BittensorSettlementScope, Long>?> {
            val json = preferences.getString(settlementScopesField(key)) ?: return@runCatching null
            val stored = requireNotNull(gson.fromJson(json, StoredSettlement::class.java))
            val now = nowNanos()
            val wallClock = nowMillis()

            // Already elapsed scopes still need a successful fetch after restart before showing settled estimates.
            fun deadline(wallDeadline: Long): Long = now + (wallDeadline - wallClock).coerceIn(0L, SETTLEMENT_MILLIS) * NANOS_PER_MILLI

            buildMap<BittensorSettlementScope, Long> {
                stored.accountDeadlineMillis?.let { put(BittensorSettlementScope.Account, deadline(it)) }
                stored.networkDeadlinesMillis.forEach { (netuid, wallDeadline) ->
                    put(BittensorSettlementScope.Network(BittensorNetuid.from(netuid)), deadline(wallDeadline))
                }
            }
        }.getOrNull()
        // Existing versions only stored an account-wide deadline; retain that protection when scope is unknown.
        return restored ?: mapOf(BittensorSettlementScope.Account to legacyDeadline)
    }

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
        requireNotNull(loadPages { page -> loadHistoryPage(baseUrl, address, page, metaAccountId, persist) })

    private suspend fun cachedHistory(request: BittensorHistoryRequest): HistoryPages? = loadPages { page ->
        val snapshot = cachedOperationHistory(request.baseUrl, request.address, page, request.metaAccountId)
        snapshot?.let { historyPage(it.value, it.freshUntilNanos) }
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
            return historyPage(body, nowNanos())
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

    private suspend fun loadPages(loadPage: suspend (Int) -> HistoryPage?): HistoryPages? {
        val items = mutableListOf<BittensorReportedOperation>()
        var nextPage: Int? = FIRST_PAGE
        var validUntilNanos = Long.MAX_VALUE
        var pagesRead = 0
        var stale = false
        while (nextPage != null && pagesRead < HISTORY_PAGE_LIMIT) {
            // A missing saved page cannot become a successful, apparently complete history.
            val page = loadPage(nextPage) ?: return null
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
    ) {
        fun toHistory() = BittensorOperationHistory(items, allPagesRead, stale)
    }

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
        fetch = { url -> fetchHistoryPage(url, page) }
    )

    private suspend fun cachedOperationHistory(
        base: HttpUrl,
        address: String,
        page: Int,
        metaAccountId: Long
    ): CachedResponse<BittensorOperationsRemote>? = client.peekWithLifetime(
        url = operationHistoryUrl(base, address, page),
        type = BittensorOperationsRemote::class.java,
        metaAccountId = metaAccountId,
        validate = { validateOperationHistory(it, page) }
    )

    /** During indexer settlement reads must neither reuse nor seed the cache. */
    internal suspend fun uncachedOperationHistory(base: HttpUrl, address: String, page: Int): BittensorOperationsRemote {
        val response = client.fetch(operationHistoryUrl(base, address, page)) { url -> fetchHistoryPage(url, page) }
        if (!response.isSuccessful) throw HttpException(response)
        return requireNotNull(response.body()).also { validateOperationHistory(it, page) }
    }

    private suspend fun fetchHistoryPage(url: String, page: Int): Response<BittensorOperationsRemote> {
        // Throttle transport only; restored/fresh pages must not delay the first render.
        if (page > FIRST_PAGE) delay(spacingMillis)
        return estimatesApi.operations(url)
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
        const val INACTIVE_HISTORY_LIMIT = 32
        const val SETTLEMENT_MILLIS = 300_000L
        const val NANOS_PER_MILLI = 1_000_000L
        const val SETTLEMENT_NANOS = SETTLEMENT_MILLIS * NANOS_PER_MILLI
        const val REFRESH_INTERVAL_MILLIS = 60_000L
    }
}
