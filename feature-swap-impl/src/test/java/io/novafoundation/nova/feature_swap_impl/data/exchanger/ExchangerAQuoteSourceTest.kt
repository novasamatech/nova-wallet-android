package io.novafoundation.nova.feature_swap_impl.data.exchanger

import io.novafoundation.nova.common.data.network.NetworkApiCreator
import io.novafoundation.nova.infrastructure.InfrastructureUrlMissingException
import io.novafoundation.nova.infrastructure.InfrastructureUrls
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException

class ExchangerAQuoteSourceTest {

    private val payAsset = ExchangerAAssetReference("asset-hub", 1)

    private val receiveAsset = ExchangerAAssetReference("bittensor", 0)

    @Test
    fun `assets preserve backend direction flags and use the configured origin`() = runBlocking {
        val fixture = Fixture(
            """{"assets":[{"chainId":"asset-hub","assetId":1,"send":true,"receive":false}]}"""
        )

        val result = fixture.source.assets()

        assertEquals("asset-hub", result.single().chainId)
        assertEquals(1, result.single().assetId)
        assertEquals(true, result.single().send)
        assertEquals(false, result.single().receive)
        val request = fixture.backend.requests.single()
        assertEquals("GET", request.method)
        assertEquals("https://infra.test/v1/exchanger-a/assets", request.url.toString())
    }

    @Test
    fun `empty assets is a successful empty catalogue`() = runBlocking {
        val fixture = Fixture("""{"assets":[]}""")

        assertTrue(fixture.source.assets().isEmpty())
    }

    @Test
    fun `null entries are filtered while valid assets are preserved`() = runBlocking {
        val fixture = Fixture(
            """{"assets":[null,{"chainId":"asset-hub","assetId":1,"send":true,"receive":false},null]}"""
        )

        val assets = fixture.source.assets()

        assertEquals(1, assets.size)
        assertEquals("asset-hub", assets.single().chainId)
        assertEquals(1, assets.single().assetId)
    }

    @Test
    fun `missing assets field is an error rather than an empty catalogue`() = runBlocking {
        for (json in listOf("{}", """{"assets":null}""")) {
            val fixture = Fixture(json)

            assertTrue(runCatching { fixture.source.assets() }.exceptionOrNull() is IOException)
        }
    }

    @Test
    fun `pay quote sends only pay amount as a decimal string`() = runBlocking {
        val fixture = Fixture(QUOTE)
        val request = ExchangerAQuoteRequest.pay(payAsset, receiveAsset, "100.000001")

        val result = fixture.source.quote(request)

        val sent = fixture.backend.requests.single()
        assertEquals("POST", sent.method)
        assertEquals("https://infra.test/v1/exchanger-a/quotes", sent.url.toString())
        assertEquals(
            """{"from":{"chainId":"asset-hub","assetId":1},"to":{"chainId":"bittensor","assetId":0},"payAmount":"100.000001"}""",
            sent.bodyText()
        )
        assertEquals("quote", result.kind)
        assertEquals("100.000001", result.payAmount)
        assertEquals("0.0421139", result.receiveAmount)
        assertEquals("0.2877999", result.limits.minPayAmount)
        assertNull(result.limits.maxPayAmount)
        assertEquals("0.0017", result.networkFees?.deposit)
        assertEquals("0.00006", result.networkFees?.withdrawal)
        assertEquals(10, result.durationMinutes?.min)
        assertEquals(60, result.durationMinutes?.max)
    }

    @Test
    fun `receive quote sends only receive amount and retains the returned estimate`() = runBlocking {
        val fixture = Fixture(QUOTE)
        val request = ExchangerAQuoteRequest.receive(payAsset, receiveAsset, "0.042")

        val result = fixture.source.quote(request)

        assertEquals(
            """{"from":{"chainId":"asset-hub","assetId":1},"to":{"chainId":"bittensor","assetId":0},"receiveAmount":"0.042"}""",
            fixture.backend.requests.single().bodyText()
        )
        assertEquals("100.000001", result.payAmount)
        assertEquals("0.0421139", result.receiveAmount)
    }

    @Test
    fun `out of range retains both limits and does not fabricate quote amounts`() = runBlocking {
        val fixture = Fixture(
            """{"kind":"out_of_range","reason":"above_maximum","limits":{"minPayAmount":"1","maxPayAmount":"500"}}"""
        )
        val request = ExchangerAQuoteRequest.pay(payAsset, receiveAsset, "600")

        val result = fixture.source.quote(request)

        assertEquals("out_of_range", result.kind)
        assertEquals("above_maximum", result.reason)
        assertEquals("1", result.limits.minPayAmount)
        assertEquals("500", result.limits.maxPayAmount)
        assertNull(result.payAmount)
        assertNull(result.receiveAmount)
        assertNull(result.networkFees)
        assertNull(result.durationMinutes)
    }

