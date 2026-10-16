package io.novafoundation.nova.feature_staking_impl.data.bittensor

import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorValidatorGates
import kotlinx.coroutines.runBlocking
import okhttp3.Headers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response
import java.math.BigDecimal
import java.math.BigInteger

class BittensorValidatorBackendTest {

    @Test
    fun `uses gateway paths and reuses only HTTP fresh responses`() = runBlocking {
        val api = Api("max-age=60")
        val backend = BittensorValidatorBackend(api, "https://infra.test/")
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
    fun `no store responses and missing infrastructure URL are not silently cached or sent unsigned`() = runBlocking {
        val api = Api("no-store")
        val backend = BittensorValidatorBackend(api, "https://infra.test/")
        backend.validators(1)
        backend.validators(1)
        assertEquals(2, api.requests.size)
        assertTrue(runCatching { BittensorValidatorBackend(api, "").validators(1) }.isFailure)
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

    @Test
    fun `ranked validators use the actual ranking route and cache only fresh HTTP data`() = runBlocking {
        val api = Api("max-age=60")
        val backend = BittensorValidatorBackend(api, "https://infra.test/")
        backend.rankedValidators(64)
        backend.rankedValidators(64)
        assertEquals(listOf("https://infra.test/v1/bittensor/recommendations/subnets/64/validators"), api.requests)
    }

    @Test
    fun `a stale ranking cannot supply recommendation or selection gates`() = runBlocking {
        val api = Api("max-age=60")
        api.rankedFreshness = "STALE"
        val backend = BittensorValidatorBackend(api, "https://infra.test/")
        assertTrue(runCatching { backend.rankedValidators(64) }.isFailure)
    }

    @Test
    fun `root metrics must describe the requested hotkey`() = runBlocking {
        val address = "5".repeat(48)
        val api = Api("max-age=60")
        api.root = BittensorRootValidatorRemote(BittensorRootValidatorRemote.Validator("6".repeat(48), "Other", null), rootMeta())
        val backend = BittensorValidatorBackend(api, "https://infra.test/")
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
        val config = object : BittensorEarnConfigApi {
            override suspend fun getConfig(url: String) = BittensorEarnConfigRemote(BittensorEarnConfigRemote.Entry(true), address)
        }
        val backend = BittensorValidatorBackend(api, "https://infra.test/")
        val selection = BittensorRootValidatorSelection()
        val logger = org.mockito.Mockito.mock(io.novasama.substrate_sdk_android.wsrpc.logging.Logger::class.java)
        val source = RemoteBittensorRootValidatorSource(config, "https://config.test/", backend, logger, selection)
        assertEquals(BigDecimal("7.25"), source.annualPercentageYield())
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
        val backend = BittensorValidatorBackend(api, "https://infra.test/")
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
        override suspend fun validators(url: String): Response<BittensorValidatorsRemote> {
            requests += url
            return Response.success(BittensorValidatorsRemote(emptyList()), headers())
        }
        override suspend fun recommendations(url: String): Response<BittensorRecommendationsRemote> {
            requests += url
            return Response.success(policy("FRESH"), headers())
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

    companion object {
        private fun policy(freshness: String) = BittensorRecommendationsRemote(
            BittensorRecommendationsRemote.Meta(
                BittensorRecommendationsRemote.Gates("0.18", true, true),
                BittensorRecommendationsRemote.Components(BittensorRecommendationsRemote.Component("AVAILABLE", freshness))
            )
        )
    }
}
