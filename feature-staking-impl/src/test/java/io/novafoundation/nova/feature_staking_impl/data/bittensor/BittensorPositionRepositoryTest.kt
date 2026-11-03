package io.novafoundation.nova.feature_staking_impl.data.bittensor

import io.novasama.substrate_sdk_android.extensions.toHexString
import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.common.di.modules.CommonModule
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorDataStatus
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorNetuid
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorRootPosition
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorStakePositions
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorSubnetPosition
import io.novafoundation.nova.runtime.repository.ChainStateRepository
import io.novasama.substrate_sdk_android.wsrpc.logging.Logger
import java.math.BigInteger
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import org.mockito.Mockito.times
import org.mockito.Mockito.verify

class BittensorPositionRepositoryTest {

    private val snapshots = createResponseTestCache()

    private val positionSource = mock(BittensorPositionRemoteSource::class.java)

    private val priceRepository = mock(BittensorPortfolioPriceRepository::class.java)

    private val chainStateRepository = mock(ChainStateRepository::class.java)

    private val logger = mock(Logger::class.java)

    private val commonModule = CommonModule()

    private val computationalCache = commonModule.provideComputationalCache()

    private val portfolioLoader = BittensorPortfolioLoader(priceRepository, logger)

    private val pricesSource = BittensorPricesSource(portfolioLoader, snapshots, computationalCache)

    private val positionUpdates = io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorPositionUpdates()

    private val sharedPositionSource = BittensorPositionSource(
        positionSource,
        chainStateRepository,
        computationalCache,
        logger,
        positionUpdates,
        snapshots
    )

    private val repository = RealBittensorPositionRepository(sharedPositionSource, portfolioLoader, pricesSource)

    private val blocks = MutableStateFlow(BigInteger.ONE)

    private val coldKey = ByteArray(32)

    private val hotkey = AccountIdKey(ByteArray(32) { 1 })

    private val subnetId = BittensorNetuid.Subnet(1)

    private val positions = BittensorStakePositions(
        listOf(BittensorRootPosition(hotkey, BigInteger.TEN)),
        listOf(BittensorSubnetPosition(subnetId, hotkey, BigInteger.ONE))
    )

    @Test
    fun `invalidation rejects previous position and price writes only for the operation wallet`() = runBlocking {
        val positionKey = BittensorCacheKeys.positions("chain", coldKey.toHexString())
        val priceKey = BittensorCacheKeys.prices("chain", coldKey.toHexString())
        val positionTicket = snapshots.snapshotTicket(positionKey, 1L)
        val priceTicket = snapshots.snapshotTicket(priceKey, 1L)
        val otherWalletTicket = snapshots.snapshotTicket(priceKey, 2L)
        snapshots.saveSnapshot(positionKey, 1L, positions)

        repository.invalidate("chain", coldKey, 1L).getOrThrow()

        assertFalse(snapshots.saveSnapshot(positionKey, 1L, positions, positionTicket))
        assertFalse(snapshots.saveSnapshot(priceKey, 1L, "obsolete prices", priceTicket))
        assertTrue(snapshots.saveSnapshot(priceKey, 2L, "other wallet prices", otherWalletTicket))
        assertEquals(positions, repository.observePositions("chain", coldKey, 1L).first().positions)
    }

    @Test
    fun `same coldkey in another meta account does not inherit the saved positions`() = runBlocking {
        val key = BittensorCacheKeys.positions("chain", coldKey.toHexString())
        snapshots.saveSnapshot(key, 1L, positions)
        val other = repository.observePositions("chain", coldKey, metaAccountId = 2L).first()
        assertNull(other.positions)
        assertTrue(other.status is BittensorDataStatus.Loading)
        val owner = repository.observePositions("chain", coldKey, metaAccountId = 1L).first()
        assertEquals(positions, owner.positions)
        assertTrue(owner.status is BittensorDataStatus.Stale)
    }

