package io.novafoundation.nova.feature_staking_impl.data.bittensor

import io.novafoundation.nova.core_db.dao.ResponseCacheDao
import io.novafoundation.nova.core_db.model.ResponseCacheKey
import kotlinx.coroutines.CancellationException
import io.novafoundation.nova.core_db.model.ResponseCacheLocal
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorNetuid
import io.novafoundation.nova.infrastructure.InfrastructureUrls
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import okhttp3.Headers
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito.mock
import retrofit2.Response

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class BittensorBackendClientTest {

    private var base = "https://infra.test/".toHttpUrl()

    private val urls = InfrastructureUrls { base }

    private val dao = InMemoryResponseCacheDao()

    private val component = BittensorRecommendationsRemote.Component("AVAILABLE", "FRESH", null)

    private fun catalogue() = BittensorSubnetCatalogueRemote(
        listOf(BittensorSubnetCatalogueRemote.Subnet(1, "1", "Subnet", "a", 1.toBigInteger())),
        BittensorSubnetCatalogueRemote.Meta(BittensorSubnetCatalogueRemote.Components(component, component))
    )

    private fun history(page: Int) = BittensorOperationsRemote(
        emptyList(),
        "TAO_APP_PARTIAL",
        BittensorOperationsRemote.Meta(BittensorOperationsRemote.Components(component)),
        BittensorHistoryPageInfo(page, null, 100)
    )

    private fun <T> response(value: T) = Response.success(value, Headers.headersOf("Cache-Control", "max-age=60"))

    @Test
    fun `history generation advances before invalidation cancellation is rethrown`() = runTest {
        val cancellation = CancellationException("Leaving screen")
        val failingDao = object : ResponseCacheDao by dao {
            override suspend fun keys(): List<ResponseCacheKey> = throw cancellation
        }
        val sources = createBittensorTestSources(urls, dao = failingDao, scope = backgroundScope)
        val request = requireNotNull(sources.operations.request("owner", metaAccountId = 1L))

        val failure = runCatching { sources.operations.ownTradeSubmitted(request) }.exceptionOrNull()

        assertTrue(failure is CancellationException)
        assertTrue(requireNotNull(sources.operations.request("owner", metaAccountId = 1L)).generation > request.generation)
    }

    @Test
    fun `operation invalidation failure still invalidates the portfolio chart`() = runTest {
        var attempts = 0
        val failingDao = object : ResponseCacheDao by dao {
            override suspend fun keys(): List<ResponseCacheKey> {
                attempts++
                if (attempts == 1) error("Operations cache failure")
                return dao.keys()
            }
        }
        val sources = createBittensorTestSources(urls, dao = failingDao, scope = backgroundScope)
        val chart = ResponseCacheLocal(
            "https://infra.test/v1/bittensor/portfolio/history/search?accountSubject=owner&period=DAY",
            1L,
            "{}",
            1L,
            2L
        )
        dao.put(chart)
        val request = requireNotNull(sources.operations.request("owner", metaAccountId = 1L))

        val result = runCatching { sources.operations.ownTradeSubmitted(request) }

        assertTrue(result.isFailure)
        assertEquals(2, attempts)
        assertTrue(dao.rows.isEmpty())
        assertTrue(requireNotNull(sources.operations.request("owner", metaAccountId = 1L)).generation > request.generation)
    }

    @Test
    fun `origin change through yields rejects a catalogue flight from the previous backend`() = runTest {
        val release = CompletableDeferred<Unit>()
        val catalogueApi = object : BittensorSubnetCatalogueApi {
            override suspend fun subnets(url: String): Response<BittensorSubnetCatalogueRemote> {
                release.await()
                return response(catalogue())
            }
        }
        val estimatesApi = object : BittensorEstimatesApi {
            override suspend fun operations(url: String): Response<BittensorOperationsRemote> = error("Unexpected history request")
            override suspend fun alphaYields(url: String): Response<BittensorAlphaYieldsRemote> = response(
                BittensorAlphaYieldsRemote(emptyList(), BittensorAlphaYieldsRemote.Meta(BittensorAlphaYieldsRemote.Components(component)))
            )
        }
        val sources = createBittensorTestSources(
            urls,
            scope = backgroundScope,
            catalogueApi = catalogueApi,
            estimatesApi = estimatesApi
        )
        val chain = mock(Chain::class.java)
        val old = async { runCatching { sources.catalogue.subnets() } }
        runCurrent()
        base = "https://other.test/".toHttpUrl()
        sources.yields.alphaYields(base, chain, BittensorNetuid.Subnet(1))
        release.complete(Unit)

        assertTrue(old.await().isFailure)
        assertNull(sources.catalogue.availableSubnets())
    }

    @Test
    fun `own trade invalidates every history page and chart period only for its account and backend`() = runTest {
        val sources = createBittensorTestSources(urls, dao = dao, scope = backgroundScope)
        val paths = listOf(
            "operations/search?accountSubject=owner&page=1",
            "operations/search?accountSubject=owner&page=2",
            "portfolio/history/search?accountSubject=owner&period=DAY",
            "portfolio/history/search?accountSubject=owner&period=WEEK"
        )
        val affected = paths.map { ResponseCacheLocal("https://infra.test/v1/bittensor/$it", 1L, "{}", 1L, 2L) }
        val retained = listOf(
            ResponseCacheLocal(affected.first().normalizedUrl, 2L, "{}", 1L, 2L),
            ResponseCacheLocal("https://other.test/v1/bittensor/${paths.first()}", 1L, "{}", 1L, 2L),
            ResponseCacheLocal("https://infra.test/v1/bittensor/operations/search?accountSubject=other&page=1", 1L, "{}", 1L, 2L),
            ResponseCacheLocal("https://infra.test/v1/bittensor/subnets", null, "{}", 1L, 2L)
        )
        (affected + retained).forEach { dao.put(it) }
        val request = requireNotNull(sources.operations.request("owner", metaAccountId = 1L))

        sources.operations.ownTradeSubmitted(request)

        assertEquals(retained.map { it.key }.toSet(), dao.rows.keys)
    }

    @Test
    fun `base URL change rejects old in flight response and never revives previous namespace`() = runTest {
        val release = CompletableDeferred<Unit>()
        val api = object : BittensorSubnetCatalogueApi {
            override suspend fun subnets(url: String): Response<BittensorSubnetCatalogueRemote> {
                if (url.startsWith("https://infra.test/")) release.await()
                return response(catalogue())
            }
        }
        val source = createBittensorTestSources(urls, dao = dao, scope = backgroundScope, catalogueApi = api)
        val old = async { runCatching { source.catalogue.subnets() } }
        runCurrent()
        base = "https://other.test/".toHttpUrl()
        assertNull(source.catalogue.availableSubnets())
        source.catalogue.subnets()
        release.complete(Unit)
        assertTrue(old.await().isFailure)
        base = "https://infra.test/".toHttpUrl()
        assertNull(source.catalogue.availableSubnets())
    }

    @Test
    fun `transaction invalidation rejects in flight history without affecting another account`() = runTest {
        val release = CompletableDeferred<Unit>()
        val api = object : BittensorEstimatesApi {
            override suspend fun alphaYields(url: String): Response<BittensorAlphaYieldsRemote> = error("Unexpected yield request")
            override suspend fun operations(url: String): Response<BittensorOperationsRemote> {
                if (url.toHttpUrl().queryParameter("accountSubject") == "owner") release.await()
                return response(history(1))
            }
        }
        val source = createBittensorTestSources(urls, dao = dao, scope = backgroundScope, estimatesApi = api)
        source.operationHistory(base, "other", 1, metaAccountId = 1L)
        val old = async { runCatching { source.operationHistory(base, "owner", 1, metaAccountId = 1L) } }
        runCurrent()
        source.operations.ownTradeSubmitted(requireNotNull(source.operations.request("owner", metaAccountId = 1L)))
        release.complete(Unit)
        assertTrue(old.await().isFailure)
        assertNull(source.cachedOperationHistory("owner", 1, metaAccountId = 1L))
        assertEquals(emptyList<BittensorOperationsRemote.Item>(), source.cachedOperationHistory("other", 1, metaAccountId = 1L)!!.value.items)
        assertTrue(dao.rows.keys.single().contains("accountSubject=other"))
    }

    @Test
    fun `typed history methods isolate the same address by captured wallet`() = runTest {
        val release = CompletableDeferred<Unit>()
        var calls = 0
        val api = object : BittensorEstimatesApi {
            override suspend fun alphaYields(url: String): Response<BittensorAlphaYieldsRemote> = error("Unexpected yield request")
            override suspend fun operations(url: String): Response<BittensorOperationsRemote> {
                calls++
                if (calls == 1) release.await()
                return response(history(1))
            }
        }
        val source = createBittensorTestSources(urls, dao = dao, scope = backgroundScope, estimatesApi = api)
        val first = async { source.operationHistory(base, "shared", 1, metaAccountId = 1L) }
        runCurrent()
        val duplicate = async { source.operationHistory(base, "shared", 1, metaAccountId = 1L) }
        val other = async { source.operationHistory(base, "shared", 1, metaAccountId = 2L) }
        runCurrent()
        other.await()
        assertFalse(first.isCompleted)
        release.complete(Unit)
        first.await()
        duplicate.await()
        assertEquals(2, calls)
        source.operations.ownTradeSubmitted(requireNotNull(source.operations.request("shared", metaAccountId = 1L)))
        assertNull(source.cachedOperationHistory("shared", 1, metaAccountId = 1L))
        assertEquals(1, source.cachedOperationHistory("shared", 1, metaAccountId = 2L)!!.value.pageInfo?.page)
    }
}