    @Test
    fun `nullable fee and duration fields stay unavailable`() = runBlocking {
        val fixture = Fixture(
            """{"kind":"quote","payAmount":"10","receiveAmount":"0.02","limits":{"minPayAmount":"1","maxPayAmount":null},
                "networkFees":{"deposit":null,"withdrawal":null},"durationMinutes":null,"warning":"Slow network"}"""
        )
        val request = ExchangerAQuoteRequest.pay(payAsset, receiveAsset, "10")

        val result = fixture.source.quote(request)

        assertNull(result.networkFees?.deposit)
        assertNull(result.networkFees?.withdrawal)
        assertNull(result.durationMinutes)
        assertEquals("Slow network", result.warning)
    }

    @Test
    fun `each request resolves the current configured infrastructure origin`() = runBlocking {
        var baseUrl = "https://first.test/config-path/".toHttpUrl()
        val urls = InfrastructureUrls { baseUrl }
        val fixture = Fixture("""{"assets":[]}""", urls = urls)

        fixture.source.assets()
        baseUrl = "https://second.test/".toHttpUrl()
        fixture.source.assets()

        assertEquals(
            listOf("https://first.test/v1/exchanger-a/assets", "https://second.test/v1/exchanger-a/assets"),
            fixture.backend.requests.map { it.url.toString() }
        )
    }

    @Test
    fun `missing infrastructure configuration fails before HTTP`() = runBlocking {
        val urls = InfrastructureUrls { null }
        val fixture = Fixture("""{"assets":[]}""", urls = urls)

        val error = runCatching { fixture.source.assets() }.exceptionOrNull()

        assertTrue(error is InfrastructureUrlMissingException)
        assertTrue(fixture.backend.requests.isEmpty())
    }

    @Test
    fun `HTTP failures retain status and body for caller classification`() = runBlocking {
        val body = """{"error":{"code":"rate_limited","message":"Try again later"}}"""
        val fixture = Fixture(body, status = 503)
        val request = ExchangerAQuoteRequest.pay(payAsset, receiveAsset, "10")

        val error = runCatching { fixture.source.quote(request) }.exceptionOrNull() as HttpException

        assertEquals(503, error.code())
        assertEquals(body, error.response()?.errorBody()?.string())
        assertEquals(1, fixture.backend.requests.size)
    }

    @Test
    fun `configuration failures and cancellation propagate without sending HTTP`() = runBlocking {
        val errors = listOf(IOException("Config unavailable"), CancellationException("Caller cancelled"))
        for (expected in errors) {
            val urls = InfrastructureUrls { throw expected }
            val fixture = Fixture("""{"assets":[]}""", urls = urls)
            val request = ExchangerAQuoteRequest.receive(payAsset, receiveAsset, "1")

            val actual = runCatching { fixture.source.quote(request) }.exceptionOrNull()

            assertSame(expected, actual)
            assertTrue(fixture.backend.requests.isEmpty())
        }
    }

    private class Fixture(
        body: String,
        status: Int = 200,
        urls: InfrastructureUrls = InfrastructureUrls { "https://infra.test/".toHttpUrl() }
    ) {

        val backend = Backend(body, status)

        private val client = OkHttpClient.Builder().addInterceptor(backend).build()

        private val apiCreator = NetworkApiCreator(client, "https://placeholder.test/")

        private val api = apiCreator.create(ExchangerAApi::class.java)

        val source = ExchangerAQuoteSource(api, urls)
    }

    private class Backend(private val body: String, private val status: Int) : Interceptor {

        val requests = mutableListOf<Request>()

        override fun intercept(chain: Interceptor.Chain): Response {
            val request = chain.request()
            requests += request
            return Response.Builder()
                .request(request)
                .protocol(Protocol.HTTP_1_1)
                .code(status)
                .message("Test response")
                .body(body.toResponseBody("application/json".toMediaType()))
                .build()
        }
    }

    private fun Request.bodyText(): String {
        val buffer = Buffer()
        requireNotNull(body).writeTo(buffer)
        return buffer.readUtf8()
    }

    companion object {

        private val QUOTE = """
            {"kind":"quote","payAmount":"100.000001","receiveAmount":"0.0421139",
             "limits":{"minPayAmount":"0.2877999","maxPayAmount":null},
             "networkFees":{"deposit":"0.0017","withdrawal":"0.00006"},
             "durationMinutes":{"min":10,"max":60},"warning":null}
        """.trimIndent()
    }
}
