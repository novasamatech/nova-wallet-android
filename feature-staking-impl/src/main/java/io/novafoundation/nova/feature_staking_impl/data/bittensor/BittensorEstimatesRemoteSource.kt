package io.novafoundation.nova.feature_staking_impl.data.bittensor

import kotlinx.coroutines.flow.Flow

import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorNetuid
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorReportedOperation
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import kotlinx.coroutines.delay
import okhttp3.HttpUrl

class BittensorEstimatesRemoteSource(
    private val api: BittensorEstimatesApi,
    private val spacingMillis: Long,
    private val cache: BittensorResponseCache
) {

    suspend fun history(baseUrl: HttpUrl, address: String, persist: Boolean = true): EstimatePages<BittensorReportedOperation> =
        loadPages(HISTORY_PAGE_LIMIT, spacingMillis) { page -> loadHistoryPage(baseUrl, address, page, persist) }

    suspend fun invalidateHistory(baseUrl: HttpUrl, address: String) {
        cache.invalidate(baseUrl, address, listOf("operations/search", "portfolio/history/search"))
    }

    suspend fun cachedHistory(address: String): EstimatePages<BittensorReportedOperation>? {
        val items = mutableListOf<BittensorReportedOperation>()
        var page: Int? = FIRST_PAGE
        var deadline = Long.MAX_VALUE
        var count = 0
        while (page != null && count < HISTORY_PAGE_LIMIT) {
            val current = page
            val snapshot = cache.peek("operations/search", BittensorOperationsRemote::class.java, address, "page=$current") {
                validateHistory(it, current)
            } ?: return null
            items += requireNotNull(snapshot.value.items).map(::mapBittensorReportedOperation)
            deadline = minOf(deadline, snapshot.freshUntilNanos)
            page = nextPage(snapshot.value.pageInfo, current)
            count += 1
        }
        return EstimatePages(items, deadline, allPagesRead = page == null)
    }

    val foregroundRefreshes get() = cache.foregroundRefreshes

    fun observeYields(chain: Chain, subnet: BittensorNetuid.Subnet): Flow<BittensorCachedResponse<BittensorAlphaYieldsRemote>> = cache.observe(
        path = "subnets/${subnet.value}/yields/alpha",
        type = BittensorAlphaYieldsRemote::class.java,
        validate = { body ->
            requireAvailableBittensorComponent(body.meta?.components?.alphaYield, "Yield source")
            requireNotNull(body.items).forEach { mapBittensorValidatorYield(it, chain, subnet) }
        },
        fetch = api::alphaYields
    )

    /** Alpha yields arrive as one unpaginated collection per subnet, so there is nothing to page through. */
    internal suspend fun yields(baseUrl: HttpUrl, chain: Chain, subnet: BittensorNetuid.Subnet): EstimatePages<BittensorValidatorYield> {
        val snapshot = loadYieldSnapshot(baseUrl, chain, subnet)
        return EstimatePages(snapshot.items, snapshot.validUntilNanos, allPagesRead = true)
    }

    private suspend fun loadHistoryPage(baseUrl: HttpUrl, address: String, page: Int, persist: Boolean): EstimatePage<BittensorReportedOperation> {
        val snapshot = cache.get(
            path = "operations/search",
            type = BittensorOperationsRemote::class.java,
            account = address,
            query = "page=$page",
            expectedBase = baseUrl,
            // During indexer settlement every read reaches the backend and cannot seed the next subscriber.
            force = !persist,
            persist = persist,
            validate = { validateHistory(it, page) },
            fetch = { url -> api.operations(url, address, page) }
        )
        val body = snapshot.value
        return EstimatePage(requireNotNull(body.items).map(::mapBittensorReportedOperation), nextPage(body.pageInfo, page), deadline(snapshot))
    }

    private fun validateHistory(body: BittensorOperationsRemote, page: Int) {
        requireAvailableBittensorComponent(body.meta?.components?.operationHistory, "Operation history")
        require(body.historyScope == "TAO_APP_PARTIAL")
        val items = requireNotNull(body.items)
        validateHistoryPage(body.pageInfo, page, items.size)
        nextPage(body.pageInfo, page)
        items.forEach { mapBittensorReportedOperation(it) }
    }

    private suspend fun loadYieldSnapshot(
        baseUrl: HttpUrl,
        chain: Chain,
        subnetId: BittensorNetuid.Subnet
    ): EstimatePage<BittensorValidatorYield> {
        val snapshot = cache.get(
            path = "subnets/${subnetId.value}/yields/alpha",
            type = BittensorAlphaYieldsRemote::class.java,
            allowStale = true,
            expectedBase = baseUrl,
            validate = { body ->
                requireAvailableBittensorComponent(body.meta?.components?.alphaYield, "Yield source")
                requireNotNull(body.items).forEach { mapBittensorValidatorYield(it, chain, subnetId) }
            },
            fetch = api::alphaYields
        )
        val items = requireNotNull(snapshot.value.items).mapNotNull { mapBittensorValidatorYield(it, chain, subnetId) }
        return EstimatePage(items, null, deadline(snapshot))
    }

    private fun deadline(snapshot: BittensorCachedResponse<*>): Long = snapshot.freshUntilNanos

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

    data class EstimatePage<T>(val items: List<T>, val nextPage: Int?, val validUntilNanos: Long)

    data class EstimatePages<T>(val items: List<T>, val validUntilNanos: Long, val allPagesRead: Boolean)

    private companion object {
        const val FIRST_PAGE = 1
        const val PAGE_SIZE = 100
        const val HISTORY_PAGE_LIMIT = 10
        const val MAX_PROVIDER_PAGE = 100
    }
}
