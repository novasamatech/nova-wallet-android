package io.novafoundation.nova.feature_staking_impl.data.bittensor

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorDataStatus
import io.novasama.substrate_sdk_android.wsrpc.logging.Logger
import java.io.IOException
import java.math.BigInteger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.mockito.Mockito.mock

class BittensorRootClaimObservationTest {

    private val logger = mock(Logger::class.java)

    private val hotkey = AccountIdKey(ByteArray(32) { 1 })

    private val preview = BittensorRootClaimPreview(hotkey, BigInteger.TEN, BigInteger.TEN, BigInteger.ZERO)

    private val claims = BittensorRootClaims(listOf(preview), BigInteger.ONE)

    @Test
    fun `blocks update claimability after external claim and new rewards`() = runBlocking {
        val blocks = MutableStateFlow(1)
        var current = claims
        val updates = Channel<BittensorRootClaimSnapshot>(Channel.UNLIMITED)
        val observation = observeBittensorRootClaims(blocks, { current }, logger)
        val job = launch { observation.collect { updates.send(it) } }
        try {
            withTimeout(3_000) {
                assertTrue(updates.receive().status is BittensorDataStatus.Loading)
                assertEquals(1, updates.receive().claims!!.claimable.size)
                current = claims.copy(previews = emptyList())
                blocks.value = 2
                assertTrue(updates.receive().claims!!.claimable.isEmpty())
                current = claims
                blocks.value = 3
                assertEquals(1, updates.receive().claims!!.claimable.size)
            }
        } finally {
            job.cancelAndJoin()
        }
    }

    @Test
    fun `failed refresh retains stale data and later block recovers`() = runBlocking {
        val blocks = MutableStateFlow(1)
        var failure = false
        val updates = Channel<BittensorRootClaimSnapshot>(Channel.UNLIMITED)
        val observation = observeBittensorRootClaims(blocks, { if (failure) throw IOException("offline") else claims }, logger)
        val job = launch { observation.collect { updates.send(it) } }
        try {
            withTimeout(3_000) {
                updates.receive()
                assertTrue(updates.receive().status is BittensorDataStatus.Fresh)
                failure = true
                blocks.value = 2
                val stale = updates.receive()
                assertEquals(claims, stale.claims)
                assertTrue(stale.status is BittensorDataStatus.Stale)
                failure = false
                blocks.value = 3
                assertTrue(updates.receive().status is BittensorDataStatus.Fresh)
            }
        } finally {
            job.cancelAndJoin()
        }
    }

    @Test
    fun `cancellation is not converted to stale claims`() = runBlocking {
        val blocks = MutableStateFlow(1)
        val observation = observeBittensorRootClaims(blocks, { throw CancellationException("cancel") }, logger)
        val emitted = mutableListOf<BittensorRootClaimSnapshot>()
        try {
            observation.toList(emitted)
            fail("Expected cancellation")
        } catch (expected: CancellationException) {
            assertEquals(1, emitted.size)
            assertTrue(emitted.single().status is BittensorDataStatus.Loading)
        }
    }
}
