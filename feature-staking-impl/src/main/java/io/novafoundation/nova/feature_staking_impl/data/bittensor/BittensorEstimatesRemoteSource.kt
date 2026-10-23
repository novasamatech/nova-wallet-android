package io.novafoundation.nova.feature_staking_impl.data.bittensor

import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorNetuid
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorReportedOperation
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.CacheControl
import okhttp3.HttpUrl
import retrofit2.HttpException
import retrofit2.Response

class BittensorEstimatesRemoteSource(
    private val api: BittensorEstimatesApi,
    private val spacingMillis: Long
) {

    suspend fun history(baseUrl: HttpUrl, address: String): EstimatePages<BittensorReportedOperation> =
        loadPages(HISTORY_PAGE_LIMIT, spacingMillis) { page -> loadHistoryPage(baseUrl, address, page) }

    /** Alpha yields arrive as one unpaginated collection per subnet, so there is nothing to page through. */
    internal suspend fun yields(baseUrl: HttpUrl, chain: Chain, subnet: BittensorNetuid.Subnet): EstimatePages<BittensorValidatorYield> {
        val snapshot = loadYieldSnapshot(baseUrl, chain, subnet)
        return EstimatePages(snapshot.items, snapshot.validUntilNanos, allPagesRead = true)
    }

    private suspend fun loadHistoryPage(baseUrl: HttpUrl, address: String, page: Int): EstimatePage<BittensorReportedOperation> {
        val url = endpoint(baseUrl, "operations/search")
        val requestStartedAtNanos = System.nanoTime()
        val response = withTimeoutOrNull(REQUEST_TIMEOUT_MILLIS) { api.operations(url, address, page) }
            ?: error("Operation history page timed out")
        val validUntilNanos = deadline(response, requestStartedAtNanos)
        val body = requireBody(response)
        requireFreshComponent(body.meta?.components?.operationHistory)
        require(body.historyScope == "TAO_APP_PARTIAL")
        val remoteItems = requireNotNull(body.items)
        validateHistoryPage(body.pageInfo, page, remoteItems.size)
        val items = remoteItems
            .map(::mapBittensorReportedOperation)
        return EstimatePage(items, nextPage(body.pageInfo, page), validUntilNanos)
    }

    private suspend fun loadYieldSnapshot(
        baseUrl: HttpUrl,
        chain: Chain,
        subnetId: BittensorNetuid.Subnet
    ): EstimatePage<BittensorValidatorYield> {
        val url = endpoint(baseUrl, "subnets/${subnetId.value}/yields/alpha")
        val requestStartedAtNanos = System.nanoTime()
        val response = withTimeoutOrNull(REQUEST_TIMEOUT_MILLIS) { api.alphaYields(url) }
            ?: error("Validator yield snapshot timed out")
        val validUntilNanos = deadline(response, requestStartedAtNanos)
        val body = requireBody(response)
        val component = body.meta?.components?.alphaYield
        require(component?.availability == "AVAILABLE" && component.freshness in setOf("FRESH", "STALE")) {
            "Yield source is unavailable"
        }
        val items = requireNotNull(body.items).mapNotNull { mapBittensorValidatorYield(it, chain, subnetId) }
        return EstimatePage(items, null, validUntilNanos)
    }

    private suspend fun <T> loadPages(
        pageLimit: Int,
        spacingMillis: Long,
        loadPage: suspend (Int) -> EstimatePage<T>
    ): EstimatePages<T> {
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

    private fun deadline(response: Response<*>, requestStartedAtNanos: Long): Long {
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
        return requestStartedAtNanos + seconds * NANOS_PER_SECOND
    }

    private fun requireFreshComponent(component: BittensorRecommendationsRemote.Component?) {
        require(component?.availability == "AVAILABLE" && component.freshness == "FRESH") { "Estimate source is unavailable or stale" }
    }

    private fun nextPage(info: BittensorHistoryPageInfo?, current: Int): Int? {
        val value = requireNotNull(info).nextPage
        require(value == null || value == current + 1 && value <= MAX_PROVIDER_PAGE)
        return value
    }

    private fun validateHistoryPage(info: BittensorHistoryPageInfo?, requestedPage: Int, itemCount: Int) {
        requireNotNull(info) { "Missing operation history pagination" }
        require(info.page == requestedPage && requestedPage in FIRST_PAGE..MAX_PROVIDER_PAGE) { "Unexpected operation history page" }
        require(info.pageSize == PAGE_SIZE) { "Unexpected operation history page size" }
        require(itemCount <= PAGE_SIZE) { "Operation history page exceeds its declared size" }
    }

    private fun <T> requireBody(response: Response<T>): T {
        if (!response.isSuccessful) throw HttpException(response)
        return requireNotNull(response.body())
    }

    private fun endpoint(base: HttpUrl, path: String): String {
        require(base.isHttps)
        return requireNotNull(base.resolve("/v1/bittensor/$path")).toString()
    }

    data class EstimatePage<T>(val items: List<T>, val nextPage: Int?, val validUntilNanos: Long)

    data class EstimatePages<T>(val items: List<T>, val validUntilNanos: Long, val allPagesRead: Boolean)

    private companion object {
        const val FIRST_PAGE = 1
        const val PAGE_SIZE = 100
        const val HISTORY_PAGE_LIMIT = 10
        const val MAX_PROVIDER_PAGE = 100
        const val REQUEST_TIMEOUT_MILLIS = 25_000L
        const val MAX_CACHE_AGE_SECONDS = 60L
        const val NANOS_PER_SECOND = 1_000_000_000L
    }
}
