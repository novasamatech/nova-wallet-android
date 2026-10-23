package io.novafoundation.nova.feature_staking_impl.data.bittensor

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorNetuid
import io.novafoundation.nova.infrastructure.InfrastructureUrls
import io.novafoundation.nova.runtime.ext.addressOf
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.network.rpc.RpcCalls
import io.novafoundation.nova.runtime.storage.source.StorageDataSource
import io.novafoundation.nova.runtime.storage.source.query.StorageQueryContext
import java.math.BigDecimal
import java.math.BigInteger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.ResponseBody.Companion.toResponseBody
import io.novafoundation.nova.common.domain.dataOrNull
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorValidatorDirectory
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.BittensorRetryController
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.subnet.observeBittensorValidatorDirectory
import io.novasama.substrate_sdk_android.wsrpc.logging.Logger
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorValidatorDirectoryStatus.Availability
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorValidatorDirectoryStatus.Completeness
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorValidatorDirectoryStatus.Freshness
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import retrofit2.Response

class BittensorSubnetValidatorRepositoryTest {

    private val api = mock(BittensorValidatorsApi::class.java)

    private val urls = InfrastructureUrls { "https://infra.test/".toHttpUrl() }

    private val backend = BittensorValidatorBackend(api, urls)

    private val chain = mock(Chain::class.java)

    private val rpc = mock(RpcCalls::class.java)

    private val hotkey = AccountIdKey(ByteArray(32) { 1 })

    private val storageDelegate = mock(StorageDataSource::class.java)

    private val storage = SnapshotStorage(storageDelegate)

    private val repository = RealBittensorSubnetValidatorRepository(backend, storage, rpc)

    private val subnet = BittensorNetuid.Subnet(64)

    @Test
    fun `ranking 503 leaves directory and chain take available but disables selection`() = runBlocking {
        val metadata = directoryMetadata()
        prepare(metadata)
        `when`(api.rankedValidators(RANKING_URL)).thenReturn(Response.error(503, "".toResponseBody()))

        val directory = repository.directory(chain, subnet)

        val row = directory.validators.single()
        assertEquals("Directory name", row.validator.name)
        assertEquals(BigDecimal("42"), row.validator.reportedStake)
        assertEquals(BigDecimal.ZERO, row.commissionRate.stripTrailingZeros())
        assertFalse(row.selectable)
        assertFalse(row.recommended)
        assertNotNull(directory.recommendationFailure)
        assertEquals(Completeness.COMPLETE, directory.status.completeness)
        assertEquals(Availability.UNAVAILABLE, directory.status.metagraph.availability)
        assertEquals("source_not_supported", directory.status.metagraph.availabilityReason)
        assertEquals("0xblock", storage.requestedHash)
    }

    @Test
    fun `missing subnet UID remains visibly inactive during policy recovery without authorizing stake`() = runBlocking {
        prepare(directoryMetadata())
        storage.snapshot = storage.snapshot.copy(uids = emptyMap())
        `when`(api.rankedValidators(RANKING_URL)).thenReturn(Response.error(503, "".toResponseBody()))

        val row = repository.directory(chain, subnet).validators.single()
        assertTrue(row.inactive)
        assertFalse(row.canSelect)
        assertFalse(row.canAddToExistingPosition)
    }

    @Test
    fun `list consumer receives ranking failure and can retry successfully`() = runBlocking {
        prepare(directoryMetadata())
        `when`(api.rankedValidators(RANKING_URL)).thenReturn(Response.error(503, "".toResponseBody()))

        val failure = runCatching { repository.validators(chain, subnet) }.exceptionOrNull()

        assertNotNull(failure)
        assertFalse(failure is CancellationException)
        prepare(directoryMetadata())
        assertTrue(repository.validators(chain, subnet).single().selectable)
    }

    @Test
    fun `recommendations before registration cannot name or recommend a validator`() = runBlocking {
        prepare(directoryMetadata())
        storage.snapshot = storage.snapshot.copy(registeredAt = 124.toBigInteger())

        val directory = repository.directory(chain, subnet)

        assertNotNull(directory.recommendationFailure)
        assertEquals("Directory name", directory.validators.single().validator.name)
        assertFalse(directory.validators.single().recommended)
        assertFalse(directory.validators.single().selectable)
    }

    @Test
    fun `generation at registration is usable and resource rows are not netuid filtered`() = runBlocking {
        prepare(directoryMetadata())
        storage.snapshot = storage.snapshot.copy(registeredAt = 123.toBigInteger())

        val directory = repository.directory(chain, subnet)

        assertNull(directory.recommendationFailure)
        assertEquals("Ranked name", directory.validators.single().validator.name)
        assertTrue(directory.validators.single().recommended)
        assertTrue(directory.validators.single().selectable)
    }

    @Test
    fun `component stale directory hides stake and retains partial metadata`() = runBlocking {
        val metadata = directoryMetadata(freshness = "STALE", completeness = "PARTIAL")
        prepare(metadata)

        val directory = repository.directory(chain, subnet)

        assertNull(directory.validators.single().validator.reportedStake)
        assertEquals(Completeness.PARTIAL, directory.status.completeness)
        assertEquals(Freshness.STALE, directory.status.stakes.freshness)
        assertEquals(parseBittensorTimestamp(AS_OF), directory.status.stakes.asOfEpochSeconds)
        assertTrue(directory.validators.single().selectable)
    }