    @Test
    fun `offline cold start emits saved positions before any chain request`() = runBlocking {
        val dao = InMemoryResponseCacheDao()
        val previousProcess = createResponseTestCache(dao)
        previousProcess.saveSnapshot(BittensorCacheKeys.positions("chain", coldKey.toHexString()), 1L, positions)
        val restartedSnapshots = createResponseTestCache(dao)
        val restartedPricesSource = BittensorPricesSource(portfolioLoader, restartedSnapshots, computationalCache)
        val sharedPositionSource = BittensorPositionSource(
            positionSource,
            chainStateRepository,
            computationalCache,
            logger,
            positionUpdates,
            restartedSnapshots
        )

        val restarted = RealBittensorPositionRepository(sharedPositionSource, portfolioLoader, restartedPricesSource)
        val state = withTimeout(3_000) { restarted.observePositions("chain", coldKey, metaAccountId = 1L).first() }
        assertEquals(positions, state.positions)
        assertTrue(state.status is BittensorDataStatus.Stale)
    }

    @Test
    fun `confirmed operation refreshes positions without waiting for a block`() = runBlocking<Unit> {
        prepare()
        val states = Channel<BittensorPositionState>(Channel.UNLIMITED)
        val observer = launch {
            repository.observePositions("chain", coldKey, metaAccountId = 1L)
                .filter { it.status is BittensorDataStatus.Fresh }
                .collect { states.send(it) }
        }
        try {
            withTimeout(3_000L) {
                states.receive()
                positionUpdates.confirmed(1L, "other-chain", AccountIdKey(coldKey))
                positionUpdates.confirmed(1L, "chain", AccountIdKey(ByteArray(32) { 1 }))
                positionUpdates.confirmed(1L, "chain", AccountIdKey(coldKey))
                states.receive()
                verify(positionSource, times(2)).positions("chain", coldKey)
            }
        } finally {
            observer.cancelAndJoin()
        }
    }

    @Test
    fun `root and portfolio consumers share requests and independent priced state`() = runBlocking<Unit> {
        prepare()
        val rootUpdates = Channel<BittensorPositionState>(Channel.UNLIMITED)
        val portfolioUpdates = Channel<BittensorPortfolioState>(Channel.UNLIMITED)
        val dashboardUpdates = Channel<BittensorPortfolioState>(Channel.UNLIMITED)
        val rootJob = launch {
            repository.observePositions("chain", coldKey, metaAccountId = 1L)
                .filter { it.status is BittensorDataStatus.Fresh }
                .collect { rootUpdates.send(it) }
        }
        val portfolioJob = launch {
            repository.observePortfolio("chain", coldKey.copyOf(), metaAccountId = 1L)
                .filter { it.status is BittensorDataStatus.Fresh && it.positions?.subnetPrices?.isNotEmpty() == true }
                .collect { portfolioUpdates.send(it) }
        }
        val dashboardJob = launch {
            repository.observePortfolio("chain", coldKey.copyOf(), metaAccountId = 1L)
                .filter { it.status is BittensorDataStatus.Fresh && it.positions?.subnetPrices?.isNotEmpty() == true }
                .collect { dashboardUpdates.send(it) }
        }
        try {
            withTimeout(3_000L) {
                assertEquals(positions, rootUpdates.receive().positions)
                assertEquals(portfolioUpdates.receive(), dashboardUpdates.receive())
                verify(positionSource).positions("chain", coldKey)
                verify(priceRepository).prices("chain", setOf(subnetId))

                rootJob.cancelAndJoin()
                val changed = positions.copy(root = listOf(positions.root.single().copy(stakeInPlanks = BigInteger.valueOf(20))))
                `when`(positionSource.positions("chain", coldKey)).thenReturn(changed)
                blocks.value += BigInteger.ONE
                val portfolio = portfolioUpdates.next { it.positions?.root == changed.root }
                val dashboard = dashboardUpdates.next { it.positions?.root == changed.root }
                assertEquals(portfolio, dashboard)
                verify(positionSource, times(2)).positions("chain", coldKey)

                // Price sharing must outlive the first portfolio consumer while another is still observing.
                portfolioJob.cancelAndJoin()
                val updatedPrice = BigInteger.valueOf(50)
                `when`(priceRepository.prices("chain", setOf(subnetId))).thenReturn(mapOf(subnetId to updatedPrice))
                blocks.value += BigInteger.ONE
                val repriced = dashboardUpdates.next { it.positions?.subnetPrices?.get(subnetId) == updatedPrice }
                assertEquals(changed.subnets, repriced.positions?.subnets)
                assertEquals(1, blocks.subscriptionCount.value)
            }
        } finally {
            rootJob.cancelAndJoin()
            portfolioJob.cancelAndJoin()
            dashboardJob.cancelAndJoin()
        }
        withTimeout(3_000L) { blocks.subscriptionCount.first { it == 0 } }
    }

