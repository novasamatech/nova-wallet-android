package io.novafoundation.nova.feature_staking_impl.data.bittensor

import com.google.gson.Gson
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorNetuid
import io.novasama.substrate_sdk_android.wsrpc.logging.Logger
import java.io.IOException
import java.math.BigDecimal
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito.mock

class BittensorConfigRepositoryTest {

    private val gson = Gson()

    private val logger = mock(Logger::class.java)

    @Test
    fun `fee model distinguishes absent fields from invalid values`() {
        assertNull(document("""{"subnets":[]}""").swapFee)
        assertNull(document("""{"swapFee":null,"subnets":[]}""").swapFee)

        for (fee in listOf("{}", "[]", "true", "\"invalid\"", "\"0.004\"", "1e2147483648")) {
            val remote = document("""{"swapFee":$fee,"subnets":[]}""")

            assertSame(fee, BittensorSwapFeeRemote.Invalid, remote.swapFee)
            assertEquals(BigDecimal("0.003"), mapBittensorConfig(remote).commission.fraction)
        }
    }

    @Test
    fun `fee model preserves exact decimal fractions through decoding mapping and serialization`() {
        for (fraction in listOf("0", "0.003", "0.12345678901234567890123456789", "1e-20", "0.999999999999999999999")) {
            val remote = document("""{"swapFee":$fraction,"subnets":[]}""")
            val expected = BigDecimal(fraction)
            val decoded = remote.swapFee as BittensorSwapFeeRemote.Value

            assertEquals(expected, decoded.fraction.inFraction)
            assertEquals(expected, mapBittensorConfig(remote).commission.fraction)
            assertEquals(expected, gson.toJsonTree(remote).asJsonObject.get("swapFee").asBigDecimal)
        }
    }

    @Test
    fun `invalid fee model cannot be serialized as an absent or valid fee`() {
        val remote = BittensorConfigRemote(BittensorSwapFeeRemote.Invalid, subnets = emptyList())

        assertTrue(runCatching { gson.toJson(remote) }.exceptionOrNull() is IllegalArgumentException)
    }

    @Test
    fun `canonical iOS config supplies the fee and only valid HTTPS subnet logos`() {
        val remote = document(
            """
            {"swapFee":0.005,"subnets":[
                {"netuid":1,"logo":"https://logos.test/first.png"},
                {"netuid":1,"logo":"https://logos.test/duplicate.png"},
                {"netuid":2,"logo":"http://logos.test/insecure.png"},
                {"netuid":3,"logo":null},
                {"netuid":0,"logo":"https://logos.test/root.png"},
                {"netuid":65536,"logo":"https://logos.test/overflow.png"},
                {"netuid":-1,"logo":"https://logos.test/negative.png"},
                null, {"netuid":4}, {"logo":"https://logos.test/missing-id.png"}
            ]}
        """
        )
        val config = mapBittensorConfig(remote)

        assertEquals(BigDecimal("0.005"), config.commission.fraction)
        assertEquals(mapOf(BittensorNetuid.Subnet(1) to "https://logos.test/first.png"), config.subnetLogos)
    }

    @Test
    fun `missing or invalid fee uses the iOS fallback without losing valid logos`() {
        for (fee in listOf("null", "-0.1", "1", "2")) {
            val config = mapBittensorConfig(document("""{"swapFee":$fee,"subnets":[{"netuid":1,"logo":"https://logos.test/1.png"}]}"""))
            assertEquals(BigDecimal("0.003"), config.commission.fraction)
            assertEquals(1, config.subnetLogos.size)
        }
        assertEquals(BigDecimal("0.003"), mapBittensorConfig(document("""{"subnets":[]}""")).commission.fraction)
        assertEquals(BigDecimal.ZERO, mapBittensorConfig(document("""{"swapFee":0,"subnets":[]}""")).commission.fraction)
    }

