package io.novafoundation.nova.feature_staking_impl.data.bittensor

import com.google.gson.Gson
import io.novafoundation.nova.common.data.memory.ComputationalCache
import io.novafoundation.nova.common.data.storage.Preferences
import io.novafoundation.nova.core_db.dao.ResponseCacheDao
import io.novafoundation.nova.feature_staking_impl.data.cache.ResponseCache
import io.novafoundation.nova.core_db.model.ResponseCacheKey
import io.novafoundation.nova.core_db.model.ResponseCacheLocal
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorStakePositions
import io.novafoundation.nova.infrastructure.InfrastructureUrls
import io.novasama.substrate_sdk_android.runtime.AccountId
import io.novasama.substrate_sdk_android.wsrpc.logging.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.mockito.Mockito.mock

/**
 * A chain that never answers. Position streams are shared with replay 1, so a test that reads their first value
 * only sees the saved snapshot when no chain answer can overtake it.
 */
class PendingBittensorPositionRemote : BittensorPositionRemoteSource {

    override suspend fun positions(chainId: String, coldKey: AccountId): BittensorStakePositions = awaitCancellation()
}

class InMemoryResponseCacheDao(private val owners: MutableSet<Long>? = null) : ResponseCacheDao {

    val rows = linkedMapOf<String, ResponseCacheLocal>()

    private val changes = MutableStateFlow(0L)

    override suspend fun get(url: String, metaAccountId: Long?) = rows[ResponseCacheKey(url, metaAccountId).storageKey]

    override fun observe(url: String, metaAccountId: Long?) = changes.map { get(url, metaAccountId) }

    override suspend fun put(entry: ResponseCacheLocal) {
        require(entry.metaAccountId == null || owners == null || entry.metaAccountId in owners) { "Deleted owner" }
        rows[entry.key] = entry
        changes.value++
    }

    fun deleteOwner(owner: Long) {
        owners?.remove(owner)
        rows.entries.removeAll { it.value.metaAccountId == owner }
        changes.value++
    }

    override suspend fun ownerExists(metaAccountId: Long): Boolean = owners == null || metaAccountId in owners

    override suspend fun keys() = rows.values.map { ResponseCacheKey(it.normalizedUrl, it.metaAccountId) }

    override suspend fun delete(keys: List<String>) {
        keys.forEach(rows::remove)
        changes.value++
    }

    override suspend fun trim(limit: Int) {
        rows.values.filter { it.expiresAt != Long.MAX_VALUE }.sortedByDescending { it.fetchedAt }.drop(limit).forEach { rows.remove(it.key) }
        changes.value++
    }
}

fun createResponseTestCache(
    dao: ResponseCacheDao = InMemoryResponseCacheDao(),
    scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined),
    nowMillis: () -> Long = System::currentTimeMillis,
    nanoTime: () -> Long = System::nanoTime,
    foreground: Flow<Boolean> = emptyFlow()
): ResponseCache {
    val gson = Gson()
    val logger = mock(Logger::class.java)
    val flightScope = CoroutineScope(scope.coroutineContext + SupervisorJob(scope.coroutineContext[kotlinx.coroutines.Job]))
    return ResponseCache(dao, gson, flightScope, logger, foreground, nowMillis, nanoTime)
}

fun createBittensorTestSources(
    urls: InfrastructureUrls = InfrastructureUrls { "https://infra.test/".toHttpUrl() },
    nanoTime: () -> Long = System::nanoTime,
    nowMillis: () -> Long = System::currentTimeMillis,
    dao: ResponseCacheDao = InMemoryResponseCacheDao(),
    scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined),
    validatorsApi: BittensorValidatorsApi? = null,
    catalogueApi: BittensorSubnetCatalogueApi? = null,
    estimatesApi: BittensorEstimatesApi? = null,
    portfolioHistoryApi: BittensorPortfolioHistoryApi? = null,
    readExpiredCache: Boolean = true,
    foreground: Flow<Boolean> = emptyFlow()
): BittensorTestSources {
    val responses = createResponseTestCache(dao, scope, nowMillis, nanoTime, foreground)
    val validators = validatorsApi ?: mock(BittensorValidatorsApi::class.java)
    val catalogue = catalogueApi ?: mock(BittensorSubnetCatalogueApi::class.java)
    val estimates = estimatesApi ?: mock(BittensorEstimatesApi::class.java)
    val history = portfolioHistoryApi ?: mock(BittensorPortfolioHistoryApi::class.java)
    val client = BittensorBackendClient(responses, urls)
    val validatorSource = BittensorValidatorSource(client, validators, readExpiredCache)
    val catalogueSource = BittensorSubnetCatalogueSource(client, catalogue)
    val yieldSource = BittensorAlphaYieldSource(client, estimates)
    val portfolioSource = BittensorPortfolioHistorySource(client, history)
    val computations = mock(ComputationalCache::class.java)
    val preferences = mock(Preferences::class.java)
    val gson = Gson()
    val operations = BittensorOperationHistorySource(client, estimates, portfolioSource, urls, computations, preferences, gson, spacingMillis = 0L)
    return BittensorTestSources(client, urls, estimates, validatorSource, catalogueSource, yieldSource, portfolioSource, operations)
}

