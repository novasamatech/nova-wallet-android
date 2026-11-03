package io.novafoundation.nova.feature_staking_impl.data.bittensor

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.common.di.modules.CommonModule
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorDataStatus
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorPositionUpdates
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorRootPosition
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorStakePositions
import io.novafoundation.nova.runtime.repository.ChainStateRepository
import io.novasama.substrate_sdk_android.runtime.AccountId
import io.novasama.substrate_sdk_android.extensions.toHexString
import io.novasama.substrate_sdk_android.wsrpc.logging.Logger
import java.math.BigInteger
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock

class BittensorPositionSourceTest {

    private val dao = InMemoryResponseCacheDao()

    private val cache = createResponseTestCache(dao)

    private val remote = mock(BittensorPositionRemoteSource::class.java)

    private val blocks = MutableStateFlow(BigInteger.ONE)

    private val chainState = mock(ChainStateRepository::class.java)

    private val commonModule = CommonModule()

    private val computationalCache = commonModule.provideComputationalCache()

    private val logger = mock(Logger::class.java)

    private val updates = BittensorPositionUpdates()

    private val source = BittensorPositionSource(remote, chainState, computationalCache, logger, updates, cache)

    private val coldKey = ByteArray(32)

    private val key = BittensorCacheKeys.positions("chain", coldKey.toHexString())

    private val saved = BittensorStakePositions(listOf(BittensorRootPosition(AccountIdKey(ByteArray(32) { 1 }), BigInteger.TEN)), emptyList())

    @Test
    fun `incompatible stored positions are discarded before publishing`() = runBlocking {
        val invalid = saved.copy(root = listOf(saved.root.single().copy(stakeInPlanks = BigInteger.valueOf(-1))))
        cache.saveSnapshot(key, 1L, invalid)
        `when`(chainState.currentRemoteBlockNumberFlow("chain")).thenReturn(blocks)
        `when`(remote.positions("chain", coldKey)).thenThrow(IllegalStateException("Offline"))

        val initial = withTimeout(3_000) { source.observePositions("chain", coldKey, 1L).first() }

        assertNull(initial.positions)
        assertEquals(BittensorDataStatus.Loading, initial.status)
        assertNull(dao.get(key, 1L))
    }

    @Test
    fun `failed refresh keeps restored positions stale`() = runBlocking<Unit> {
        cache.saveSnapshot(key, 1L, saved)
        val failure = IllegalStateException("Offline")
        `when`(chainState.currentRemoteBlockNumberFlow("chain")).thenReturn(blocks)
        `when`(remote.positions("chain", coldKey)).thenThrow(failure)

        val state = withTimeout(3_000) {
            source.observePositions("chain", coldKey, 1L).first { (it.status as? BittensorDataStatus.Stale)?.cause?.message == failure.message }
        }

        assertEquals(saved, state.positions)
        assertTrue(state.status is BittensorDataStatus.Stale)
    }

    @Test
    fun `source persists fresh positions for the next process`() = runBlocking<Unit> {
        `when`(chainState.currentRemoteBlockNumberFlow("chain")).thenReturn(blocks)
        `when`(remote.positions("chain", coldKey)).thenReturn(saved)
        withTimeout(3_000) { source.observePositions("chain", coldKey, 1L).first { it.status is BittensorDataStatus.Fresh } }
        val restartedCache = createResponseTestCache(dao)
        val restartedComputations = commonModule.provideComputationalCache()
        val restarted = BittensorPositionSource(remote, chainState, restartedComputations, logger, updates, restartedCache)

        val initial = withTimeout(3_000) { restarted.observePositions("chain", coldKey, 1L).first() }

        assertEquals(saved, initial.positions)
        assertTrue(initial.status is BittensorDataStatus.Stale)
    }

    @Test
    fun `invalidation rejects an older remote load and retains saved positions`() = runBlocking<Unit> {
        cache.saveSnapshot(key, 1L, saved)
        `when`(chainState.currentRemoteBlockNumberFlow("chain")).thenReturn(blocks)
        val started = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        val lateRemote = object : BittensorPositionRemoteSource {
            override suspend fun positions(chainId: String, coldKey: AccountId): BittensorStakePositions {
                started.complete(Unit)
                release.await()
                return BittensorStakePositions(emptyList(), emptyList())
            }
        }
        val delayed = BittensorPositionSource(lateRemote, chainState, computationalCache, logger, updates, cache)
        val states = Channel<BittensorPositionState>(Channel.UNLIMITED)
        val observer = launch { delayed.observePositions("chain", coldKey, 1L).collect { states.send(it) } }
        try {
            withTimeout(3_000) {
                assertEquals(saved, states.receive().positions)
                started.await()
                delayed.invalidate("chain", coldKey, 1L)
                release.complete(Unit)
                val rejected = states.receive()
                assertEquals(saved, rejected.positions)
                assertTrue(rejected.status is BittensorDataStatus.Stale)
                assertEquals(saved, cache.readSnapshot(key, BittensorStakePositions::class.java, 1L))
            }
        } finally {
            release.complete(Unit)
            observer.cancelAndJoin()
        }
    }
}
