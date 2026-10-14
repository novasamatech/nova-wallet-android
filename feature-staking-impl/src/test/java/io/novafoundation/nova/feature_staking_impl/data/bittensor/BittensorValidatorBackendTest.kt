package io.novafoundation.nova.feature_staking_impl.data.bittensor

import com.google.gson.Gson
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorValidatorGates
import io.novafoundation.nova.infrastructure.InfrastructureUrlMissingException
import io.novafoundation.nova.infrastructure.InfrastructureUrls
import kotlinx.coroutines.runBlocking
import okhttp3.Headers
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response
import java.math.BigDecimal
import java.math.BigInteger

class BittensorValidatorBackendTest {

    private val gson = Gson()

    @Test
    fun `recommendations use the attested gateway and preserve class priority for the requested netuid`() = runBlocking {
        val api = Api("max-age=60")
        api.recommendationsDocument = recommendations("FRESH")
        val urls = InfrastructureUrls { "https://infra.test/".toHttpUrl() }
        val backend = BittensorValidatorBackend(api, urls)

        assertEquals(listOf("stable-root", "balanced-root", "higher-root"), backend.recommendedHotkeys(0))
        assertEquals(listOf("subnet-one"), backend.recommendedHotkeys(1))
        assertEquals(listOf("https://infra.test/v1/bittensor/recommendations"), api.requests)
    }

    @Test
    fun `stale recommendations or missing generation cannot select a root validator`() = runBlocking {
        val api = Api("no-store")
        val urls = InfrastructureUrls { "https://infra.test/".toHttpUrl() }
        val backend = BittensorValidatorBackend(api, urls)
        api.recommendationsDocument = recommendations("STALE")
        assertTrue(runCatching { backend.recommendedHotkeys(0) }.isFailure)
        api.recommendationsDocument = policy("FRESH")
        assertTrue(runCatching { backend.recommendedHotkeys(0) }.isFailure)
    }

    @Test
    fun `uses gateway paths and reuses only HTTP fresh responses`() = runBlocking {
        val api = Api("max-age=60")
        val infrastructureUrls = InfrastructureUrls { "https://infra.test/".toHttpUrl() }
        val backend = BittensorValidatorBackend(api, infrastructureUrls)
        backend.validators(64)
        backend.validators(64)
        backend.gates()
        backend.gates()
        assertEquals(
            listOf("https://infra.test/v1/bittensor/subnets/64/validators", "https://infra.test/v1/bittensor/recommendations/subnets"),
            api.requests
        )
    }

    @Test
    fun `changing infrastructure invalidates cached validator recommendations`() = runBlocking {
        val api = Api("max-age=60")
        api.recommendationsDocument = recommendations("FRESH")
        var baseUrl = "https://infra.test/".toHttpUrl()
        val urls = InfrastructureUrls { baseUrl }
        val backend = BittensorValidatorBackend(api, urls)
        backend.recommendedHotkeys(0)
        baseUrl = "https://other-infra.test/".toHttpUrl()
        backend.recommendedHotkeys(0)

        assertEquals(listOf("https://infra.test/v1/bittensor/recommendations", "https://other-infra.test/v1/bittensor/recommendations"), api.requests)
    }

    @Test
    fun `no store responses and missing infrastructure URL are not silently cached or sent unsigned`() = runBlocking {
        val api = Api("no-store")
        val infrastructureUrls = InfrastructureUrls { "https://infra.test/".toHttpUrl() }
        val backend = BittensorValidatorBackend(api, infrastructureUrls)
        backend.validators(1)
        backend.validators(1)
        assertEquals(2, api.requests.size)
        val missingInfrastructureUrls = InfrastructureUrls { null }
        val unconfiguredBackend = BittensorValidatorBackend(api, missingInfrastructureUrls)
        assertTrue(runCatching { unconfiguredBackend.validators(1) }.exceptionOrNull() is InfrastructureUrlMissingException)
        assertEquals(2, api.requests.size)
    }