    @Test
    fun `root positions remain available while subnet prices are pending`() = runBlocking<Unit> {
        prepare()
        val priceRequested = CompletableDeferred<Unit>()
        val price = CompletableDeferred<BigInteger>()
        val slowPriceRepository = object : BittensorPortfolioPriceRepository by priceRepository {
            override suspend fun prices(chainId: String, heldSubnets: Set<BittensorNetuid.Subnet>): Map<BittensorNetuid.Subnet, BigInteger> {
                priceRequested.complete(Unit)
                return mapOf(subnetId to price.await())
            }
        }
        val slowPortfolioLoader = BittensorPortfolioLoader(slowPriceRepository, logger)
        val pricesSource = BittensorPricesSource(slowPortfolioLoader, snapshots, computationalCache)
        val sharedPositionSource = BittensorPositionSource(
            positionSource,
            chainStateRepository,
            computationalCache,
            logger,
            positionUpdates,
            snapshots
        )

        val sharedPositions = RealBittensorPositionRepository(sharedPositionSource, slowPortfolioLoader, pricesSource)
        val portfolioJob = launch { sharedPositions.observePortfolio("chain", coldKey, metaAccountId = 1L).collect {} }
        try {
            withTimeout(3_000L) {
                priceRequested.await()
                val root = sharedPositions.observePositions("chain", coldKey, metaAccountId = 1L)
                    .first { it.status is BittensorDataStatus.Fresh }
                assertEquals(positions.root, root.positions?.root)
                verify(positionSource).positions("chain", coldKey)
            }
        } finally {
            portfolioJob.cancelAndJoin()
        }
        withTimeout(3_000L) { blocks.subscriptionCount.first { it == 0 } }
    }

    @Test
    fun `failed portfolio account does not receive another account positions or prices`() = runBlocking {
        prepare()
        val otherColdKey = ByteArray(32) { 2 }
        `when`(positionSource.positions("chain", otherColdKey)).thenThrow(IllegalStateException("Unavailable"))
        val firstWallet = launch { repository.observePortfolio("chain", coldKey, metaAccountId = 1L).collect {} }
        try {
            withTimeout(3_000L) {
                val state = repository.observePortfolio("chain", otherColdKey, metaAccountId = 1L)
                    .first { it.status is BittensorDataStatus.Stale }
                assertNull(state.positions)
            }
        } finally {
            firstWallet.cancelAndJoin()
        }
    }

