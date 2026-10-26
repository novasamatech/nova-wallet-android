package io.novafoundation.nova.feature_staking_impl.data.bittensor

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorDataStatus
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorFailureStage
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorOperationResult
import java.io.IOException
import java.math.BigInteger
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BittensorPendingClaimsTest {

    private var now = 0L

    private val clock = BittensorClaimClock { now }

    private val pending = BittensorPendingClaims(clock)

    private val coldkey = AccountIdKey(ByteArray(32) { 1 })

    private val hotkey = AccountIdKey(ByteArray(32) { 2 })

    private val target = BittensorClaimTarget("chain", coldkey, hotkey)

    private val preview = BittensorRootClaimPreview(hotkey, BigInteger.TEN, BigInteger.TEN, BigInteger.ZERO)

    private val claims = BittensorRootClaims(listOf(preview), BigInteger.ONE)

    @Test
    fun `simultaneous duplicate targets acquire exactly once`() {
        val pool = Executors.newFixedThreadPool(4)
        try {
            val results = pool.invokeAll(List(20) { Callable { pending.tryAcquire(target) } }).map { it.get() }
            assertEquals(1, results.count { it != null })
            assertEquals(setOf(hotkey), pending.pendingTargets("chain", coldkey))
        } finally {
            pool.shutdownNow()
        }
    }

    @Test
    fun `ttl expires at fifteen minutes and late completion cannot clear replacement`() {
        val old = requireNotNull(pending.tryAcquire(target))
        now = BittensorPendingClaims.PENDING_MILLIS - 1
        assertNull(pending.tryAcquire(target))
        now += 1
        assertTrue(pending.pendingTargets("chain", coldkey).isEmpty())
        val replacement = requireNotNull(pending.tryAcquire(target))
        pending.finish(old, BittensorOperationResult.Success("old"))
        assertEquals(setOf(hotkey), pending.pendingTargets("chain", coldkey))
        pending.finish(replacement, BittensorOperationResult.Success("new"))
        assertTrue(pending.pendingTargets("chain", coldkey).isEmpty())
    }

    @Test
    fun `fresh ineligible target settles but stale or still eligible snapshots do not`() {
        pending.tryAcquire(target)
        now = 1
        val fresh = claims.copy(readStartedAtMillis = now)
        pending.reconcileFreshClaims("chain", coldkey, fresh, BittensorDataStatus.Fresh)
        assertNull(pending.tryAcquire(target))
        val settled = fresh.copy(previews = emptyList())
        pending.reconcileFreshClaims("chain", coldkey, settled, BittensorDataStatus.Stale(IOException("offline")))
        assertNull(pending.tryAcquire(target))
        pending.reconcileFreshClaims("chain", coldkey, settled, BittensorDataStatus.Fresh)
        assertNotNull(pending.tryAcquire(target))
        now = 2
        pending.reconcileFreshClaims("chain", coldkey, claims.copy(minimum = BigInteger.valueOf(11), readStartedAtMillis = now), BittensorDataStatus.Fresh)
        assertTrue(pending.pendingTargets("chain", coldkey).isEmpty())
    }

    @Test
    fun `unknown stages retain and proven terminal stages release`() {
        val lease = requireNotNull(pending.tryAcquire(target))
        pending.finish(lease, BittensorOperationResult.Unknown(null))
        assertNull(pending.tryAcquire(target))
        pending.finish(lease, BittensorOperationResult.Failure(IOException(), stage = BittensorFailureStage.UNKNOWN))
        assertNull(pending.tryAcquire(target))
        pending.finish(lease, BittensorOperationResult.Failure(IOException(), stage = BittensorFailureStage.NOT_SUBMITTED))
        val dispatched = requireNotNull(pending.tryAcquire(target))
        pending.finish(dispatched, BittensorOperationResult.Failure(IOException(), stage = BittensorFailureStage.DISPATCHED))
        val success = requireNotNull(pending.tryAcquire(target))
        pending.finish(success, BittensorOperationResult.Success("hash"))
        assertTrue(pending.pendingTargets("chain", coldkey).isEmpty())
    }

    @Test
    fun `other validators coldkeys and chains are independent`() {
        val other = AccountIdKey(ByteArray(32) { 3 })
        assertNotNull(pending.tryAcquire(target))
        assertNotNull(pending.tryAcquire(target.copy(hotkey = other)))
        assertNotNull(pending.tryAcquire(target.copy(coldkey = other)))
        assertNotNull(pending.tryAcquire(target.copy(chainId = "other")))
        now = 1
        pending.reconcileFreshClaims("chain", coldkey, claims.copy(readStartedAtMillis = now), BittensorDataStatus.Fresh)
        assertEquals(setOf(hotkey), pending.pendingTargets("chain", coldkey))
        assertEquals(setOf(hotkey), pending.pendingTargets("chain", other))
        assertEquals(setOf(hotkey), pending.pendingTargets("other", coldkey))
    }

    @Test
    fun `observers react to acquire and finish without waiting for a block`() = runBlocking {
        val updates = Channel<Set<AccountIdKey>>(Channel.UNLIMITED)
        val observation = pending.observe("chain", coldkey)
        val job = launch { observation.collect { updates.send(it) } }
        try {
            withTimeout(3_000) {
                assertTrue(updates.receive().isEmpty())
                val lease = requireNotNull(pending.tryAcquire(target))
                assertEquals(setOf(hotkey), updates.receive())
                pending.finish(lease, BittensorOperationResult.Success("hash"))
                assertTrue(updates.receive().isEmpty())
            }
        } finally {
            job.cancelAndJoin()
        }
    }

    @Test
    fun `delayed fresh read cannot settle a lease acquired after read began`() {
        val delayed = claims.copy(previews = emptyList(), readStartedAtMillis = now)
        now = 1
        val first = requireNotNull(pending.tryAcquire(target))
        pending.reconcileFreshClaims("chain", coldkey, delayed, BittensorDataStatus.Fresh)
        assertNull(pending.tryAcquire(target))
        pending.finish(first, BittensorOperationResult.Success("first"))
        now = 2
        pending.tryAcquire(target)
        pending.reconcileFreshClaims("chain", coldkey, delayed, BittensorDataStatus.Fresh)
        assertNull(pending.tryAcquire(target))
        // Equal millisecond provenance is ambiguous and must also retain the lease.
        pending.reconcileFreshClaims("chain", coldkey, delayed.copy(readStartedAtMillis = now), BittensorDataStatus.Fresh)
        assertNull(pending.tryAcquire(target))
        pending.reconcileFreshClaims("chain", coldkey, delayed.copy(readStartedAtMillis = null), BittensorDataStatus.Fresh)
        assertNull(pending.tryAcquire(target))
        now = 3
        pending.reconcileFreshClaims("chain", coldkey, delayed.copy(readStartedAtMillis = now), BittensorDataStatus.Fresh)
        assertTrue(pending.pendingTargets("chain", coldkey).isEmpty())
    }

    @Test
    fun `expiry crossing between observer clock reads still emits empty`() = runBlocking {
        var reads = 0
        val advancingClock = BittensorClaimClock {
            when (reads++) {
                0 -> 0L // Acquire.
                1 -> BittensorPendingClaims.PENDING_MILLIS - 1 // One active snapshot with a 1 ms deadline.
                else -> BittensorPendingClaims.PENDING_MILLIS
            }
        }
        val repository = BittensorPendingClaims(advancingClock)
        repository.tryAcquire(target)
        val updates = Channel<Set<AccountIdKey>>(Channel.UNLIMITED)
        val observation = repository.observe("chain", coldkey)
        val job = launch { observation.collect { updates.send(it) } }
        try {
            withTimeout(3_000) {
                assertEquals(setOf(hotkey), updates.receive())
                assertTrue(updates.receive().isEmpty())
                assertEquals(3, reads)
            }
        } finally {
            job.cancelAndJoin()
        }
    }
}
