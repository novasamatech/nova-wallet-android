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
        loadPages(
            pageLimit = HISTORY_PAGE_LIMIT,
            spacingMillis
        ) { page -> loadHistoryPage(baseUrl, address, page) }

    /** Alpha yields arrive as one unpaginated collection per subnet, so there is nothing to page through. */
    internal suspend fun yields(baseUrl: HttpUrl, chain: Chain, subnet: BittensorNetuid.Subnet): EstimatePages<BittensorValidatorYield> {
        val snapshot = loadYieldSnapshot(baseUrl, chain, subnet)
        return EstimatePages(snapshot.items, snapshot.validUntilNanos, allPagesRead = true)
    }

    private suspend fun loadHistoryPage(baseUrl: HttpUrl, address: String, page: Int): EstimatePage<BittensorReportedOperation> {
        val url = endpoint(baseUrl, path = "operations/search")
        val requestStartedAtNanos = System.nanoTime()
        val response = requestHistoryPage(url, address, page)
        val validUntilNanos = deadline(response, requestStartedAtNanos)

        val remote = requireBody(response)
        return mapHistoryPage(remote, page, validUntilNanos)
    }

    private suspend fun loadYieldSnapshot(
        baseUrl: HttpUrl,
        chain: Chain,
        subnetId: BittensorNetuid.Subnet
    ): EstimatePage<BittensorValidatorYield> {
        val url = endpoint(baseUrl, path = "subnets/${subnetId.value}/yields/alpha")
        val requestStartedAtNanos = System.nanoTime()
        val response = requestYieldSnapshot(url)
        val validUntilNanos = deadline(response, requestStartedAtNanos)

        val remote = requireBody(response)
        return mapYieldSnapshot(remote, chain, subnetId, validUntilNanos)
    }

    private suspend fun requestHistoryPage(url: String, address: String, page: Int): Response<BittensorOperationsRemote> =
        withTimeoutOrNull(REQUEST_TIMEOUT_MILLIS) { api.operations(url, address, page) }
            ?: throw BittensorTimeoutException(operation = "Operation history page")

    private suspend fun requestYieldSnapshot(url: String): Response<BittensorAlphaYieldsRemote> =
        withTimeoutOrNull(REQUEST_TIMEOUT_MILLIS) { api.alphaYields(url) }
            ?: throw BittensorTimeoutException(operation = "Validator yield snapshot")

    private fun mapHistoryPage(
        remote: BittensorOperationsRemote,
        requestedPage: Int,
        validUntilNanos: Long
    ): EstimatePage<BittensorReportedOperation> {
        requireAvailableBittensorComponent(remote.meta?.components?.operationHistory, name = "Operation history")
        require(remote.historyScope == "TAO_APP_PARTIAL")
        val remoteItems = requireNotNull(remote.items)
        val nextPage = requireHistoryNextPage(remote.pageInfo, requestedPage, remoteItems.size)
        val items = remoteItems.map(::mapBittensorReportedOperation)
        return EstimatePage(items, nextPage, validUntilNanos)
    }

    private fun mapYieldSnapshot(
        remote: BittensorAlphaYieldsRemote,
        chain: Chain,
        subnetId: BittensorNetuid.Subnet,
        validUntilNanos: Long
    ): EstimatePage<BittensorValidatorYield> {
        requireAvailableBittensorComponent(remote.meta?.components?.alphaYield, name = "Yield source")
        val remoteItems = requireNotNull(remote.items)
        val items = remoteItems.mapNotNull { mapBittensorValidatorYield(it, chain, subnetId) }
        return EstimatePage(items, nextPage = null, validUntilNanos)
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
        val cacheControl = CacheControl.parse(response.headers())
        val ageSeconds = response.headers()["Age"]
            ?.toLongOrNull()
            ?.coerceAtLeast(0) ?: 0L
        val lifetimeSeconds = if (cacheControl.noStore || cacheControl.noCache) {
            0L
        } else {
            (cacheControl.maxAgeSeconds.toLong() - ageSeconds)
                .coerceIn(0, MAX_CACHE_AGE_SECONDS)
        }
        return requestStartedAtNanos + lifetimeSeconds * NANOS_PER_SECOND
    }

    private fun requireHistoryNextPage(pageInfo: BittensorHistoryPageInfo?, requestedPage: Int, itemCount: Int): Int? {
        requireNotNull(pageInfo) { "Missing operation history pagination" }
        require(pageInfo.page == requestedPage && requestedPage in FIRST_PAGE..MAX_PROVIDER_PAGE) { "Unexpected operation history page" }
        require(pageInfo.pageSize == PAGE_SIZE) { "Unexpected operation history page size" }
        require(itemCount <= PAGE_SIZE) { "Operation history page exceeds its declared size" }
        val nextPage = pageInfo.nextPage
        require(nextPage == null || nextPage == requestedPage + 1 && nextPage <= MAX_PROVIDER_PAGE) { "Unexpected operation history next page" }
        return nextPage
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