    @Test
    fun `portfolio publishes latest holdings and stale status while same-set prices remain in flight`() = runBlocking<Unit> {
        prepare()
        val requested = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        var priceCalls = 0
        val slowPrices = object : BittensorPortfolioPriceRepository {
            override suspend fun prices(chainId: String, heldSubnets: Set<BittensorNetuid.Subnet>): Map<BittensorNetuid.Subnet, BigInteger> {
                priceCalls++
                requested.complete(Unit)
                release.await()
                return mapOf(subnetId to BigInteger.TEN)
            }
        }
        val loader = BittensorPortfolioLoader(slowPrices, logger)
        val pricesSource = BittensorPricesSource(loader, snapshots, computationalCache)
        val sharedPositionSource = BittensorPositionSource(
            positionSource,
            chainStateRepository,
            computationalCache,
            logger,
            positionUpdates,
            snapshots
        )

        val repository = RealBittensorPositionRepository(sharedPositionSource, loader, pricesSource)
        val updates = Channel<BittensorPortfolioState>(Channel.UNLIMITED)
        val observer = launch { repository.observePortfolio("chain", coldKey, metaAccountId = 1L).collect { updates.send(it) } }
        try {
            withTimeout(3_000L) {
                requested.await()
                val initial = updates.next { it.status is BittensorDataStatus.Fresh }
                assertEquals(positions.subnets, initial.positions?.subnets)
                assertNull(initial.positions?.totalStake)
                assertTrue(initial.pricesPending)

                val changed = positions.copy(subnets = listOf(positions.subnets.single().copy(alphaInPlanks = BigInteger.TEN)))
                `when`(positionSource.positions("chain", coldKey)).thenReturn(changed)
                blocks.value += BigInteger.ONE
                val changedState = updates.next { it.positions?.subnets == changed.subnets }
                assertTrue(changedState.status is BittensorDataStatus.Fresh)
                assertEquals(1, priceCalls)

                `when`(positionSource.positions("chain", coldKey)).thenThrow(IllegalStateException("RPC unavailable"))
                blocks.value += BigInteger.ONE
                val stale = updates.next { it.status is BittensorDataStatus.Stale }
                assertEquals(changed.subnets, stale.positions?.subnets)

                release.complete(Unit)
                val priced = updates.next { it.positions?.subnetPrices?.isNotEmpty() == true }
                assertTrue(priced.status is BittensorDataStatus.Stale)
                assertEquals(changed.subnets, priced.positions?.subnets)
                assertFalse(priced.pricesPending)
                assertEquals(1, priceCalls)
            }
        } finally {
            observer.cancelAndJoin()
        }
    }

    @Test
    fun `changed held set starts new pricing and late cancelled response cannot replace it`() = runBlocking<Unit> {
        prepare()
        val firstStarted = CompletableDeferred<Unit>()
        val releaseFirst = CompletableDeferred<Unit>()
        val firstFinished = CompletableDeferred<Unit>()
        val otherSubnet = BittensorNetuid.Subnet(2)
        val changed = positions.copy(subnets = listOf(positions.subnets.single().copy(subnetId = otherSubnet)))
        val slowPrices = object : BittensorPortfolioPriceRepository {
            override suspend fun prices(chainId: String, heldSubnets: Set<BittensorNetuid.Subnet>): Map<BittensorNetuid.Subnet, BigInteger> {
                if (subnetId in heldSubnets) {
                    firstStarted.complete(Unit)
                    withContext(NonCancellable) { releaseFirst.await() }
                    firstFinished.complete(Unit)
                    return mapOf(subnetId to BigInteger.ONE)
                }
                return mapOf(otherSubnet to BigInteger.TEN)
            }
        }
        val loader = BittensorPortfolioLoader(slowPrices, logger)
        val pricesSource = BittensorPricesSource(loader, snapshots, computationalCache)
        val sharedPositionSource = BittensorPositionSource(
            positionSource,
            chainStateRepository,
            computationalCache,
            logger,
            positionUpdates,
            snapshots
        )

        val repository = RealBittensorPositionRepository(sharedPositionSource, loader, pricesSource)
        val updates = Channel<BittensorPortfolioState>(Channel.UNLIMITED)
        val observer = launch { repository.observePortfolio("chain", coldKey, metaAccountId = 1L).collect { updates.send(it) } }
        try {
            withTimeout(3_000L) {
                firstStarted.await()
                `when`(positionSource.positions("chain", coldKey)).thenReturn(changed)
                blocks.value += BigInteger.ONE
                val priced = updates.next { it.positions?.subnetPrices?.get(otherSubnet) == BigInteger.TEN }
                assertEquals(changed.subnets, priced.positions?.subnets)
                releaseFirst.complete(Unit)
                firstFinished.await()

                // An active second subscriber gets the shared latest state, not old prices.
                val replayed = repository.observePortfolio("chain", coldKey, metaAccountId = 1L)
                    .first { it.positions?.subnetPrices?.isNotEmpty() == true }
                assertEquals(mapOf(otherSubnet to BigInteger.TEN), replayed.positions?.subnetPrices)
            }
        } finally {
            releaseFirst.complete(Unit)
            observer.cancelAndJoin()
        }
    }

