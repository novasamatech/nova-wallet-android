package io.novafoundation.nova.feature_staking_impl.data.bittensor

import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onStart
import io.novafoundation.nova.common.utils.isNonNegative
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.WalletRepository
import io.novafoundation.nova.runtime.ext.utilityAsset
import kotlinx.coroutines.flow.map
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorEstimateContext
import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorEstimatedCostBasis
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorReportedOperation
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorNetuid
import io.novafoundation.nova.runtime.ext.accountIdOf
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import okhttp3.CacheControl
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.domain.model.requireAccountIdIn
import io.novafoundation.nova.runtime.ext.addressOf
import retrofit2.Response
import retrofit2.HttpException
import io.novasama.substrate_sdk_android.wsrpc.logging.Logger
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import io.novafoundation.nova.common.domain.ExtendedLoadingState
import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.common.utils.withSafeLoading
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout
import java.math.BigDecimal

class BittensorEstimatesRepository(
    private val api: BittensorEstimatesApi,
    private val infrastructureUrls: io.novafoundation.nova.infrastructure.InfrastructureUrls,
    private val spacingMillis: Long,
    private val accounts: AccountRepository,
    private val wallets: WalletRepository,
    private val logger: Logger
) {
    private data class Cached<T>(val value: T, val until: Long, val revision: java.math.BigInteger? = null)

    private val historyMutex = Mutex()

    private val histories = mutableMapOf<String, Cached<BittensorEstimatedCostBasis>>()

    private val yieldMutex = Mutex()

    private val yields = mutableMapOf<String, Cached<Map<AccountIdKey, BigDecimal>>>()

    fun observeCostBasis(
        context: Flow<BittensorEstimateContext?>,
        retry: Flow<Long> = kotlinx.coroutines.flow.flowOf(0L)
    ): Flow<ExtendedLoadingState<BittensorEstimatedCostBasis?>> =
        combine(context.distinctUntilChanged(), retry) { context, _ -> context }
            .flatMapLatest(::costBasisFlow)

    private fun costBasisFlow(context: BittensorEstimateContext?): Flow<ExtendedLoadingState<BittensorEstimatedCostBasis?>> {
        val balance = context?.let { wallets.assetFlow(it.metaId, it.chain.utilityAsset).map { it.transferableInPlanks } }
            ?: kotlinx.coroutines.flow.flowOf(null)
        return balance.distinctUntilChanged()
            .flatMapLatest { revision ->
                flowOf { context?.let { costBasis(it.metaId, it.chain, revision) } }
                    .withSafeLoading()
                    .onEach { state ->
                        if (state is ExtendedLoadingState.Error) reportUnavailable(state.exception)
                    }
            }
    }

    fun observeAlphaYields(
        context: Flow<Chain?>,
        subnetId: BittensorNetuid.Subnet,
        retry: Flow<Long> = kotlinx.coroutines.flow.flowOf(0L)
    ): Flow<Map<AccountIdKey, BigDecimal>?> =
        combine(context.distinctUntilChanged(), retry) { chain, _ -> chain }
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

    suspend fun costBasis(metaId: Long, chain: Chain, revision: java.math.BigInteger? = null): BittensorEstimatedCostBasis {
        val account = accounts.getMetaAccount(metaId)
        return costBasis(chain.addressOf(account.requireAccountIdIn(chain)), revision)
    }

    private suspend fun costBasis(account: String, revision: java.math.BigInteger?): BittensorEstimatedCostBasis = historyMutex.withLock {
        val baseUrl = requireNotNull(infrastructureUrls.baseUrl())
        val cacheKey = "$baseUrl:$account"
        histories[cacheKey]?.takeIf { System.nanoTime() < it.until && revision == it.revision }?.let { return@withLock it.value }
        var validUntil = Long.MAX_VALUE
        val rows = mutableListOf<BittensorReportedOperation>()
        var page: Int? = 1
        repeat(10) {
            val current = page ?: return@repeat
            if (it > 0) delay(spacingMillis)
            val response = withTimeout(25_000) {
                api.operations(
                    url(baseUrl, "operations/search", mapOf("accountSubject" to account, "page" to current.toString()))
                )
            }
            validUntil = minOf(validUntil, deadline(response))
            val body = body(response)
            fresh(body.meta?.components?.operationHistory)
            require(body.historyScope == "TAO_APP_PARTIAL" && body.pageInfo?.page == current)
            rows += requireNotNull(body.items).map { row ->
                val netuid = requireNotNull(row.netuid)
                require(netuid in 0..65535)
                BittensorReportedOperation(
                    netuid, requireNotNull(row.hotkey), requireNotNull(row.reportedAmountIn), requireNotNull(row.reportedAmountOut),
                    requireNotNull(row.reportedPrice), requireNotNull(row.sourceEventId), requireNotNull(row.sourceTimestamp),
                    requireNotNull(row.sourceExtrinsicReference), row.sourceOperationType
                )
            }
            page = next(body.pageInfo, current)
        }
        val result = BittensorEstimatedCostBasis(rows, allProviderPagesRead = page == null)
        if (histories.size >= 8) histories.clear()
        histories[cacheKey] = Cached(result, validUntil, revision)
        result
    }

    /** Like iOS, reportedRate is interpreted as annual percent; no validator take is subtracted again. */
    suspend fun alphaYields(chain: Chain, subnet: BittensorNetuid.Subnet): Map<AccountIdKey, BigDecimal> = yieldMutex.withLock {
        val baseUrl = requireNotNull(infrastructureUrls.baseUrl())
        val cacheKey = "$baseUrl:${subnet.value}"
        yields[cacheKey]?.takeIf { System.nanoTime() < it.until }?.let { return@withLock it.value }
        var validUntil = Long.MAX_VALUE
        val result = mutableMapOf<AccountIdKey, BigDecimal>()
        var page: Int? = 1
        repeat(3) {
            val current = page ?: return@repeat
            if (it > 0) delay(spacingMillis)
            val response = withTimeout(25_000) { api.alphaYields(url(baseUrl, "subnets/${subnet.value}/yields/alpha", mapOf("page" to current.toString()))) }
            validUntil = minOf(validUntil, deadline(response))
            val body = body(response)
            fresh(body.meta?.components?.alphaYield)
            require(body.pageInfo?.page == current)
            requireNotNull(body.items).forEach { row ->
                require(row.netuid == subnet.value && row.metricKind == "ALPHA_VALIDATOR_APY")
                val key = row.hotkey?.let { runCatching { AccountIdKey(chain.accountIdOf(it)) }.getOrNull() } ?: return@forEach
                val percent = row.reportedRate?.toBigDecimalOrNull()?.takeIf { it.isNonNegative } ?: return@forEach
                if (key !in result) result[key] = percent
            }
            page = next(body.pageInfo, current)
        }
        if (yields.size >= 64) yields.clear()
        yields[cacheKey] = Cached(result, validUntil)
        result
    }

    private fun deadline(response: Response<*>): Long {
        val cache = CacheControl.parse(response.headers())
        val age = response.headers()["Age"]?.toLongOrNull()?.coerceAtLeast(0) ?: 0L
        val seconds = if (cache.noStore || cache.noCache) 0L else (cache.maxAgeSeconds.toLong() - age).coerceIn(0, 60)
        return System.nanoTime() + seconds * 1_000_000_000L
    }

    private fun fresh(component: BittensorRecommendationsRemote.Component?) {
        require(component?.availability == "AVAILABLE" && component.freshness == "FRESH") { "Estimate source is unavailable or stale" }
    }

    private fun next(info: BittensorHistoryPageInfo?, current: Int): Int? {
        val value = requireNotNull(info).nextPage
        require(value == null || value in (current + 1)..100)
        return value
    }

    private fun <T> body(response: Response<T>): T {
        if (!response.isSuccessful) throw HttpException(response)
        return requireNotNull(response.body())
    }

    private fun url(base: okhttp3.HttpUrl, path: String, query: Map<String, String>): String {
        require(base.isHttps)
        val target = requireNotNull(base.resolve("/v1/bittensor/$path")).newBuilder()
        query.forEach { (key, value) -> target.addQueryParameter(key, value) }
        return target.build().toString()
    }
}