class BittensorTestSources(
    val client: BittensorBackendClient,
    private val urls: InfrastructureUrls,
    private val api: BittensorEstimatesApi,
    val validators: BittensorValidatorSource,
    val catalogue: BittensorSubnetCatalogueSource,
    val yields: BittensorAlphaYieldSource,
    val portfolio: BittensorPortfolioHistorySource,
    val operations: BittensorOperationHistorySource
) {

    suspend fun operationHistory(base: HttpUrl, address: String, page: Int, metaAccountId: Long) = client.getWithLifetime(
        url = operationsUrl(base, address, page),
        type = BittensorOperationsRemote::class.java,
        metaAccountId = metaAccountId,
        fetch = api::operations
    )

    suspend fun cachedOperationHistory(address: String, page: Int, metaAccountId: Long) = client.peekWithLifetime(
        url = operationsUrl(requireNotNull(urls.baseUrl()), address, page),
        type = BittensorOperationsRemote::class.java,
        metaAccountId = metaAccountId
    )

    private fun operationsUrl(base: HttpUrl, address: String, page: Int): String =
        base.newBuilder().addPathSegments("v1/bittensor/operations/search")
            .addQueryParameter("accountSubject", address)
            .addQueryParameter("page", page.toString())
            .build().toString()

    fun historySource(
        computations: ComputationalCache,
        preferences: Preferences,
        spacingMillis: Long = 0L,
        nowNanos: () -> Long = System::nanoTime,
        nowMillis: () -> Long = System::currentTimeMillis
    ): BittensorOperationHistorySource {
        val gson = Gson()
        return BittensorOperationHistorySource(client, api, portfolio, urls, computations, preferences, gson, spacingMillis, nowNanos, nowMillis)
    }
}

// Existing contract suites use named fixture factories while production receives the shared feature cache through DI.
fun BittensorValidatorSource(
    api: BittensorValidatorsApi,
    infrastructureUrls: InfrastructureUrls,
    nanoTime: () -> Long = System::nanoTime
): BittensorValidatorSource {
    val backendSource = createBittensorTestSources(infrastructureUrls, nanoTime, validatorsApi = api, readExpiredCache = false)
    return backendSource.validators
}

fun BittensorSubnetCatalogueSource(
    api: BittensorSubnetCatalogueApi,
    infrastructureUrls: InfrastructureUrls,
    nanoTime: () -> Long = System::nanoTime
): BittensorSubnetCatalogueSource {
    val backendSource = createBittensorTestSources(infrastructureUrls, nanoTime, catalogueApi = api)
    return backendSource.catalogue
}

fun createBittensorTestFxSource(
    prices: io.novafoundation.nova.feature_wallet_api.data.repository.CoinPriceRepository,
    cache: ResponseCache = createResponseTestCache()
): BittensorPortfolioFxSource {
    val computations = UnsharedBittensorComputations()
    val logger = mock(Logger::class.java)
    return BittensorPortfolioFxSource(prices, cache, computations, logger)
}

private class UnsharedBittensorComputations : ComputationalCache {
    override suspend fun <T> useCache(key: String, scope: CoroutineScope, computation: suspend CoroutineScope.() -> T): T = computation(scope)

    override fun <T> useSharedFlow(key: String, scope: CoroutineScope, flowLazy: suspend CoroutineScope.() -> kotlinx.coroutines.flow.Flow<T>) =
        kotlinx.coroutines.flow.flow { emitAll(flowLazy(scope)) }
}