    @Test
    fun `config is fetched once and reused by fee and logo consumers`() = runBlocking {
        val api = Api(gson, """{"swapFee":0.004,"subnets":[{"netuid":1,"logo":"https://logos.test/1.png"}]}""")
        val repository = RemoteBittensorConfigRepository(api, "https://config.test/config.json", logger)

        val config = repository.config()

        assertSame(config, repository.config())
        assertSame(config.subnetLogos, repository.subnetLogos())
        assertEquals(listOf("https://config.test/config.json"), api.urls)
    }

    @Test
    fun `concurrent fee and logo consumers share a successful request`() = runTest {
        val api = Api(gson, """{"swapFee":0.004,"subnets":[{"netuid":1,"logo":"https://logos.test/1.png"}]}""")
        api.delayMillis = 1_000L
        val repository = RemoteBittensorConfigRepository(api, "https://config.test/config.json", logger)

        val config = async(start = CoroutineStart.UNDISPATCHED) { repository.config() }
        val logos = async(start = CoroutineStart.UNDISPATCHED) { repository.subnetLogos() }

        assertEquals(1, api.urls.size)
        assertSame(config.await().subnetLogos, logos.await())
        assertEquals(1, api.urls.size)
    }

    @Test
    fun `cancelling an in flight request leaves a waiting consumer able to fetch config`() = runTest {
        val api = Api(gson, """{"swapFee":0.004,"subnets":[]}""")
        api.delayMillis = 1_000L
        val repository = RemoteBittensorConfigRepository(api, "https://config.test/config.json", logger)
        val cancelled = async(start = CoroutineStart.UNDISPATCHED) { repository.config() }
        val waiting = async(start = CoroutineStart.UNDISPATCHED) { repository.config() }
        assertEquals(1, api.urls.size)

        api.delayMillis = 0L
        cancelled.cancelAndJoin()
        val config = waiting.await()

        assertTrue(cancelled.isCancelled)
        assertEquals(BigDecimal("0.004"), config.commission.fraction)
        assertSame(config, repository.config())
        assertEquals(2, api.urls.size)
    }

    @Test
    fun `failed request propagates and can recover on the next load`() = runBlocking {
        val api = Api(gson, """{"swapFee":0.004,"subnets":[]}""")
        val unavailable = IOException("Unavailable")
        api.failure = unavailable
        val repository = RemoteBittensorConfigRepository(api, "https://config.test/config.json", logger)

        val failure = runCatching { repository.config() }.exceptionOrNull()
        assertTrue(failure is IOException)
        assertEquals(unavailable.message, failure?.message)
        api.failure = null
        assertEquals(BigDecimal("0.004"), repository.config().commission.fraction)
        assertEquals(2, api.urls.size)
    }

    @Test
    fun `cancellation is never converted to fallback configuration`() = runBlocking {
        val api = Api(gson, """{"subnets":[]}""")
        val cancellation = CancellationException("Screen closed")
        api.failure = cancellation
        val repository = RemoteBittensorConfigRepository(api, "https://config.test/config.json", logger)

        val failure = runCatching { repository.config() }.exceptionOrNull()
        assertTrue(failure is CancellationException)
        assertEquals(cancellation.message, failure?.message)
    }

    @Test
    fun `malformed logo catalogue cannot be cached as valid config`() {
        assertTrue(runCatching { mapBittensorConfig(document("""{"swapFee":0.004}""")) }.isFailure)
    }

    @Test
    fun `config decode failures propagate and do not poison the config cache`() = runBlocking {
        val malformedResponses = listOf(
            """{"swapFee":{""",
            """{"swapFee":[0.004""",
            """{"swapFee":0.004}""",
            """{"swapFee":0.004,"subnets":null}""",
            """{"swapFee":0.004,"subnets":[""",
            "null",
            "[]",
            """{"swapFee":0.004,"subnets":{}}""",
            """{"swapFee":0.004,"subnets":[123]}""",
            """{"swapFee":0.004,"subnets":[{"netuid":1.5,"logo":"https://logos.test/1.png"}]}""",
            """{"swapFee":0.004,"subnets":[{"netuid":4294967297,"logo":"https://logos.test/1.png"}]}""",
            """{"swapFee":0.004,"subnets":[{"netuid":1,"logo":{}}]}"""
        )
        for (response in malformedResponses) {
            val api = Api(gson, response)
            val repository = RemoteBittensorConfigRepository(api, "https://config.test/config.json", logger)

            assertTrue(response, runCatching { repository.config() }.isFailure)
            api.json = """{"swapFee":0.004,"subnets":[]}"""
            assertEquals(BigDecimal("0.004"), repository.config().commission.fraction)
            assertEquals(2, api.urls.size)
        }
    }

