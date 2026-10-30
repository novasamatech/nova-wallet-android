package io.novafoundation.nova.feature_staking_impl.data.bittensor

import io.novafoundation.nova.common.data.network.NetworkApiCreator
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorNetuid
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import kotlinx.coroutines.runBlocking
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito.mock

class BittensorYieldRequestTest {

    @Test
    fun `snapshot request passes a backend that rejects all query parameters`() = runBlocking {
        verifySnapshotRequest(extraFields = "")
    }

    @Test
    fun `legacy page info does not trigger another snapshot request`() = runBlocking {
        verifySnapshotRequest(extraFields = """, "pageInfo": {"page": 1, "pageSize": 100, "nextPage": 2}""")
    }

    private suspend fun verifySnapshotRequest(extraFields: String) {
        val requests = mutableListOf<Request>()
        val backend = Interceptor { call ->
            val request = call.request()
            requests += request
            val code = if (request.url.query == null) 200 else 400
            val json = """
                {
                  "items": [],
                  "meta": {"components": {"alphaYield": {"availability": "AVAILABLE", "freshness": "FRESH"}}}
                  $extraFields
                }
            """.trimIndent()
            val body = json.toResponseBody("application/json".toMediaType())
            Response.Builder()
                .request(request)
                .protocol(Protocol.HTTP_1_1)
                .code(code)
                .message("test")
                .body(body)
                .build()
        }
        val client = OkHttpClient.Builder().addInterceptor(backend).build()
        val apiCreator = NetworkApiCreator(client, "https://infra.test/")
        val api = apiCreator.create(BittensorEstimatesApi::class.java)
        val source = BittensorEstimatesRemoteSource(api, spacingMillis = 0L)
        val chain = mock(Chain::class.java)
        val subnet = BittensorNetuid.Subnet(64)
        val baseUrl = "https://infra.test/".toHttpUrl()

        try {
            val result = source.yields(baseUrl, chain, subnet)

            assertTrue(result.allPagesRead)
            assertTrue(result.items.isEmpty())
            assertEquals(1, requests.size)
            assertEquals("GET", requests.single().method)
            assertEquals("/v1/bittensor/subnets/64/yields/alpha", requests.single().url.encodedPath)
            assertNull(requests.single().url.query)
        } finally {
            client.dispatcher.executorService.shutdown()
            client.connectionPool.evictAll()
        }
    }
}
