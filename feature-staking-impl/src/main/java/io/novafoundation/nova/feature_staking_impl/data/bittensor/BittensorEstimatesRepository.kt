package io.novafoundation.nova.feature_staking_impl.data.bittensor

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.common.domain.ExtendedLoadingState
import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.common.utils.withSafeLoading
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.domain.model.requireAccountIdIn
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorEstimateContext
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorEstimatedCostBasis
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorNetuid
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorReportedOperation
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.WalletRepository
import io.novafoundation.nova.infrastructure.InfrastructureUrls
import io.novafoundation.nova.runtime.ext.addressOf
import io.novafoundation.nova.runtime.ext.utilityAsset
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novasama.substrate_sdk_android.wsrpc.logging.Logger
import java.math.BigDecimal
import java.math.BigInteger
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout
import okhttp3.CacheControl
import okhttp3.HttpUrl
import retrofit2.HttpException
import retrofit2.Response

class BittensorEstimatesRepository(
    private val api: BittensorEstimatesApi,
    private val infrastructureUrls: InfrastructureUrls,
    private val spacingMillis: Long,
    private val accountRepository: AccountRepository,
    private val walletRepository: WalletRepository,
    private val logger: Logger
) {

    private data class Cached<T>(val value: T, val validUntilNanos: Long, val balanceRevision: BigInteger? = null) {

        fun isFresh(): Boolean = System.nanoTime() < validUntilNanos
    }

    private val historyMutex = Mutex()

    private val histories = mutableMapOf<String, Cached<BittensorEstimatedCostBasis>>()

    private val yieldMutex = Mutex()

    private val yields = mutableMapOf<String, Cached<Map<AccountIdKey, BigDecimal>>>()

    fun observeCostBasis(
        context: Flow<BittensorEstimateContext?>,
        retry: Flow<Long> = kotlinx.coroutines.flow.flowOf(0L)
    ): Flow<ExtendedLoadingState<BittensorEstimatedCostBasis?>> =
        combine(
            context.distinctUntilChanged(),
            retry
        ) { context, _ -> context }
            .flatMapLatest(::observeContextCostBasis)

    private fun observeContextCostBasis(context: BittensorEstimateContext?): Flow<ExtendedLoadingState<BittensorEstimatedCostBasis?>> {
        return observeBalanceRevision(context)
            .distinctUntilChanged().flatMapLatest { revision ->
                flowOf { context?.let { costBasis(it.metaId, it.chain, revision) } }
                    .withSafeLoading().onEach { state ->
                        if (state is ExtendedLoadingState.Error) reportUnavailable(state.exception)
                    }
            }
    }

    private fun observeBalanceRevision(context: BittensorEstimateContext?): Flow<BigInteger?> {
        if (context == null) return kotlinx.coroutines.flow.flowOf(null)

        return walletRepository.assetFlow(context.metaId, context.chain.utilityAsset).map { it.transferableInPlanks }
    }

    fun observeAlphaYields(
        context: Flow<Chain?>,
        subnetId: BittensorNetuid.Subnet,
        retry: Flow<Long> = kotlinx.coroutines.flow.flowOf(0L)
    ): Flow<Map<AccountIdKey, BigDecimal>?> =
        combine(
            context.distinctUntilChanged(),
            retry
        ) { chain, _ -> chain }
            .flatMapLatest { chain ->
                flowOf { chain?.let { alphaYields(it, subnetId) } }
                    .onStart { emit(null) }
                    .catch { cause ->
                        reportUnavailable(cause)
                        emit(null)
                    }
            }

    private fun reportUnavailable(cause: Throwable) {
        logger.log("[BittensorEstimates] Optional estimate unavailable")
        logger.log(cause)
    }

    suspend fun costBasis(metaId: Long, chain: Chain, revision: BigInteger? = null): BittensorEstimatedCostBasis {
        val account = accountRepository.getMetaAccount(metaId)
        return costBasis(chain.addressOf(account.requireAccountIdIn(chain)), revision)
    }

    private suspend fun costBasis(account: String, revision: BigInteger?): BittensorEstimatedCostBasis = historyMutex.withLock {
        val baseUrl = requireNotNull(infrastructureUrls.baseUrl())
        val cacheKey = "$baseUrl:$account"
        val cached = histories[cacheKey]
        if (cached != null && cached.isFresh() && revision == cached.balanceRevision) return@withLock cached.value
        val history = loadPages(HISTORY_PAGE_LIMIT) { page -> loadHistoryPage(baseUrl, account, page) }
        val result = BittensorEstimatedCostBasis(history.items, allProviderPagesRead = history.allPagesRead)
        if (histories.size >= HISTORY_CACHE_LIMIT) histories.clear()
        histories[cacheKey] = Cached(result, history.validUntilNanos, revision)
        result
    }

    /** Like iOS, reportedRate is interpreted as annual percent; no validator take is subtracted again. */
    suspend fun alphaYields(chain: Chain, subnet: BittensorNetuid.Subnet): Map<AccountIdKey, BigDecimal> = yieldMutex.withLock {
        val baseUrl = requireNotNull(infrastructureUrls.baseUrl())
        val cacheKey = "$baseUrl:${subnet.value}"
        val cached = yields[cacheKey]
        if (cached != null && cached.isFresh()) return@withLock cached.value
        val pages = loadPages(YIELD_PAGE_LIMIT) { page ->
            loadYieldPage(
                baseUrl,
                chain,
                subnet,
                page
            )
        }
        val result = buildMap {
            pages.items.forEach { yield ->
                if (yield.hotkey !in this) put(yield.hotkey, yield.annualPercentageYield)
            }
        }
        if (yields.size >= YIELD_CACHE_LIMIT) yields.clear()
        yields[cacheKey] = Cached(result, pages.validUntilNanos)
        result
    }

    private suspend fun loadHistoryPage(baseUrl: HttpUrl, address: String, page: Int): EstimatePage<BittensorReportedOperation> {
        val url = endpoint(baseUrl, "operations/search", mapOf("accountSubject" to address, "page" to page.toString()))
        val response = withTimeout(REQUEST_TIMEOUT_MILLIS) { api.operations(url) }
        val validUntilNanos = deadline(response)
        val body = requireBody(response)
        requireFreshComponent(body.meta?.components?.operationHistory)
        require(body.historyScope == "TAO_APP_PARTIAL" && body.pageInfo?.page == page)
        val items = requireNotNull(body.items)
            .map(::mapBittensorReportedOperation)
        return EstimatePage(items, nextPage(body.pageInfo, page), validUntilNanos)
    }

    private suspend fun loadYieldPage(
        baseUrl: HttpUrl,
        chain: Chain,
        subnetId: BittensorNetuid.Subnet,
        page: Int
    ): EstimatePage<BittensorValidatorYield> {
        val url = endpoint(baseUrl, "subnets/${subnetId.value}/yields/alpha", mapOf("page" to page.toString()))
        val response = withTimeout(REQUEST_TIMEOUT_MILLIS) { api.alphaYields(url) }
        val validUntilNanos = deadline(response)
        val body = requireBody(response)
        requireFreshComponent(body.meta?.components?.alphaYield)
        require(body.pageInfo?.page == page)
        val items = requireNotNull(body.items).mapNotNull { mapBittensorValidatorYield(it, chain, subnetId) }
        return EstimatePage(items, nextPage(body.pageInfo, page), validUntilNanos)
    }

    private suspend fun <T> loadPages(pageLimit: Int, loadPage: suspend (Int) -> EstimatePage<T>): EstimatePages<T> {
        val items = mutableListOf<T>()
        var nextPage: Int? = FIRST_PAGE
        var validUntilNanos = Long.MAX_VALUE
        var pagesRead = 0

        while (nextPage != null && pagesRead < pageLimit) {
            if (pagesRead > 0) delay(spacingMillis)
            val page = loadPage(nextPage)
            items += page.items
            validUntilNanos = minOf(validUntilNanos, page.validUntilNanos)
            nextPage = page.nextPage
            pagesRead += 1
        }

        return EstimatePages(items, validUntilNanos, allPagesRead = nextPage == null)
    }

    private fun deadline(response: Response<*>): Long {
        val cache = CacheControl.parse(response.headers())
        val age = response.headers()["Age"]
            ?.toLongOrNull()
            ?.coerceAtLeast(0) ?: 0L
        val seconds = if (cache.noStore || cache.noCache) {
            0L
        } else {
            (cache.maxAgeSeconds.toLong() - age)
                .coerceIn(0, MAX_CACHE_AGE_SECONDS)
        }
        return System.nanoTime() + seconds * NANOS_PER_SECOND
    }

    private fun requireFreshComponent(component: BittensorRecommendationsRemote.Component?) {
        require(component?.availability == "AVAILABLE" && component.freshness == "FRESH") { "Estimate source is unavailable or stale" }
    }

    private fun nextPage(info: BittensorHistoryPageInfo?, current: Int): Int? {
        val value = requireNotNull(info).nextPage
        require(value == null || value in (current + 1)..MAX_PROVIDER_PAGE)
        return value
    }

    private fun <T> requireBody(response: Response<T>): T {
        if (!response.isSuccessful) throw HttpException(response)
        return requireNotNull(response.body())
    }

    private fun endpoint(base: HttpUrl, path: String, query: Map<String, String>): String {
        require(base.isHttps)
        val target = requireNotNull(base.resolve("/v1/bittensor/$path"))
            .newBuilder()
        query.forEach { (key, value) -> target.addQueryParameter(key, value) }
        return target.build().toString()
    }

    private data class EstimatePage<T>(val items: List<T>, val nextPage: Int?, val validUntilNanos: Long)

    private data class EstimatePages<T>(val items: List<T>, val validUntilNanos: Long, val allPagesRead: Boolean)

    private companion object {

        const val FIRST_PAGE = 1

        const val HISTORY_PAGE_LIMIT = 10

        const val YIELD_PAGE_LIMIT = 3

        const val MAX_PROVIDER_PAGE = 100

        const val HISTORY_CACHE_LIMIT = 8

        const val YIELD_CACHE_LIMIT = 64

        const val REQUEST_TIMEOUT_MILLIS = 25_000L

        const val MAX_CACHE_AGE_SECONDS = 60L

        const val NANOS_PER_SECOND = 1_000_000_000L
    }
}