    @Test
    fun `invalid fee field alone defaults after successful config decoding`() = runBlocking {
        for (fee in listOf("{}", "[]", "true", "\"invalid\"", "\"0.004\"", "null", "-0.1", "1", "2")) {
            val api = Api(gson, """{"swapFee":$fee,"subnets":[{"netuid":1,"logo":"https://logos.test/1.png"}]}""")
            val repository = RemoteBittensorConfigRepository(api, "https://config.test/config.json", logger)

            val config = repository.config()

            assertEquals(BigDecimal("0.003"), config.commission.fraction)
            assertEquals(mapOf(BittensorNetuid.Subnet(1) to "https://logos.test/1.png"), config.subnetLogos)
            assertSame(config, repository.config())
            assertEquals(1, api.urls.size)
        }
    }

    @Test
    fun `logo fallback does not mask commission failure or cache a default`() = runBlocking {
        val api = Api(gson, """{"swapFee":0.004,"subnets":[]}""")
        val unavailable = IOException("Unavailable")
        api.failure = unavailable
        val repository = RemoteBittensorConfigRepository(api, "https://config.test/config.json", logger)

        assertTrue(repository.subnetLogos().isEmpty())
        val failure = runCatching { repository.config() }.exceptionOrNull()
        assertTrue(failure is IOException)
        assertEquals(unavailable.message, failure?.message)
        api.failure = null
        assertEquals(BigDecimal("0.004"), repository.config().commission.fraction)
        assertEquals(3, api.urls.size)
    }

    @Test
    fun `logo fallback preserves decode failure for fee consumers`() = runBlocking {
        val api = Api(gson, """{"swapFee":0.004,"subnets":{}}""")
        val repository = RemoteBittensorConfigRepository(api, "https://config.test/config.json", logger)

        assertTrue(repository.subnetLogos().isEmpty())
        assertTrue(runCatching { repository.config() }.isFailure)
    }

    @Test
    fun `logo loading propagates caller cancellation`() = runBlocking {
        val api = Api(gson, """{"subnets":[]}""")
        val cancellation = CancellationException("Screen closed")
        api.failure = cancellation
        val repository = RemoteBittensorConfigRepository(api, "https://config.test/config.json", logger)

        val failure = runCatching { repository.subnetLogos() }.exceptionOrNull()

        assertTrue(failure is CancellationException)
        assertEquals(cancellation.message, failure?.message)
    }

    @Test
    fun `config timeout fails while optional logos fall back and next request recovers`() = runTest {
        val api = Api(gson, """{"swapFee":0.004,"subnets":[]}""")
        api.delayMillis = 30_001L
        val repository = RemoteBittensorConfigRepository(api, "https://config.test/config.json", logger)

        assertTrue(runCatching { repository.config() }.exceptionOrNull() is IOException)
        assertTrue(repository.subnetLogos().isEmpty())
        api.delayMillis = 0L
        assertEquals(BigDecimal("0.004"), repository.config().commission.fraction)
        assertEquals(3, api.urls.size)
    }

    private fun document(json: String): BittensorConfigRemote = gson.fromJson(json, BittensorConfigRemote::class.java)

    private class Api(private val gson: Gson, var json: String) : BittensorConfigApi {

        val urls = mutableListOf<String>()

        var failure: Exception? = null

        var delayMillis = 0L

        override suspend fun config(url: String): BittensorConfigRemote {
            urls += url
            delay(delayMillis)
            failure?.let { throw it }
            return gson.fromJson(json, BittensorConfigRemote::class.java)
        }
    }
}