    private suspend fun Channel<BittensorPortfolioState>.next(predicate: (BittensorPortfolioState) -> Boolean): BittensorPortfolioState {
        while (true) {
            val state = receive()
            if (predicate(state)) return state
        }
    }

    @Test
    fun `last subscriber stops updates and a later subscription reloads positions`() = runBlocking<Unit> {
        prepare()
        withTimeout(3_000L) {
            repository.observePositions("chain", coldKey, metaAccountId = 1L)
                .first { it.status is BittensorDataStatus.Fresh }
            blocks.subscriptionCount.first { it == 0 }

            val emptyPositions = BittensorStakePositions(emptyList(), emptyList())
            `when`(positionSource.positions("chain", coldKey)).thenReturn(emptyPositions)
            val state = repository.observePositions("chain", coldKey, metaAccountId = 1L)
                .first { it.status is BittensorDataStatus.Fresh }

            assertTrue(requireNotNull(state.positions).root.isEmpty())
            verify(positionSource, times(2)).positions("chain", coldKey)
            blocks.subscriptionCount.first { it == 0 }
        }
    }

    @Test
    fun `chain identity separates subscriptions for the same account`() = runBlocking<Unit> {
        prepare()
        val otherBlocks = MutableStateFlow(BigInteger.ONE)
        val emptyPositions = BittensorStakePositions(emptyList(), emptyList())
        `when`(chainStateRepository.currentRemoteBlockNumberFlow("other-chain")).thenReturn(otherBlocks)
        `when`(positionSource.positions("other-chain", coldKey)).thenReturn(emptyPositions)
        val firstChain = launch { repository.observePositions("chain", coldKey, metaAccountId = 1L).collect {} }
        try {
            withTimeout(3_000L) {
                val state = repository.observePositions("other-chain", coldKey, metaAccountId = 1L)
                    .first { it.status is BittensorDataStatus.Fresh }
                assertEquals(emptyPositions, state.positions)
                verify(positionSource).positions("other-chain", coldKey)
            }
        } finally {
            firstChain.cancelAndJoin()
        }
    }

    @Test
    fun `failed block subscription preserves positions and reconnects`() = runBlocking<Unit> {
        prepare()
        val subscriptionFailure = CompletableDeferred<Unit>()
        val failedBlocks = flow<BigInteger> {
            emit(BigInteger.ONE)
            subscriptionFailure.await()
            throw IllegalStateException("Subscription failed")
        }
        `when`(chainStateRepository.currentRemoteBlockNumberFlow("chain")).thenReturn(failedBlocks, blocks)
        val updates = Channel<BittensorPositionState>(Channel.UNLIMITED)
        val observer = launch {
            repository.observePositions("chain", coldKey, metaAccountId = 1L)
                .filter { it.status !is BittensorDataStatus.Loading }
                .collect { updates.send(it) }
        }
        try {
            withTimeout(3_000L) {
                val initial = updates.receive()
                assertEquals(positions, initial.positions)
                subscriptionFailure.complete(Unit)
                val stale = updates.receive()
                assertTrue(stale.status is BittensorDataStatus.Stale)
                assertEquals(positions, stale.positions)
                val recovered = updates.receive()
                assertTrue(recovered.status is BittensorDataStatus.Fresh)
                assertEquals(positions, recovered.positions)
                verify(chainStateRepository, times(2)).currentRemoteBlockNumberFlow("chain")
            }
        } finally {
            observer.cancelAndJoin()
        }
    }

    private suspend fun prepare() {
        `when`(chainStateRepository.currentRemoteBlockNumberFlow("chain")).thenReturn(blocks)
        `when`(positionSource.positions("chain", coldKey)).thenReturn(positions)
        `when`(priceRepository.prices("chain", setOf(subnetId))).thenReturn(mapOf(subnetId to BigInteger.TEN))
    }
}