    @Test
    fun `missing directory metadata never turns reported stake into fresh data`() = runBlocking {
        prepare(null)

        val directory = repository.directory(chain, subnet)

        assertEquals(Completeness.UNKNOWN, directory.status.completeness)
        assertEquals(Availability.UNKNOWN, directory.status.stakes.availability)
        assertEquals(Freshness.UNKNOWN, directory.status.stakes.freshness)
        assertNull(directory.status.stakes.asOfEpochSeconds)
        assertNull(directory.validators.single().validator.reportedStake)
    }

    @Test
    fun `cancellation is not converted to an unavailable recommendation`() = runBlocking {
        prepare(directoryMetadata())
        `when`(api.rankedValidators(RANKING_URL)).thenThrow(CancellationException("Cancelled"))

        assertTrue(runCatching { repository.directory(chain, subnet) }.exceptionOrNull() is CancellationException)
        assertNull(storage.requestedHash)
    }

    @Test
    fun `selector retains directory during ranking failure and dialog retry without reloading directory`() = runBlocking<Unit> {
        prepare(directoryMetadata())
        `when`(api.rankedValidators(RANKING_URL)).thenReturn(Response.error(503, "".toResponseBody()))
        val resources = mock(ResourceManager::class.java)
        val logger = mock(Logger::class.java)
        val controller = BittensorRetryController(resources, logger)
        `when`(resources.getString(R.string.common_error_general_title)).thenReturn("Error")
        `when`(resources.getString(R.string.common_retry_message)).thenReturn("Retry")
        val retryStarted = CompletableDeferred<Unit>()
        val finishRetry = CompletableDeferred<Unit>()
        val recovered = CompletableDeferred<Unit>()
        val directoryVisible = CompletableDeferred<Unit>()
        val directories = mutableListOf<BittensorValidatorDirectory>()
        var rankingAttempts = 0
        val collection = launch {
            observeBittensorValidatorDirectory(
                retryController = controller,
                loadDirectory = { repository.unrankedDirectory(chain, subnet) },
                rankDirectory = { directory ->
                    rankingAttempts++
                    if (rankingAttempts == 2) {
                        retryStarted.complete(Unit)
                        finishRetry.await()
                    }
                    repository.rankDirectory(chain, subnet, directory)
                }
            ).collect { state ->
                state.dataOrNull?.let { directory ->
                    directories.add(directory)
                    if (directory.validators.isNotEmpty()) directoryVisible.complete(Unit)
                    if (directory.validators.single().selectable) recovered.complete(Unit)
                }
            }
        }

        try {
            withTimeout(3_000L) {
                val retry = controller.events.first()
                directoryVisible.await()
                assertEquals("Directory name", directories.last().validators.single().validator.name)
                assertFalse(directories.last().validators.single().selectable)
                prepare(directoryMetadata())
                retry.onRetry()
                retryStarted.await()
                assertFalse(directories.last().validators.single().selectable)
                assertTrue(directories.all { it.validators.isNotEmpty() })
                finishRetry.complete(Unit)
                recovered.await()
            }
            assertEquals("Ranked name", directories.last().validators.single().validator.name)
            assertTrue(directories.last().validators.single().recommended)
            verify(api, times(1)).validators(DIRECTORY_URL)
            verify(api, times(2)).rankedValidators(RANKING_URL)
        } finally {
            collection.cancelAndJoin()
        }
    }

    @Test
    fun `root rows carry the fresh root stake and list only root-registered hotkeys`() = runBlocking<Unit> {
        prepareRoot(rootFreshness = "FRESH")

        val unranked = repository.unrankedDirectory(chain, BittensorNetuid.Root)
        val ranked = repository.directory(chain, BittensorNetuid.Root)

        listOf(unranked, ranked).forEach { directory ->
            val row = directory.validators.single()
            assertEquals(hotkey, row.validator.hotkey)
            assertEquals(BigDecimal("1028782.5"), row.validator.reportedStake)
        }
        assertEquals(unranked.validators.size, ranked.validators.size)
    }

    @Test
    fun `stale or failing root stake leaves the root directory readable without an amount`() = runBlocking<Unit> {
        prepareRoot(rootFreshness = "STALE")
        assertNull(repository.unrankedDirectory(chain, BittensorNetuid.Root).validators.single().validator.reportedStake)

        val failing = RealBittensorSubnetValidatorRepository(BittensorValidatorBackend(api, urls), storage, rpc)
        `when`(api.rootValidators(ROOT_VALIDATORS_URL)).thenReturn(Response.error(503, "".toResponseBody()))
        assertNull(failing.unrankedDirectory(chain, BittensorNetuid.Root).validators.single().validator.reportedStake)
    }

