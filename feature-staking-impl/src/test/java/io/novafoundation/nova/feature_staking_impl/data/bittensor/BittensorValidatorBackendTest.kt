package io.novafoundation.nova.feature_staking_impl.data.bittensor

import com.google.gson.Gson
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorValidatorGates
import io.novafoundation.nova.infrastructure.InfrastructureUrlMissingException
import io.novafoundation.nova.infrastructure.InfrastructureUrls
import io.novafoundation.nova.runtime.ext.addressOf
import java.math.BigDecimal
import java.math.BigInteger
import kotlinx.coroutines.runBlocking
import okhttp3.Headers
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.ResponseBody.Companion.toResponseBody
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
        assertTrue(
            runCatching { backend.recommendedHotkeys(0) }
                .isFailure
        )
        api.recommendationsDocument = policy("FRESH")
        assertTrue(
            runCatching { backend.recommendedHotkeys(0) }
                .isFailure
        )
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
        assertTrue(
            runCatching { unconfiguredBackend.validators(1) }
                .exceptionOrNull() is InfrastructureUrlMissingException
        )
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
        assertTrue(
            runCatching { backend.validators(1) }
                .exceptionOrNull() is InfrastructureUrlMissingException
        )
        assertTrue(
            runCatching { backend.gates() }
                .exceptionOrNull() is InfrastructureUrlMissingException
        )

        baseUrl = "http://infra.test/".toHttpUrl()
        assertTrue(
            runCatching { backend.validators(1) }
                .exceptionOrNull() is IllegalArgumentException
        )
        assertTrue(
            runCatching { backend.gates() }
                .exceptionOrNull() is IllegalArgumentException
        )
        assertEquals(2, api.requests.size)
    }

    @Test
    fun `missing or stale server policy cannot enable validator selection`() {
        assertTrue(
            runCatching { mapBittensorValidatorGates(BittensorRecommendationsRemote(null)) }
                .isFailure
        )
        assertTrue(
            runCatching { mapBittensorValidatorGates(policy("STALE")) }
                .isFailure
        )
        assertEquals(
            BigDecimal("0.18"),
            mapBittensorValidatorGates(policy("FRESH")).maxCommissionRate
        )
        assertTrue(
            runCatching { BittensorValidatorGates(BigDecimal("1.01"), true, true) }
                .isFailure
        )
    }

    @Test
    fun `take limit uses exact integers instead of a rounded percentage`() {
        val gates = BittensorValidatorGates(BigDecimal("0.18"), true, true)
        assertTrue(gates.allowsCommission(BigInteger.valueOf(11796)))
        assertFalse(gates.allowsCommission(BigInteger.valueOf(11797)))
        assertTrue(
            runCatching { gates.allowsCommission(BigInteger.valueOf(65536)) }
                .isFailure
        )
    }

    @Test
    fun `invalid fresh HTTP payloads do not prevent a subsequent recovery`() = runBlocking {
        val api = Api("max-age=60")
        val urls = InfrastructureUrls { "https://infra.test/".toHttpUrl() }
        val backend = BittensorValidatorBackend(api, urls)
        api.recommendationsDocument = policy("STALE")
        assertTrue(
            runCatching { backend.gates() }
                .isFailure
        )
        api.recommendationsDocument = policy("FRESH")
        backend.gates()

        api.recommendationsDocument = recommendations("STALE")
        assertTrue(
            runCatching { backend.recommendedHotkeys(0) }
                .isFailure
        )
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
        assertTrue(
            runCatching { backend.rankedValidators(64) }
                .isFailure
        )
    }

    @Test
    fun `root metrics use the collection endpoint`() = runBlocking {
        val api = Api("max-age=60")
        api.root = BittensorRootValidatorRemote(emptyList(), rootMeta())
        val urls = InfrastructureUrls { "https://infra.test/".toHttpUrl() }
        val backend = BittensorValidatorBackend(api, urls)
        assertTrue(backend.rootValidators().items!!.isEmpty())
        assertEquals(listOf("https://infra.test/v1/bittensor/root/validators"), api.requests)
    }

    @Test
    fun `root yield is annual percent already net of validator take`() = runBlocking {
        val address = "5".repeat(48)
        val api = Api("max-age=60")
        api.root = BittensorRootValidatorRemote(
            listOf(
                BittensorRootValidatorRemote.Validator(
                    address,
                    "Synthetic",
                    BittensorRootValidatorRemote.Yield(
                        BittensorRootValidatorRemote.Window("7.25")
                    ),
                    "100"
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
            listOf(BittensorRootValidatorRemote.Validator(chain.addressOf(hotkey.value), "Synthetic", api.root!!.items!!.single().stakeYield, "100")),
            rootMeta()
        )
        val source = RemoteBittensorRootValidatorSource(backend, validators, logger)
        assertEquals(BigDecimal("7.25"), source.annualPercentageYield(chain, validator))
    }

    @Test
    fun `invalid ranked payload cannot poison cache or advance generation`() = runBlocking {
        val api = Api("max-age=60")
        val urls = InfrastructureUrls { "https://infra.test/".toHttpUrl() }
        val backend = BittensorValidatorBackend(api, urls)
        api.rankedDocument = BittensorRankedValidatorsRemote(policy("FRESH", block = 200).meta, null)

        assertTrue(runCatching { backend.rankedValidators(64) }.isFailure)
        api.rankedDocument = BittensorRankedValidatorsRemote(policy("FRESH", block = 100).meta, emptyList())
        assertTrue(backend.rankedValidators(64).items!!.isEmpty())
        assertEquals(2, api.requests.size)
    }

    @Test
    fun `malformed ranked gates cannot be cached`() = runBlocking {
        val api = Api("max-age=60")
        val urls = InfrastructureUrls { "https://infra.test/".toHttpUrl() }
        val backend = BittensorValidatorBackend(api, urls)
        val validMeta = requireNotNull(policy("FRESH").meta)
        val invalidGates = BittensorRecommendationsRemote.Gates("invalid", true, true)
        val invalidMeta = BittensorRecommendationsRemote.Meta(invalidGates, validMeta.components, validMeta.generation)
        api.rankedDocument = BittensorRankedValidatorsRemote(invalidMeta, emptyList())

        assertTrue(runCatching { backend.rankedValidators(64) }.isFailure)
        api.rankedDocument = BittensorRankedValidatorsRemote(validMeta, emptyList())
        backend.rankedValidators(64)
        assertEquals(2, api.requests.size)
    }

    @Test
    fun `invalid root payload can recover before TTL`() = runBlocking {
        val api = Api("max-age=60")
        val urls = InfrastructureUrls { "https://infra.test/".toHttpUrl() }
        val backend = BittensorValidatorBackend(api, urls)
        val address = "5".repeat(48)
        api.root = BittensorRootValidatorRemote(null, rootMeta())

        assertTrue(runCatching { backend.rootValidators() }.isFailure)
        api.root = BittensorRootValidatorRemote(listOf(BittensorRootValidatorRemote.Validator(address, "Correct", null, "100")), rootMeta())
        assertEquals(address, backend.rootValidators().items?.single()?.hotkey)
        assertEquals(2, api.requests.size)
    }

    @Test
    fun `newer cross route generation invalidates ranked cache and rejects regression`() = runBlocking {
        val api = Api("max-age=60")
        val urls = InfrastructureUrls { "https://infra.test/".toHttpUrl() }
        val backend = BittensorValidatorBackend(api, urls)
        api.rankedDocument = BittensorRankedValidatorsRemote(policy("FRESH", block = 100).meta, emptyList())
        backend.rankedValidators(64)
        api.recommendationsDocument = policy("FRESH", block = 101)
        backend.gates()

        assertTrue(runCatching { backend.rankedValidators(64) }.isFailure)
        api.rankedDocument = BittensorRankedValidatorsRemote(policy("FRESH", block = 101).meta, emptyList())
        backend.rankedValidators(64)
        assertEquals(4, api.requests.size)
    }

    @Test
    fun `newer ranking invalidates global recommendations and policy`() = runBlocking {
        val api = Api("max-age=60")
        val urls = InfrastructureUrls { "https://infra.test/".toHttpUrl() }
        val backend = BittensorValidatorBackend(api, urls)
        api.recommendationsDocument = recommendations("FRESH")
        backend.recommendedValidators()
        backend.gates()
        api.rankedDocument = BittensorRankedValidatorsRemote(policy("FRESH", block = 124).meta, emptyList())
        backend.rankedValidators(64)

        assertTrue(runCatching { backend.recommendedValidators() }.isFailure)
        assertTrue(runCatching { backend.gates() }.isFailure)
        assertEquals(5, api.requests.size)
    }

    @Test
    fun `generation compares timestamps first and resets on origin change`() = runBlocking {
        val api = Api("no-store")
        var base = "https://infra.test/".toHttpUrl()
        val urls = InfrastructureUrls { base }
        val backend = BittensorValidatorBackend(api, urls)
        api.recommendationsDocument = policy("FRESH", block = 200, asOf = "2026-10-19T00:00:00.100001Z")
        backend.gates()
        api.recommendationsDocument = policy("FRESH", block = 201, asOf = "2026-10-19T00:00:00.100000Z")
        assertTrue(runCatching { backend.gates() }.isFailure)
        base = "https://other.test/".toHttpUrl()
        backend.gates()
        assertEquals(3, api.requests.size)
    }

    @Test
    fun `missing or invalid generation timestamp cannot be cached`() = runBlocking {
        val api = Api("max-age=60")
        val urls = InfrastructureUrls { "https://infra.test/".toHttpUrl() }
        val backend = BittensorValidatorBackend(api, urls)
        api.recommendationsDocument = policy("FRESH", asOf = "2026-02-30T00:00:00Z")
        assertTrue(runCatching { backend.gates() }.isFailure)
        val metadata = BittensorRecommendationsRemote.Meta(
            BittensorRecommendationsRemote.Gates("0.18", true, true),
            BittensorRecommendationsRemote.Components(BittensorRecommendationsRemote.Component("AVAILABLE", "FRESH")),
            BittensorRecommendationsRemote.Generation(BigInteger.ONE)
        )
        api.rankedDocument = BittensorRankedValidatorsRemote(metadata, emptyList())
        assertTrue(runCatching { backend.rankedValidators(1) }.isFailure)
        api.rankedDocument = BittensorRankedValidatorsRemote(policy("FRESH").meta, emptyList())
        backend.rankedValidators(1)
        assertEquals(3, api.requests.size)
    }

    @Test
    fun `validator cache deadline includes time spent fetching`() = runBlocking {
        var now = 0L
        val clock = { now }
        val api = Api("max-age=30")
        api.beforeResponse = { now += 20_000_000_000L }
        val urls = InfrastructureUrls { "https://infra.test/".toHttpUrl() }
        val backend = BittensorValidatorBackend(api, urls, clock)

        backend.validators(1)
        now = 29_000_000_000L
        backend.validators(1)
        assertEquals(1, api.requests.size)
        now = 30_000_000_000L
        backend.validators(1)
        assertEquals(2, api.requests.size)
    }

    @Test
    fun `ranked root and policy caches use request start deadlines`() = runBlocking {
        var now = 0L
        val clock = { now }
        val api = Api("max-age=30")
        api.beforeResponse = { now += 31_000_000_000L }
        val urls = InfrastructureUrls { "https://infra.test/".toHttpUrl() }
        val backend = BittensorValidatorBackend(api, urls, clock)
        val address = "5".repeat(48)
        api.root = BittensorRootValidatorRemote(listOf(BittensorRootValidatorRemote.Validator(address, null, null, "100")), rootMeta())
        api.recommendationsDocument = recommendations("FRESH")

        repeat(2) { backend.rankedValidators(1) }
        repeat(2) { backend.rootValidators() }
        repeat(2) { backend.gates() }
        repeat(2) { backend.recommendedValidators() }
        assertEquals(8, api.requests.size)
    }

    @Test
    fun `root yield ignores legacy h24 and validators without positive root stake`() = runBlocking {
        val api = Api("no-store")
        val urls = InfrastructureUrls { "https://infra.test/".toHttpUrl() }
        val backend = BittensorValidatorBackend(api, urls)
        val validators = org.mockito.Mockito.mock(BittensorSubnetValidatorRepository::class.java)
        val logger = org.mockito.Mockito.mock(io.novasama.substrate_sdk_android.wsrpc.logging.Logger::class.java)
        val chain = org.mockito.Mockito.mock(io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain::class.java)
        org.mockito.Mockito.`when`(chain.addressPrefix).thenReturn(42)
        val hotkey = io.novafoundation.nova.common.address.AccountIdKey(ByteArray(32) { 1 })
        val validator = io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorValidator(hotkey, "Validator")
        val source = RemoteBittensorRootValidatorSource(backend, validators, logger)
        val address = chain.addressOf(hotkey.value)
        val yield = gson.fromJson("""{"h24":{"apy":"99"},"d30":{"apy":"7.25"}}""", BittensorRootValidatorRemote.Yield::class.java)
        for (stake in listOf(null, "0", "-1", "100")) {
            val row = BittensorRootValidatorRemote.Validator(address, null, yield, stake)
            api.root = BittensorRootValidatorRemote(listOf(row), rootMeta())
            assertEquals(if (stake == "100") BigDecimal("7.25") else null, source.annualPercentageYield(chain, validator))
        }
        val legacyYield = gson.fromJson("""{"h24":{"apy":"99"}}""", BittensorRootValidatorRemote.Yield::class.java)
        val row = BittensorRootValidatorRemote.Validator(address, null, legacyYield, "100")
        api.root = BittensorRootValidatorRemote(listOf(row), rootMeta())
        assertEquals(null, source.annualPercentageYield(chain, validator))
    }

    @Test
    fun `network root rate uses d30 overview and rejects stale or malformed data before cache`() = runBlocking {
        val api = Api("max-age=60")
        val urls = InfrastructureUrls { "https://infra.test/".toHttpUrl() }
        val backend = BittensorValidatorBackend(api, urls)
        api.overview = overview("STALE", "8.5")
        assertTrue(runCatching { backend.networkRootRate() }.isFailure)
        api.overview = overview("FRESH", "invalid")
        assertTrue(runCatching { backend.networkRootRate() }.isFailure)
        api.overview = overview("FRESH", "8.5")
        assertEquals(BigDecimal("0.085"), backend.networkRootRate())
        assertEquals(BigDecimal("0.085"), backend.networkRootRate())
        assertEquals(List(3) { "https://infra.test/v1/bittensor/root/overview" }, api.requests)
    }

    @Test
    fun `root overview deadline includes fetch latency and resets with origin`() = runBlocking {
        val api = Api("max-age=30")
        var now = 0L
        var base = "https://infra.test/".toHttpUrl()
        val urls = InfrastructureUrls { base }
        val backend = BittensorValidatorBackend(api, urls) { now }
        api.overview = overview("FRESH", "8.5")
        api.beforeResponse = { now += 31_000_000_000L }
        repeat(2) { backend.networkRootRate() }
        assertEquals(2, api.requests.size)
        api.beforeResponse = {}
        backend.networkRootRate()
        base = "https://other.test/".toHttpUrl()
        backend.networkRootRate()
        assertEquals("https://other.test/v1/bittensor/root/overview", api.requests.last())
    }

    private fun overview(freshness: String, apy: String): BittensorRootOverviewRemote {
        val window = BittensorRootValidatorRemote.Window(apy)
        val yield = BittensorRootValidatorRemote.Yield(window)
        val stakeIndex = BittensorRootOverviewRemote.StakeIndex(yield)
        val component = BittensorRecommendationsRemote.Component("AVAILABLE", freshness, "2026-10-19T00:00:00Z")
        val components = BittensorRootOverviewRemote.Components(component)
        val meta = BittensorRootOverviewRemote.Meta(components)
        return BittensorRootOverviewRemote(stakeIndex, meta)
    }

    @Test
    fun `root yield window freshness is required independently of directory freshness`() = runBlocking {
        val api = Api("max-age=60")
        val urls = InfrastructureUrls { "https://infra.test/".toHttpUrl() }
        val backend = BittensorValidatorBackend(api, urls)
        val fresh = BittensorRecommendationsRemote.Component("AVAILABLE", "FRESH", "2026-10-19T00:00:00Z")
        val stale = BittensorRecommendationsRemote.Component("AVAILABLE", "STALE", "2026-10-19T00:00:00Z")
        val components = BittensorRootValidatorRemote.Components(fresh, stale)
        val meta = BittensorRootValidatorRemote.Meta(components)
        api.root = BittensorRootValidatorRemote(emptyList(), meta)
        assertTrue(runCatching { backend.rootValidators() }.isFailure)
        api.root = BittensorRootValidatorRemote(emptyList(), rootMeta())
        assertTrue(backend.rootValidators().items!!.isEmpty())
        assertEquals(2, api.requests.size)
    }

    @Test
    fun `network rate rejects negative yield and preserves missing index as unavailable`() = runBlocking {
        val api = Api("no-store")
        val urls = InfrastructureUrls { "https://infra.test/".toHttpUrl() }
        val backend = BittensorValidatorBackend(api, urls)
        api.overview = overview("FRESH", "-1")
        assertEquals(null, backend.networkRootRate())
        api.overview = BittensorRootOverviewRemote(null, api.overview!!.meta)
        assertEquals(null, backend.networkRootRate())
        api.overview = overview("FRESH", "0")
        assertEquals(BigDecimal("0.00"), backend.networkRootRate())
    }

    private fun rootMeta() = BittensorRootValidatorRemote.Meta(
        BittensorRootValidatorRemote.Components(
            BittensorRecommendationsRemote.Component("AVAILABLE", "FRESH", "2026-10-19T00:00:00Z"),
            BittensorRecommendationsRemote.Component("AVAILABLE", "FRESH", "2026-10-19T00:00:00Z")
        )
    )

    @Test
    fun `subnet facts reuse fresh policy response and select exact subnet`() = runBlocking {
        val api = Api("max-age=60")
        val urls = InfrastructureUrls { "https://infra.test/".toHttpUrl() }
        val backend = BittensorValidatorBackend(api, urls)
        val row = BittensorSubnetRankingRemote(1, "26000", 1000.toBigInteger(), null)
        api.recommendationsDocument = BittensorRecommendationsRemote(policy("FRESH").meta, items = listOf(row))
        backend.gates()
        assertEquals(row, backend.rankedSubnet(io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorNetuid.Subnet(1), 1.toBigInteger()))
        assertEquals(null, backend.rankedSubnet(io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorNetuid.Subnet(2), 1.toBigInteger()))
        assertEquals(1, api.requests.size)
    }

    @Test
    fun `subnet facts reject stale and previous incarnation data`() = runBlocking {
        val api = Api("no-store")
        val urls = InfrastructureUrls { "https://infra.test/".toHttpUrl() }
        val backend = BittensorValidatorBackend(api, urls)
        val subnet = io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorNetuid.Subnet(1)
        api.recommendationsDocument = BittensorRecommendationsRemote(policy("STALE").meta, items = emptyList())
        assertTrue(runCatching { backend.rankedSubnet(subnet, 1.toBigInteger()) }.isFailure)
        api.recommendationsDocument = BittensorRecommendationsRemote(policy("FRESH").meta, items = emptyList())
        assertTrue(runCatching { backend.rankedSubnet(subnet, 124.toBigInteger()) }.isFailure)
        assertEquals(null, backend.rankedSubnet(subnet, 123.toBigInteger()))
    }

    private class Api(private val cacheControl: String) : BittensorValidatorsApi {

        val requests = mutableListOf<String>()
        var rankedFreshness = "FRESH"
        var rankedDocument: BittensorRankedValidatorsRemote? = null
        var beforeResponse: () -> Unit = {}
        var root: BittensorRootValidatorRemote? = null
        var overview: BittensorRootOverviewRemote? = null

        var recommendationsDocument = policy("FRESH")

        override suspend fun validators(url: String): Response<BittensorValidatorsRemote> {
            requests += url
            beforeResponse()
            return Response.success(BittensorValidatorsRemote(emptyList()), headers())
        }
        override suspend fun recommendations(url: String): Response<BittensorRecommendationsRemote> {
            requests += url
            beforeResponse()
            return Response.success(recommendationsDocument, headers())
        }
        override suspend fun rootValidators(url: String): Response<BittensorRootValidatorRemote> {
            requests += url
            beforeResponse()
            return root?.let { Response.success(it, headers()) } ?: Response.error(503, "".toResponseBody())
        }
        override suspend fun rootOverview(url: String): Response<BittensorRootOverviewRemote> {
            requests += url
            beforeResponse()
            return overview?.let { Response.success(it, headers()) } ?: Response.error(503, "".toResponseBody())
        }
        override suspend fun rankedValidators(url: String): Response<BittensorRankedValidatorsRemote> {
            requests += url
            beforeResponse()
            return Response.success(
                rankedDocument ?: BittensorRankedValidatorsRemote(
                    policy(rankedFreshness).meta,
                    emptyList()
                ),
                headers()
            )
        }
        private fun headers() = Headers.Builder()
            .add("Cache-Control", cacheControl)
            .build()
    }

    private fun recommendations(freshness: String): BittensorRecommendationsRemote = gson.fromJson(
        """{
            "meta":{
                "components":{"recommendations":{"availability":"AVAILABLE","freshness":"$freshness","asOf":"2026-10-19T00:00:00Z"}},
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
        private fun policy(
            freshness: String,
            block: Long = 123,
            asOf: String = "2026-10-19T00:00:00Z"
        ) = BittensorRecommendationsRemote(
            BittensorRecommendationsRemote.Meta(
                BittensorRecommendationsRemote.Gates("0.18", true, true),
                BittensorRecommendationsRemote.Components(BittensorRecommendationsRemote.Component("AVAILABLE", freshness, asOf)),
                BittensorRecommendationsRemote.Generation(block.toBigInteger())
            )
        )
    }
}
