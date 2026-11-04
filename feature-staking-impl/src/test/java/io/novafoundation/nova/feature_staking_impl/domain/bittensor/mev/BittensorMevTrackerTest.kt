package io.novafoundation.nova.feature_staking_impl.domain.bittensor.mev

import io.novafoundation.nova.common.data.network.runtime.model.SignedBlock
import io.novafoundation.nova.common.utils.extrinsicHash
import io.novafoundation.nova.feature_account_api.data.extrinsic.execution.ExtrinsicDispatch
import io.novafoundation.nova.feature_account_api.data.extrinsic.execution.ExtrinsicExecutionResult
import io.novafoundation.nova.feature_account_api.data.signer.CallExecutionType
import io.novafoundation.nova.feature_account_api.data.signer.SubmissionHierarchy
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorExtrinsicOutcomeResolver
import io.novafoundation.nova.runtime.extrinsic.Mortality
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.network.rpc.RpcCalls
import io.novasama.substrate_sdk_android.runtime.definitions.types.generics.Era
import io.novasama.substrate_sdk_android.runtime.extrinsic.signer.SendableExtrinsic
import io.novasama.substrate_sdk_android.wsrpc.logging.Logger
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

class BittensorMevTrackerTest {

    private val chain = mock(Chain::class.java)

    private val rpc = mock(RpcCalls::class.java)

    private val logger = mock(Logger::class.java)

    private val account = mock(MetaAccount::class.java)

    private val hierarchy = SubmissionHierarchy(account, CallExecutionType.IMMEDIATE)

    private val inner = mock(SendableExtrinsic::class.java)

    private val carrier = mock(SendableExtrinsic::class.java)

    private val timing = BittensorMevTiming(10.toBigInteger(), 100, Mortality(Era.Mortal(8, 4), "0xbirth"))

    private var unknownInner = false

    private var delayedUnknownAttempts = 0

    private var resolutionAttempts = 0

    private val resolvedBlocks = mutableListOf<String>()

    private val resolver = BittensorExtrinsicOutcomeResolver { _, included ->
        resolvedBlocks += included.status.blockHash
        if (included.status.extrinsicHash == INNER.extrinsicHash() && delayedUnknownAttempts > 0) {
            delayedUnknownAttempts -= 1
            delay(20_000)
            return@BittensorExtrinsicOutcomeResolver ExtrinsicExecutionResult(
                included.status.extrinsicHash,
                included.status.blockHash,
                ExtrinsicDispatch.Unknown,
                included.submissionHierarchy
            )
        }
        val outcome = if (included.status.extrinsicHash == INNER.extrinsicHash() && unknownInner && resolutionAttempts++ == 0) {
            ExtrinsicDispatch.Unknown
        } else {
            ExtrinsicDispatch.Ok(emptyList())
        }
        ExtrinsicExecutionResult(included.status.extrinsicHash, included.status.blockHash, outcome, included.submissionHierarchy)
    }

    private val tracker = BittensorMevTracker(rpc, resolver, logger)

    @Test
    fun `carrier inclusion alone does not mean the order executed`() = runTest {
        configure(mapOf(102 to listOf(CARRIER)))
        val result = track() as BittensorMevExecution.NotExecuted
        assertEquals(BittensorMevCarrier.INCLUDED, result.carrier)
    }

    @Test
    fun `complete finalized era proves absence even after pool rejection`() = runTest {
        configure(emptyMap())
        `when`(rpc.submitAndWatchExtrinsic("chain", carrier)).thenReturn(flow { throw IllegalStateException("Pool rejected") })
        val result = track() as BittensorMevExecution.NotExecuted
        assertEquals(BittensorMevCarrier.ABSENT, result.carrier)
    }

    @Test
    fun `inner is found by hash independently of carrier watch`() = runTest {
        configure(mapOf(102 to listOf(CARRIER, INNER)))
        val result = track() as BittensorMevExecution.Executed
        assertEquals(INNER.extrinsicHash(), result.inner.extrinsicHash)
        assertEquals(CARRIER.extrinsicHash(), result.carrier?.extrinsicHash)
        assertEquals("0x102", result.inner.blockHash)
    }

    @Test
    fun `unknown dispatch is retried at the same height rather than declared absent`() = runTest {
        configure(mapOf(102 to listOf(INNER)))
        unknownInner = true
        assertTrue(track() is BittensorMevExecution.Executed)
        assertEquals(listOf("0x102", "0x102"), resolvedBlocks)
    }

    @Test
    fun `incomplete finality remains unconfirmed without retry permission`() = runTest {
        configure(emptyMap(), finalized = 101)
        val result = track() as BittensorMevExecution.Unconfirmed
        assertEquals(INNER.extrinsicHash(), result.innerHash)
    }

    @Test
    fun `the unconfirmed cap does not discard a late finalized result`() = runTest {
        configure(mapOf(102 to listOf(INNER)))
        delayedUnknownAttempts = 10
        val unconfirmed = mutableListOf<String>()
        val result = tracker.submitAndTrack(chain, timing, inner, carrier, hierarchy, 12_000, unconfirmed::add)
        assertTrue(result is BittensorMevExecution.Executed)
        assertEquals(listOf(INNER.extrinsicHash()), unconfirmed)
        assertTrue(testScheduler.currentTime > 204_000)
    }

    @Test
    fun `an unreadable finalized block cannot be skipped as absent`() = runTest {
        configure(emptyMap())
        `when`(rpc.getBlock("chain", "0x102")).thenThrow(IllegalStateException("Lost archive block"))
        assertTrue(track() is BittensorMevExecution.Unconfirmed)
    }

    private suspend fun configure(contents: Map<Int, List<String>>, finalized: Int = 107) {
        `when`(chain.id).thenReturn("chain")
        `when`(inner.extrinsicHex).thenReturn(INNER)
        `when`(carrier.extrinsicHex).thenReturn(CARRIER)
        `when`(rpc.submitAndWatchExtrinsic("chain", carrier)).thenReturn(flow { })
        `when`(rpc.getFinalizedHead("chain")).thenReturn("0xfinal")
        `when`(rpc.getBlockHeader("chain", "0xfinal")).thenReturn(header(finalized))
        for (height in 100..107) {
            val hash = "0x$height"
            val block = SignedBlock(SignedBlock.Block(contents[height].orEmpty(), header(height)), null)
            `when`(rpc.getBlockHash("chain", height.toBigInteger())).thenReturn(hash)
            `when`(rpc.getBlock("chain", hash)).thenReturn(block)
        }
    }

    private suspend fun track() = tracker.submitAndTrack(chain, timing, inner, carrier, hierarchy, 12_000)

    private fun header(height: Int) = SignedBlock.Block.Header("0x" + height.toString(16), null)

    private companion object {
        const val INNER = "0x010203"
        const val CARRIER = "0x040506"
    }
}
