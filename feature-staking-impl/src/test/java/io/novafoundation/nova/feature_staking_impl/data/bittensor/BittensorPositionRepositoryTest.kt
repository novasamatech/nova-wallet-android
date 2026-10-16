package io.novafoundation.nova.feature_staking_impl.data.bittensor

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
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import org.mockito.Mockito.times
import org.mockito.Mockito.verify

class BittensorPositionRepositoryTest {

    private val positionSource = mock(BittensorPositionSource::class.java)

    private val swapSource = mock(BittensorSwapSource::class.java)

    private val chainStateRepository = mock(ChainStateRepository::class.java)

    private val logger = mock(Logger::class.java)

    private val commonModule = CommonModule()

    private val computationalCache = commonModule.provideComputationalCache()

    private val portfolioLoader = BittensorPortfolioLoader(swapSource, logger)

    private val repository = RealBittensorPositionRepository(positionSource, portfolioLoader, chainStateRepository, computationalCache, logger)

    private val blocks = MutableStateFlow(BigInteger.ONE)

    private val coldKey = ByteArray(32)

    private val hotkey = AccountIdKey(ByteArray(32) { 1 })

    private val subnetId = BittensorNetuid.Subnet(1)

    private val positions = BittensorStakePositions(
        listOf(BittensorRootPosition(hotkey, BigInteger.TEN)),
        listOf(BittensorSubnetPosition(subnetId, hotkey, BigInteger.ONE))
    )

    @Test
    fun `root and portfolio consumers share requests and prices on every block`() = runBlocking<Unit> {
        prepare()
        val rootUpdates = Channel<BittensorPositionState>(Channel.UNLIMITED)
        val portfolioUpdates = Channel<BittensorPortfolioState>(Channel.UNLIMITED)
        val dashboardUpdates = Channel<BittensorPortfolioState>(Channel.UNLIMITED)
        val rootJob = launch {
            repository.observePositions("chain", coldKey)
                .filter { it.status is BittensorDataStatus.Fresh }
                .collect { rootUpdates.send(it) }
        }
        val portfolioJob = launch {
            repository.observePortfolio("chain", coldKey.copyOf())
                .filter { it.status is BittensorDataStatus.Fresh }
                .collect { portfolioUpdates.send(it) }
        }
        val dashboardJob = launch {
            repository.observePortfolio("chain", coldKey.copyOf())
                .filter { it.status is BittensorDataStatus.Fresh }
                .collect { dashboardUpdates.send(it) }
        }
        try {
            withTimeout(3_000L) {
                assertEquals(positions, rootUpdates.receive().positions)
                assertEquals(portfolioUpdates.receive(), dashboardUpdates.receive())
                verify(positionSource).positions("chain", coldKey)
                verify(swapSource).currentAlphaPrice("chain", subnetId)

                rootJob.cancelAndJoin()
                blocks.value += BigInteger.ONE
                assertEquals(portfolioUpdates.receive(), dashboardUpdates.receive())
                verify(positionSource, times(2)).positions("chain", coldKey)
                verify(swapSource, times(2)).currentAlphaPrice("chain", subnetId)
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
        val slowSwapSource = object : BittensorSwapSource by swapSource {
            override suspend fun currentAlphaPrice(chainId: String, subnetId: BittensorNetuid.Subnet): BigInteger {
                priceRequested.complete(Unit)
                return price.await()
            }
        }
        val slowPortfolioLoader = BittensorPortfolioLoader(slowSwapSource, logger)
        val sharedPositions = RealBittensorPositionRepository(positionSource, slowPortfolioLoader, chainStateRepository, computationalCache, logger)
        val portfolioJob = launch { sharedPositions.observePortfolio("chain", coldKey).collect {} }
        try {
            withTimeout(3_000L) {
                priceRequested.await()
                val root = sharedPositions.observePositions("chain", coldKey)
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
    fun `failed account does not receive another account positions`() = runBlocking {
        prepare()
        val otherColdKey = ByteArray(32) { 2 }
        `when`(positionSource.positions("chain", otherColdKey)).thenThrow(IllegalStateException("Unavailable"))
        val firstWallet = launch { repository.observePositions("chain", coldKey).collect {} }
        try {
            withTimeout(3_000L) {
                val state = repository.observePositions("chain", otherColdKey)
                    .first { it.status is BittensorDataStatus.Stale }
                assertNull(state.positions)
            }
        } finally {
            firstWallet.cancelAndJoin()
        }
    }

    @Test
    fun `last subscriber stops updates and a later subscription reloads positions`() = runBlocking<Unit> {
        prepare()
        withTimeout(3_000L) {
            repository.observePositions("chain", coldKey)
                .first { it.status is BittensorDataStatus.Fresh }
            blocks.subscriptionCount.first { it == 0 }

            val emptyPositions = BittensorStakePositions(emptyList(), emptyList())
            `when`(positionSource.positions("chain", coldKey)).thenReturn(emptyPositions)
            val state = repository.observePositions("chain", coldKey)
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
        val firstChain = launch { repository.observePositions("chain", coldKey).collect {} }
        try {
            withTimeout(3_000L) {
                val state = repository.observePositions("other-chain", coldKey)
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
            repository.observePositions("chain", coldKey)
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
        `when`(swapSource.currentAlphaPrice("chain", subnetId)).thenReturn(BigInteger.TEN)
    }
}
