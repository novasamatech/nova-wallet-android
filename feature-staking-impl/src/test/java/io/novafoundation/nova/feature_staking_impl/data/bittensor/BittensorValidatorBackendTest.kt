package io.novafoundation.nova.feature_staking_impl.data.bittensor

import io.novafoundation.nova.runtime.ext.addressOf
import com.google.gson.Gson
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorValidatorGates
import io.novafoundation.nova.infrastructure.InfrastructureUrlMissingException
import io.novafoundation.nova.infrastructure.InfrastructureUrls
import java.math.BigDecimal
import java.math.BigInteger
import kotlinx.coroutines.runBlocking
import okhttp3.Headers
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response

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
        assertTrue(runCatching { mapBittensorValidatorGates(BittensorRecommendationsRemote(null)) }.isFailure)
        assertTrue(runCatching { mapBittensorValidatorGates(policy("STALE")) }.isFailure)
        assertEquals(BigDecimal("0.18"), mapBittensorValidatorGates(policy("FRESH")).maxCommissionRate)
        assertTrue(runCatching { BittensorValidatorGates(BigDecimal("1.01"), true, true) }.isFailure)
    }

    @Test
    fun `take limit uses exact integers instead of a rounded percentage`() {
        val gates = BittensorValidatorGates(BigDecimal("0.18"), true, true)
        assertTrue(gates.allowsCommission(BigInteger.valueOf(11796)))
        assertFalse(gates.allowsCommission(BigInteger.valueOf(11797)))
        assertTrue(runCatching { gates.allowsCommission(BigInteger.valueOf(65536)) }.isFailure)
    }

    @Test
    fun `invalid fresh HTTP payloads do not prevent a subsequent recovery`() = runBlocking {
        val api = Api("max-age=60")
        val urls = InfrastructureUrls { "https://infra.test/".toHttpUrl() }
        val backend = BittensorValidatorBackend(api, urls)
        api.recommendationsDocument = policy("STALE")
        assertTrue(runCatching { backend.gates() }.isFailure)
        api.recommendationsDocument = policy("FRESH")
        backend.gates()

        api.recommendationsDocument = recommendations("STALE")
        assertTrue(runCatching { backend.recommendedHotkeys(0) }.isFailure)
        api.recommendationsDocument = recommendations("FRESH")
        assertEquals(listOf("stable-root", "balanced-root", "higher-root"), backend.recommendedHotkeys(0))
        assertEquals(4, api.requests.size)
    }

    @Test
    fun `ranked validators use the actual ranking route and cache only fresh HTTP data`() = runBlocking {
        val api = Api("max-age=60")
        val urls = InfrastructureUrls { "https://infra.test/".toHttpUrl() }
        val backend = BittensorValidatorBackend(api, urls)
        backend.rankedValidators(64)
        backend.rankedValidators(64)
        assertEquals(listOf("https://infra.test/v1/bittensor/recommendations/subnets/64/validators"), api.requests)
    }

    @Test
    fun `a stale ranking cannot supply recommendation or selection gates`() = runBlocking {
        val api = Api("max-age=60")
        api.rankedFreshness = "STALE"
        val urls = InfrastructureUrls { "https://infra.test/".toHttpUrl() }
        val backend = BittensorValidatorBackend(api, urls)
        assertTrue(runCatching { backend.rankedValidators(64) }.isFailure)
    }

    @Test
    fun `root metrics must describe the requested hotkey`() = runBlocking {
        val address = "5".repeat(48)
        val api = Api("max-age=60")
        api.root = BittensorRootValidatorRemote(BittensorRootValidatorRemote.Validator("6".repeat(48), "Other", null), rootMeta())
        val urls = InfrastructureUrls { "https://infra.test/".toHttpUrl() }
        val backend = BittensorValidatorBackend(api, urls)
        assertTrue(runCatching { backend.rootValidator(address) }.isFailure)
    }

    @Test
    fun `root yield is annual percent already net of validator take`() = runBlocking {
        val address = "5".repeat(48)
        val api = Api("max-age=60")
        api.root = BittensorRootValidatorRemote(
            BittensorRootValidatorRemote.Validator(
                address,
                "Synthetic",
                BittensorRootValidatorRemote.Yield(
                    BittensorRootValidatorRemote.Window("7.25", "FULL", "REPORTED")
                )
            ),
            rootMeta()
        )
        val urls = InfrastructureUrls { "https://infra.test/".toHttpUrl() }
        val backend = BittensorValidatorBackend(api, urls)
        val validators = org.mockito.Mockito.mock(BittensorSubnetValidatorRepository::class.java)
        val logger = org.mockito.Mockito.mock(io.novasama.substrate_sdk_android.wsrpc.logging.Logger::class.java)
        val chain = org.mockito.Mockito.mock(io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain::class.java)
        org.mockito.Mockito.`when`(chain.addressPrefix).thenReturn(42)
        val hotkey = io.novafoundation.nova.common.address.AccountIdKey(ByteArray(32) { 1 })
        val validator = io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorValidator(hotkey, "Synthetic")
        api.root = BittensorRootValidatorRemote(
            BittensorRootValidatorRemote.Validator(chain.addressOf(hotkey.value), "Synthetic", api.root!!.validator!!.stakeYield),
            rootMeta()
        )
        val source = RemoteBittensorRootValidatorSource(backend, validators, logger)
        assertEquals(BigDecimal("7.25"), source.annualPercentageYield(chain, validator))
    }

    @Test
    fun `reported root rewards sum TAO payouts without duplicating an event`() = runBlocking {
        val api = Api("no-store")
        val first = BittensorRootClaimsRemote.Item(100, 1, 2, "synthetic", "0.125")
        api.claims = BittensorRootClaimsRemote(
            listOf(first, first, BittensorRootClaimsRemote.Item(101, 1, 2, "synthetic", "0.375")),
            "TAO_APP_PARTIAL",
            BittensorRootClaimsRemote.Meta(
                BittensorRootClaimsRemote.Components(
                    BittensorRecommendationsRemote.Component("AVAILABLE", "FRESH")
                )
            )
        )
        val urls = InfrastructureUrls { "https://infra.test/".toHttpUrl() }
        val backend = BittensorValidatorBackend(api, urls)
        assertEquals(BigDecimal("0.500"), backend.receivedRootRewards("synthetic-subject"))
    }

    private fun rootMeta() = BittensorRootValidatorRemote.Meta(
        BittensorRootValidatorRemote.Components(
            BittensorRecommendationsRemote.Component("AVAILABLE", "FRESH")
        )
    )

    private class Api(private val cacheControl: String) : BittensorValidatorsApi {

        val requests = mutableListOf<String>()
        var rankedFreshness = "FRESH"
        var root: BittensorRootValidatorRemote? = null
        var claims: BittensorRootClaimsRemote? = null

        var recommendationsDocument = policy("FRESH")

        override suspend fun validators(url: String): Response<BittensorValidatorsRemote> {
            requests += url
            return Response.success(BittensorValidatorsRemote(emptyList()), headers())
        }
        override suspend fun recommendations(url: String): Response<BittensorRecommendationsRemote> {
            requests += url
            return Response.success(recommendationsDocument, headers())
        }
        override suspend fun rootClaims(url: String): Response<BittensorRootClaimsRemote> {
            requests += url
            return claims?.let { Response.success(it, headers()) } ?: Response.error(503, okhttp3.ResponseBody.create(null, ""))
        }
        override suspend fun rootValidator(url: String): Response<BittensorRootValidatorRemote> {
            requests += url
            return root?.let { Response.success(it, headers()) } ?: Response.error(503, okhttp3.ResponseBody.create(null, ""))
        }
        override suspend fun rankedValidators(url: String): Response<BittensorRankedValidatorsRemote> {
            requests += url
            return Response.success(BittensorRankedValidatorsRemote(policy(rankedFreshness).meta, emptyList()), headers())
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