    private suspend fun prepareRoot(rootFreshness: String) {
        prepare(directoryMetadata())
        val unregistered = AccountIdKey(ByteArray(32) { 2 })
        val rows = listOf(hotkey, unregistered).map { key ->
            BittensorValidatorsRemote.Item(0, chain.addressOf(key), "Root ${key.value[0]}", null, null)
        }
        `when`(api.validators(ROOT_DIRECTORY_URL)).thenReturn(Response.success(BittensorValidatorsRemote(rows, directoryMetadata())))
        val ranking = api.rankedValidators(RANKING_URL).body()!!
        `when`(api.rankedValidators(ROOT_RANKING_URL)).thenReturn(Response.success(ranking))
        val component = BittensorRecommendationsRemote.Component("AVAILABLE", rootFreshness, AS_OF)
        val rootRows = listOf(
            BittensorRootValidatorRemote.Validator(chain.addressOf(hotkey), "Root", null, "1028782.5"),
            BittensorRootValidatorRemote.Validator(chain.addressOf(unregistered), "Unregistered", null, "5")
        )
        val meta = BittensorRootValidatorRemote.Meta(BittensorRootValidatorRemote.Components(component, component))
        `when`(api.rootValidators(ROOT_VALIDATORS_URL)).thenReturn(Response.success(BittensorRootValidatorRemote(rootRows, meta)))
        storage.snapshot = storage.snapshot.copy(
            uids = mapOf(hotkey to 0),
            commissionsInU16 = mapOf(hotkey to BigInteger.ZERO, unregistered to BigInteger.ZERO)
        )
    }

    private suspend fun prepare(metadata: BittensorValidatorsRemote.Meta?) {
        `when`(chain.id).thenReturn("chain")
        `when`(chain.addressPrefix).thenReturn(42)
        `when`(rpc.getBlockHash("chain")).thenReturn("0xblock")
        val address = chain.addressOf(hotkey)
        // Row netuid filtering is owned by the backend resource contract, not this client mapper.
        val row = BittensorValidatorsRemote.Item(999, address, "Directory name", null, BittensorValidatorsRemote.ReportedMeasurements("42"))
        val response = BittensorValidatorsRemote(listOf(row), metadata)
        `when`(api.validators(DIRECTORY_URL)).thenReturn(Response.success(response))
        val component = BittensorRecommendationsRemote.Component("AVAILABLE", "FRESH", AS_OF)
        val components = BittensorRecommendationsRemote.Components(component)
        val gates = BittensorRecommendationsRemote.Gates("0.18", true, true)
        val generation = BittensorRecommendationsRemote.Generation(123.toBigInteger(), emptyList())
        val rankingMeta = BittensorRecommendationsRemote.Meta(gates, components, generation)
        val ranked = BittensorRankedValidatorsRemote.Item(999, address, "Ranked name", true, "balanced", null)
        val ranking = BittensorRankedValidatorsRemote(rankingMeta, listOf(ranked))
        `when`(api.rankedValidators(RANKING_URL)).thenReturn(Response.success(ranking))
        storage.snapshot = BittensorValidatorSnapshot(
            uids = mapOf(hotkey to 0),
            commissionsInU16 = mapOf(hotkey to BigInteger.ZERO),
            permits = listOf(true),
            lastUpdates = listOf(200.toBigInteger()),
            activeThreshold = 100.toBigInteger(),
            registeredAt = 100.toBigInteger()
        )
    }

    private fun directoryMetadata(freshness: String = "FRESH", completeness: String = "COMPLETE"): BittensorValidatorsRemote.Meta {
        val stake = BittensorRecommendationsRemote.Component("AVAILABLE", freshness, AS_OF)
        val identity = BittensorRecommendationsRemote.Component("AVAILABLE", "FRESH", AS_OF)
        val metagraph = BittensorRecommendationsRemote.Component("UNAVAILABLE", null, availabilityReason = "source_not_supported")
        val components = BittensorValidatorsRemote.Components(stake, metagraph, identity)
        return BittensorValidatorsRemote.Meta(completeness, components)
    }

    private class SnapshotStorage(delegate: StorageDataSource) : StorageDataSource by delegate {

        lateinit var snapshot: BittensorValidatorSnapshot

        var requestedHash: String? = null

        @Suppress("UNCHECKED_CAST")
        override suspend fun <R> query(
            chainId: String,
            at: String?,
            applyStorageDefault: Boolean,
            query: suspend StorageQueryContext.() -> R
        ): R {
            requestedHash = at
            return snapshot as R
        }
    }

    private companion object {
        const val AS_OF = "2026-10-19T00:00:00Z"
        const val DIRECTORY_URL = "https://infra.test/v1/bittensor/subnets/64/validators"
        const val RANKING_URL = "https://infra.test/v1/bittensor/recommendations/subnets/64/validators"
        const val ROOT_DIRECTORY_URL = "https://infra.test/v1/bittensor/subnets/0/validators"
        const val ROOT_RANKING_URL = "https://infra.test/v1/bittensor/recommendations/subnets/0/validators"
        const val ROOT_VALIDATORS_URL = "https://infra.test/v1/bittensor/root/validators"
    }
}