    @Test
    fun `changed infrastructure URL invalidates validator and policy caches`() = runBlocking {
        val api = Api("max-age=60")
        var baseUrl = "https://infra.test/".toHttpUrl()
        val infrastructureUrls = InfrastructureUrls { baseUrl }
        val backend = BittensorValidatorBackend(api, infrastructureUrls)
        backend.validators(1)
        backend.gates()

        baseUrl = "https://another-infra.test/".toHttpUrl()
        backend.validators(1)
        backend.gates()

        assertEquals(
            listOf(
                "https://infra.test/v1/bittensor/subnets/1/validators",
                "https://infra.test/v1/bittensor/recommendations/subnets",
                "https://another-infra.test/v1/bittensor/subnets/1/validators",
                "https://another-infra.test/v1/bittensor/recommendations/subnets"
            ),
            api.requests
        )
    }

    @Test
    fun `missing or insecure infrastructure cannot reuse cached data`() = runBlocking {
        val api = Api("max-age=60")
        var baseUrl: HttpUrl? = "https://infra.test/".toHttpUrl()
        val infrastructureUrls = InfrastructureUrls { baseUrl }
        val backend = BittensorValidatorBackend(api, infrastructureUrls)
        backend.validators(1)
        backend.gates()

        baseUrl = null
        assertTrue(runCatching { backend.validators(1) }.exceptionOrNull() is InfrastructureUrlMissingException)
        assertTrue(runCatching { backend.gates() }.exceptionOrNull() is InfrastructureUrlMissingException)

        baseUrl = "http://infra.test/".toHttpUrl()
        assertTrue(runCatching { backend.validators(1) }.exceptionOrNull() is IllegalArgumentException)
        assertTrue(runCatching { backend.gates() }.exceptionOrNull() is IllegalArgumentException)
        assertEquals(2, api.requests.size)
    }

    @Test
    fun `missing or stale server policy cannot enable validator selection`() {
        assertTrue(runCatching { bindBittensorValidatorGates(BittensorRecommendationsRemote(null)) }.isFailure)
        assertTrue(runCatching { bindBittensorValidatorGates(policy("STALE")) }.isFailure)
        assertEquals(BigDecimal("0.18"), bindBittensorValidatorGates(policy("FRESH")).maxTake)
        assertTrue(runCatching { BittensorValidatorGates(BigDecimal("1.01"), true, true) }.isFailure)
    }

    @Test
    fun `take limit uses exact integers instead of a rounded percentage`() {
        val gates = BittensorValidatorGates(BigDecimal("0.18"), true, true)
        assertTrue(gates.allowsTake(BigInteger.valueOf(11796)))
        assertFalse(gates.allowsTake(BigInteger.valueOf(11797)))
        assertTrue(runCatching { gates.allowsTake(BigInteger.valueOf(65536)) }.isFailure)
    }

    private class Api(private val cacheControl: String) : BittensorValidatorsApi {

        val requests = mutableListOf<String>()

        var recommendationsDocument = policy("FRESH")

        override suspend fun validators(url: String): Response<BittensorValidatorsRemote> {
            requests += url
            return Response.success(BittensorValidatorsRemote(emptyList()), headers())
        }
        override suspend fun recommendations(url: String): Response<BittensorRecommendationsRemote> {
            requests += url
            return Response.success(recommendationsDocument, headers())
        }
        private fun headers() = Headers.Builder().add("Cache-Control", cacheControl).build()
    }

    private fun recommendations(freshness: String): BittensorRecommendationsRemote = gson.fromJson(
        """{
            "meta":{
                "components":{"recommendations":{"availability":"AVAILABLE","freshness":"$freshness"}},
                "generation":{"sourceBlockNumber":123},
                "clientGates":{"maxTake":"0.18","requirePermit":true,"requireActiveWithinCutoff":true}
            },
            "classes":{
                "stable":[{"netuid":1,"hotkey":"subnet-one"},{"netuid":0,"hotkey":"stable-root"}],
                "balanced":[{"netuid":0,"hotkey":"balanced-root"}],
                "higherUpside":[{"netuid":0,"hotkey":"higher-root"}]
            }
        }""",
        BittensorRecommendationsRemote::class.java
    )

    companion object {
        private fun policy(freshness: String) = BittensorRecommendationsRemote(
            BittensorRecommendationsRemote.Meta(
                BittensorRecommendationsRemote.Gates("0.18", true, true),
                BittensorRecommendationsRemote.Components(BittensorRecommendationsRemote.Component("AVAILABLE", freshness))
            )
        )
    }
}
