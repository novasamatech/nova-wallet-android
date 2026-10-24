package io.novafoundation.nova.feature_staking_impl.data.bittensor

import io.novafoundation.nova.common.utils.sequrity.AutomaticInteractionGate
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
import io.novafoundation.nova.common.domain.ExtendedLoadingState
import io.novafoundation.nova.common.domain.dataOrNull
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorValidatorDirectory
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.BittensorRetryController
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.subnet.observeBittensorValidatorDirectory
import io.novasama.substrate_sdk_android.wsrpc.logging.Logger
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import org.mockito.Mockito.never
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

    private val unregistered = AccountIdKey(ByteArray(32) { 2 })

    private val storageDelegate = mock(StorageDataSource::class.java)

    private val storage = SnapshotStorage(storageDelegate)

    private val repository = RealBittensorSubnetValidatorRepository(backend, storage, rpc)

    private val subnet = BittensorNetuid.Subnet(64)

    @Test
    fun `ranking 503 leaves the directory, its chain take and its selection available`() = runBlocking {
        val metadata = directoryMetadata()
        prepare(metadata)
        `when`(api.rankedValidators(RANKING_URL)).thenReturn(Response.error(503, "".toResponseBody()))

        val directory = repository.directory(chain, subnet)

        val row = directory.validators.single()
        assertEquals("Directory name", row.validator.name)
        assertEquals(BigDecimal("42"), row.validator.reportedStake)
        assertEquals(BigDecimal.ZERO, row.commissionRate.stripTrailingZeros())
        assertTrue(row.canSelect)
        assertFalse(row.recommended)
        assertNotNull(directory.recommendationFailure)
        assertEquals(Completeness.COMPLETE, directory.status.completeness)
        assertEquals(Availability.UNAVAILABLE, directory.status.metagraph.availability)
        assertEquals("source_not_supported", directory.status.metagraph.availabilityReason)
        assertEquals("0xblock", storage.requestedHash)
    }

    @Test
    fun `missing subnet UID remains visibly inactive during policy recovery without authorizing a new stake`() = runBlocking {
        prepare(directoryMetadata())
        storage.snapshot = storage.snapshot.copy(uids = emptyMap())
        `when`(api.rankedValidators(RANKING_URL)).thenReturn(Response.error(503, "".toResponseBody()))

        val row = repository.directory(chain, subnet).validators.single()
        assertTrue(row.inactive)
        assertFalse(row.canSelect)
        assertTrue(row.canAddToExistingPosition)
    }

    @Test
    fun `list consumer keeps selectable rows when the ranking fails`() = runBlocking {
        prepare(directoryMetadata())
        `when`(api.rankedValidators(RANKING_URL)).thenReturn(Response.error(503, "".toResponseBody()))

        val row = repository.validators(chain, subnet).single()

        assertTrue(row.canSelect)
        assertFalse(row.recommended)
    }

    @Test
    fun `recommendations before registration neither name nor recommend but keep the selection gates`() = runBlocking {
        prepare(directoryMetadata())
        storage.snapshot = storage.snapshot.copy(registeredAt = 124.toBigInteger())

        val directory = repository.directory(chain, subnet)

        assertNull(directory.recommendationFailure)
        assertEquals("Directory name", directory.validators.single().validator.name)
        assertFalse(directory.validators.single().recommended)
        assertTrue(directory.validators.single().selectable)
    }

    @Test
    fun `chain detail names a listed validator and ignores the recommendation gates`() = runBlocking {
        prepare(directoryMetadata())
        storage.snapshot = storage.snapshot.copy(commissionsInU16 = mapOf(hotkey to 65_535.toBigInteger()), permits = listOf(false))

        val validator = repository.chainValidator(chain, subnet, hotkey)

        assertEquals("Directory name", validator.validator.name)
        assertTrue(validator.inactive)
        assertEquals(BigDecimal.ONE, validator.commissionRate.stripTrailingZeros())
        `when`(api.validators(DIRECTORY_URL)).thenReturn(Response.error(503, "".toResponseBody()))
        storage.snapshot = storage.snapshot.copy(permits = listOf(true))
        val unnamed = repository.chainValidator(chain, subnet, hotkey)
        assertEquals(chain.addressOf(hotkey), unnamed.validator.name)
        assertTrue(unnamed.canSelect)
    }

    @Test
    fun `validator above the take limit is listed and selectable but not recommended`() = runBlocking {
        prepare(directoryMetadata())
        storage.snapshot = storage.snapshot.copy(commissionsInU16 = mapOf(hotkey to 65_535.toBigInteger()))

        val row = repository.directory(chain, subnet).validators.single()

        assertTrue(row.canSelect)
        assertFalse(row.recommended)
        repository.validate(chain, subnet, row.validator)
    }

    @Test
    fun `generation at registration is usable and resource rows are not netuid filtered`() = runBlocking {
        prepare(directoryMetadata())
        storage.snapshot = storage.snapshot.copy(registeredAt = 123.toBigInteger())

        val directory = repository.directory(chain, subnet)

        assertNull(directory.recommendationFailure)
        assertEquals("Ranked name", directory.validators.single().validator.name)
        assertTrue(directory.validators.single().recommended)
        assertEquals(0, directory.validators.single().recommendationRank)
        assertTrue(directory.validators.single().selectable)
        assertFalse(directory.staleRecommendations)
    }

    @Test
    fun `stale generation still recommends and marks the directory as outdated`() = runBlocking {
        prepare(directoryMetadata(), rankingFreshness = "STALE")

        val directory = repository.directory(chain, subnet)

        assertTrue(directory.validators.single().recommended)
        assertTrue(directory.staleRecommendations)
        assertTrue(repository.rankDirectory(chain, subnet, repository.unrankedDirectory(chain, subnet)).staleRecommendations)
    }

    @Test
    fun `component stale directory keeps its usable stake and partial metadata`() = runBlocking {
        val metadata = directoryMetadata(freshness = "STALE", completeness = "PARTIAL")
        prepare(metadata)

        val directory = repository.directory(chain, subnet)

        assertEquals(BigDecimal("42"), directory.validators.single().validator.reportedStake)
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
    fun `without the unusual APY filter the directory stays selectable through a ranking failure and its retry`() = runBlocking<Unit> {
        prepare(directoryMetadata())
        `when`(api.rankedValidators(RANKING_URL)).thenReturn(Response.error(503, "".toResponseBody()))
        val resources = mock(ResourceManager::class.java)
        val logger = mock(Logger::class.java)
        val controller = BittensorRetryController(resources, logger, mock(AutomaticInteractionGate::class.java))
        `when`(resources.getString(R.string.common_error_general_title)).thenReturn("Error")
        `when`(resources.getString(R.string.bittensor_load_failed_message)).thenReturn("Retry")
        val retryStarted = CompletableDeferred<Unit>()
        val finishRetry = CompletableDeferred<Unit>()
        val recovered = CompletableDeferred<Unit>()
        val directoryVisible = CompletableDeferred<Unit>()
        val directories = mutableListOf<BittensorValidatorDirectory>()
        var rankingAttempts = 0
        val collection = launch {
            observeBittensorValidatorDirectory(
                retryController = controller,
                hideUnusualApy = flowOf(false),
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
                    if (directory.validators.single().recommended) recovered.complete(Unit)
                }
            }
        }

        try {
            withTimeout(3_000L) {
                val retry = controller.events.first()
                directoryVisible.await()
                assertEquals("Directory name", directories.last().validators.single().validator.name)
                assertTrue(directories.last().validators.single().canSelect)
                assertFalse(directories.last().validators.single().recommended)
                prepare(directoryMetadata())
                retry.onRetry()
                retryStarted.await()
                assertFalse(directories.last().validators.single().recommended)
                assertTrue(directories.all { it.validators.single().canSelect })
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
    fun `unusual APY filter waits for the ranking and fails with it`() = runBlocking<Unit> {
        prepare(directoryMetadata())
        `when`(api.rankedValidators(RANKING_URL)).thenReturn(Response.error(503, "".toResponseBody()))
        val resources = mock(ResourceManager::class.java)
        `when`(resources.getString(R.string.common_error_general_title)).thenReturn("Error")
        `when`(resources.getString(R.string.bittensor_load_failed_message)).thenReturn("Retry")
        val hideUnusualApy = MutableStateFlow(true)
        val states = mutableListOf<ExtendedLoadingState<BittensorValidatorDirectory>>()

        withTimeout(3_000L) {
            observeBittensorValidatorDirectory(
                retryController = BittensorRetryController(resources, mock(Logger::class.java), mock(AutomaticInteractionGate::class.java)),
                hideUnusualApy = hideUnusualApy,
                loadDirectory = { repository.unrankedDirectory(chain, subnet) },
                rankDirectory = { repository.rankDirectory(chain, subnet, it) }
            ).first { state ->
                states += state
                if (state is ExtendedLoadingState.Error) hideUnusualApy.value = false
                state is ExtendedLoadingState.Loaded
            }
        }

        assertTrue(states.takeWhile { it !is ExtendedLoadingState.Error }.all { it is ExtendedLoadingState.Loading })
        assertTrue(states.any { it is ExtendedLoadingState.Error })
        val unranked = states.last().dataOrNull!!.validators.single()
        assertTrue(unranked.canSelect)
        assertFalse(unranked.recommended)
    }

    @Test
    fun `root rows are the staked root list entries under their root list names whatever their registration`() = runBlocking<Unit> {
        prepareRoot(rootFreshness = "FRESH")

        val unranked = repository.unrankedDirectory(chain, BittensorNetuid.Root)
        val ranked = repository.directory(chain, BittensorNetuid.Root)

        listOf(unranked, ranked).forEach { directory ->
            val rows = directory.validators.associateBy { it.validator.hotkey }
            assertEquals(setOf(hotkey, unregistered), rows.keys)
            assertEquals("Root", rows.getValue(hotkey).validator.name)
            assertEquals(chain.addressOf(unregistered), rows.getValue(unregistered).validator.name)
            assertEquals(BigDecimal("1028782.5"), rows.getValue(hotkey).validator.reportedStake)
            assertEquals(BigDecimal("5"), rows.getValue(unregistered).validator.reportedStake)
        }
        val rows = ranked.validators.associateBy { it.validator.hotkey }
        assertTrue(rows.getValue(hotkey).recommended)
        assertTrue(rows.getValue(unregistered).canSelect)
        assertFalse(rows.getValue(unregistered).recommended)
        verify(api, never()).validators(ROOT_DIRECTORY_URL)
    }

    @Test
    fun `stale root stake stays usable and the root list does not need yield windows`() = runBlocking<Unit> {
        prepareRoot(rootFreshness = "STALE", yieldWindows = BittensorRecommendationsRemote.Component("UNAVAILABLE", null))

        val row = repository.unrankedDirectory(chain, BittensorNetuid.Root).validators.first { it.validator.hotkey == hotkey }

        assertEquals(BigDecimal("1028782.5"), row.validator.reportedStake)
    }

    private suspend fun prepareRoot(
        rootFreshness: String,
        yieldWindows: BittensorRecommendationsRemote.Component = BittensorRecommendationsRemote.Component("AVAILABLE", "FRESH", AS_OF)
    ) {
        prepare(directoryMetadata())
        val rankingMeta = api.rankedValidators(RANKING_URL).body()!!.meta
        val rankedRows = listOf(hotkey, unregistered).map { key ->
            BittensorRankedValidatorsRemote.Item(0, chain.addressOf(key), "Ranked name", true, "balanced", null)
        }
        `when`(api.rankedValidators(ROOT_RANKING_URL)).thenReturn(Response.success(BittensorRankedValidatorsRemote(rankingMeta, rankedRows)))
        val zeroStake = AccountIdKey(ByteArray(32) { 3 })
        val unstaked = AccountIdKey(ByteArray(32) { 4 })
        val rootRows = listOf(
            BittensorRootValidatorRemote.Validator(chain.addressOf(hotkey), "Root", null, "1028782.5"),
            BittensorRootValidatorRemote.Validator(chain.addressOf(unregistered), " ", null, "5"),
            BittensorRootValidatorRemote.Validator(chain.addressOf(zeroStake), "Zero", null, "0"),
            BittensorRootValidatorRemote.Validator(chain.addressOf(unstaked), "Unstaked", null, null)
        )
        val component = BittensorRecommendationsRemote.Component("AVAILABLE", rootFreshness, AS_OF)
        val meta = BittensorRootValidatorRemote.Meta(BittensorRootValidatorRemote.Components(component, yieldWindows))
        `when`(api.rootValidators(ROOT_VALIDATORS_URL)).thenReturn(Response.success(BittensorRootValidatorRemote(rootRows, meta)))
        storage.snapshot = storage.snapshot.copy(
            uids = mapOf(hotkey to 0),
            commissionsInU16 = listOf(hotkey, unregistered, zeroStake, unstaked).associateWith { BigInteger.ZERO }
        )
    }

    private suspend fun prepare(metadata: BittensorValidatorsRemote.Meta?, rankingFreshness: String = "FRESH") {
        `when`(chain.id).thenReturn("chain")
        `when`(chain.addressPrefix).thenReturn(42)
        `when`(rpc.getBlockHash("chain")).thenReturn("0xblock")
        val address = chain.addressOf(hotkey)
        // Row netuid filtering is owned by the backend resource contract, not this client mapper.
        val row = BittensorValidatorsRemote.Item(999, address, "Directory name", null, BittensorValidatorsRemote.ReportedMeasurements("42"))
        val response = BittensorValidatorsRemote(listOf(row), metadata)
        `when`(api.validators(DIRECTORY_URL)).thenReturn(Response.success(response))
        val component = BittensorRecommendationsRemote.Component("AVAILABLE", rankingFreshness, AS_OF)
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
