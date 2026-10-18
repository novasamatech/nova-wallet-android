package io.novafoundation.nova.feature_account_impl.data.extrinsic.recovery

import android.content.SharedPreferences
import com.google.gson.Gson
import io.novafoundation.nova.feature_account_api.data.extrinsic.recovery.ExtrinsicRecoveryState
import io.novafoundation.nova.feature_account_api.data.extrinsic.recovery.ExtrinsicSubmissionResolution
import io.novafoundation.nova.feature_account_api.data.extrinsic.recovery.RecoveredExtrinsicOutcome
import io.novasama.substrate_sdk_android.runtime.definitions.types.generics.Era
import io.novasama.substrate_sdk_android.runtime.definitions.types.generics.Extrinsic
import io.novasama.substrate_sdk_android.runtime.definitions.types.generics.GenericCall
import io.novasama.substrate_sdk_android.runtime.extrinsic.signer.SendableExtrinsic
import io.novasama.substrate_sdk_android.runtime.metadata.module.MetadataFunction
import io.novasama.substrate_sdk_android.runtime.metadata.module.Module
import io.novasama.substrate_sdk_android.wsrpc.logging.Logger
import java.math.BigInteger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock

class ExtrinsicSubmissionRecoveryTest {

    private val values = mutableMapOf<String, String>()

    private val preferences = mock(SharedPreferences::class.java) { invocation ->
        when (invocation.method.name) {
            "getString" -> values[invocation.getArgument<String>(0)] ?: invocation.getArgument<String?>(1)
            else -> null
        }
    }

    private val gson = Gson()

    private val pending = ExtrinsicSubmissionJournal(preferences, gson)

    private val source = FakeSource()

    private val logger = mock(Logger::class.java)

    private val recovery = RealExtrinsicSubmissionRecovery(pending, source, logger)

    private var allowCommit = true

    init {
        `when`(preferences.edit()).thenAnswer {
            val additions = mutableMapOf<String, String?>()
            val editor = mock(SharedPreferences.Editor::class.java)
            org.mockito.Mockito.doAnswer { call ->
                additions[call.getArgument(0)] = call.getArgument(1)
                editor
            }.`when`(editor)
                .putString(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.any())
            org.mockito.Mockito.doAnswer { call ->
                additions[call.getArgument(0)] = null
                editor
            }.`when`(editor)
                .remove(org.mockito.ArgumentMatchers.anyString())
            `when`(editor.commit()).thenAnswer {
                if (allowCommit) additions.forEach { (key, value) -> if (value == null) values.remove(key) else values[key] = value }
                allowCommit
            }
            editor
        }
    }

    @Test
    fun `finalized success releases pending and keeps an acknowledgement receipt`() = runBlocking {
        record()
        source.includedAt = 103
        source.found = RecoveredExtrinsicOutcome.SUCCESS
        val state = recovery.reconcile(1, "chain") as ExtrinsicRecoveryState.Resolved
        assertEquals(RecoveredExtrinsicOutcome.SUCCESS, state.result.outcome)
        assertNull(pending.pending(1))
        assertEquals(state.result, pending.resolution(1))
        assertEquals(listOf(101, 102, 103), source.visited)
    }

    @Test
    fun `finalized dispatch failure also safely releases pending`() = runBlocking {
        record()
        source.includedAt = 102
        source.found = RecoveredExtrinsicOutcome.FAILURE
        val state = recovery.reconcile(1, "chain") as ExtrinsicRecoveryState.Resolved
        assertEquals(RecoveredExtrinsicOutcome.FAILURE, state.result.outcome)
        assertNull(pending.pending(1))
    }

    @Test
    fun `absence before expiry stays blocked`() = runBlocking {
        record()
        source.finalized = 106
        assertTrue(recovery.reconcile(1, "chain") is ExtrinsicRecoveryState.Pending)
        assertEquals(
            BigInteger.valueOf(107),
            pending.pending(1)
                ?.nextBlock
        )
    }

    @Test
    fun `expiry needs every block of the validity window checked`() = runBlocking {
        record()
        source.finalized = 110
        val state = recovery.reconcile(1, "chain") as ExtrinsicRecoveryState.Resolved
        assertEquals(RecoveredExtrinsicOutcome.EXPIRED, state.result.outcome)
        assertEquals(
            (101..109)
                .toList(),
            source.visited
        )
        assertNull(pending.pending(1))
    }

    @Test
    fun `last valid block can contain the transaction and beats expiry`() = runBlocking {
        record()
        source.finalized = 110
        source.includedAt = 109
        source.found = RecoveredExtrinsicOutcome.SUCCESS
        val state = recovery.reconcile(1, "chain") as ExtrinsicRecoveryState.Resolved
        assertEquals(RecoveredExtrinsicOutcome.SUCCESS, state.result.outcome)
    }

    @Test
    fun `outage preserves cursor and reconstructed repositories resume from it`() = runBlocking {
        record()
        source.failAt = 104
        assertTrue(recovery.reconcile(1, "chain") is ExtrinsicRecoveryState.Pending)
        assertEquals(
            BigInteger.valueOf(104),
            pending.pending(1)
                ?.nextBlock
        )
        source.failAt = null
        source.visited.clear()
        source.includedAt = 105
        val restoredPending = ExtrinsicSubmissionJournal(preferences, gson)
        val restoredRecovery = RealExtrinsicSubmissionRecovery(restoredPending, source, logger)
        assertTrue(restoredRecovery.reconcile(1, "chain") is ExtrinsicRecoveryState.Resolved)
        assertEquals(listOf(104, 105), source.visited)
    }

    @Test
    fun `pruned or undecodable events never prove absence`() = runBlocking {
        record()
        source.finalized = 110
        source.failAt = 102
        assertTrue(recovery.reconcile(1, "chain") is ExtrinsicRecoveryState.Pending)
        assertEquals(
            BigInteger.valueOf(102),
            pending.pending(1)
                ?.nextBlock
        )
        assertNotNull(pending.pending(1))
    }

    @Test
    fun `bounded scans cannot expire without checking the remaining blocks`() = runBlocking {
        record(expiresAt = 300)
        source.finalized = 300
        assertTrue(recovery.reconcile(1, "chain") is ExtrinsicRecoveryState.Pending)
        assertEquals(64, source.visited.size)
        assertEquals(
            BigInteger.valueOf(165),
            pending.pending(1)
                ?.nextBlock
        )
    }

    @Test
    fun `changed finalized anchor stays blocked`() = runBlocking {
        record()
        source.anchorHash = "other-fork"
        assertTrue(recovery.reconcile(1, "chain") is ExtrinsicRecoveryState.Pending)
        assertTrue(source.visited.isEmpty())
        assertNotNull(pending.pending(1))
    }

    @Test
    fun `legacy hash and another chain are not cleared`() = runBlocking {
        values["bittensor_pending_submission_1"] = "old-hash"
        assertEquals(ExtrinsicRecoveryState.Pending("old-hash"), recovery.reconcile(1, "chain"))
        values.clear()
        record()
        assertTrue(recovery.reconcile(1, "different-chain") is ExtrinsicRecoveryState.Pending)
        assertTrue(source.visited.isEmpty())
    }

    @Test
    fun `wallets and stale resolution callbacks cannot clear another submission`() = runBlocking {
        record()
        pending.resolved(1, ExtrinsicSubmissionResolution("another-hash", RecoveredExtrinsicOutcome.SUCCESS))
        pending.resolved(2, ExtrinsicSubmissionResolution("hash", RecoveredExtrinsicOutcome.SUCCESS))
        assertNotNull(pending.pending(1))
        assertEquals(ExtrinsicRecoveryState.None, recovery.reconcile(2, "chain"))
    }

    @Test
    fun `pending record requires a successful durable commit`() = runBlocking {
        allowCommit = false
        assertTrue(
            runCatching { record() }
                .exceptionOrNull() is IllegalStateException
        )
        assertNull(pending.pending(1))
    }

    @Test
    fun `second submission cannot overwrite the unresolved first`() = runBlocking {
        record()
        assertTrue(
            runCatching { record() }
                .exceptionOrNull() is IllegalStateException
        )
        assertEquals("hash", pending.pendingHash(1))
    }

    @Test
    fun `cancellation preserves pending and propagates`() = runBlocking {
        record()
        source.cancel = true
        assertTrue(
            runCatching { recovery.reconcile(1, "chain") }
                .exceptionOrNull() is CancellationException
        )
        assertNotNull(pending.pending(1))
    }

    @Test
    fun `acknowledgement does not discard another receipt`() = runBlocking {
        record()
        pending.resolved(1, ExtrinsicSubmissionResolution("hash", RecoveredExtrinsicOutcome.SUCCESS))
        recovery.acknowledge(1, "another-hash")
        assertNotNull(pending.resolution(1))
        recovery.acknowledge(1, "hash")
        assertEquals(
            true,
            pending.resolution(1)
                ?.acknowledged
        )
    }

    @Test
    fun `era boundary after slow signing only extends expiry`() {
        val era = Era.Mortal(64, 36)
        assertEquals(BigInteger.valueOf(164), conservativeExtrinsicExpiry(era, BigInteger.valueOf(100)))
        assertEquals(BigInteger.valueOf(228), conservativeExtrinsicExpiry(era, BigInteger.valueOf(164)))
        assertEquals(BigInteger.valueOf(228), conservativeExtrinsicExpiry(era, BigInteger.valueOf(165)))
    }

    @Test
    fun `signed transaction is durable with exact anchor before broadcast`() = runBlocking {
        val signed = signedExtrinsic()
        recovery.recordSigned(1, "chain", signed)
        assertNotNull(pending.pendingHash(1))
        assertEquals(
            BigInteger.valueOf(106),
            pending.pending(1)
                ?.nextBlock
        )
        assertEquals(
            BigInteger.valueOf(110),
            pending.pending(1)
                ?.expiresAt
        )
        assertTrue(
            runCatching { recovery.recordSigned(1, "chain", signed) }
                .isFailure
        )
    }

    @Test
    fun `failed persistence prevents creation of recovery record`() = runBlocking {
        allowCommit = false
        val signed = signedExtrinsic()
        assertTrue(
            runCatching { recovery.recordSigned(1, "chain", signed) }
                .isFailure
        )
    }

    @Test
    fun `approval only transaction is rejected before persistence`() = runBlocking {
        val signed = signedExtrinsic("Multisig")
        assertTrue(
            runCatching { recovery.recordSigned(1, "chain", signed) }
                .isFailure
        )
        assertNull(pending.pending(1))
    }

    @Test
    fun `a scan cursor cannot skip unchecked blocks`() = runBlocking {
        record()
        assertTrue(
            runCatching { pending.advance(1, "hash", BigInteger.valueOf(110)) }
                .exceptionOrNull() is IllegalArgumentException
        )
        assertEquals(
            BigInteger.valueOf(101),
            pending.pending(1)
                ?.nextBlock
        )
    }

    private fun signedExtrinsic(moduleName: String = "SubtensorModule"): SendableExtrinsic {
        val signed = mock(SendableExtrinsic::class.java)
        val module = Module(
            moduleName,
            null,
            emptyMap(),
            null,
            emptyMap(),
            emptyMap(),
            BigInteger.ZERO
        )
        val function = MetadataFunction(
            "call",
            emptyList(),
            emptyList(),
            0 to 0
        )
        val call = GenericCall.Instance(module, function, emptyMap())
        val extrinsic = Extrinsic.Instance(Extrinsic.ExtrinsicType.Bare, call)
        `when`(signed.extrinsic).thenReturn(extrinsic)
        `when`(signed.extrinsicHex).thenReturn("0x010203")
        return signed
    }

    private suspend fun record(expiresAt: Long = 110) {
        val anchor = FinalizedBlockReference(BigInteger.valueOf(100), "block-100")
        val record = PendingExtrinsicSubmission(
            "hash",
            "chain",
            anchor,
            BigInteger.valueOf(101),
            BigInteger.valueOf(expiresAt)
        )
        pending.record(1, record)
    }

    private class FakeSource : ExtrinsicRecoverySource {

        var finalized = 105

        var includedAt: Int? = null

        var found = RecoveredExtrinsicOutcome.SUCCESS

        var failAt: Int? = null

        var anchorHash = "block-100"

        var cancel = false

        val visited = mutableListOf<Int>()

        override suspend fun finalizedBlock(chainId: String): FinalizedBlockReference {
            if (cancel) throw CancellationException()
            return FinalizedBlockReference(BigInteger.valueOf(finalized.toLong()), "block-$finalized")
        }

        override suspend fun blockHash(chainId: String, number: BigInteger): String =
            if (number == BigInteger.valueOf(100)) anchorHash else "block-$number"

        override suspend fun outcome(chainId: String, blockHash: String, transactionHash: String): RecoveredExtrinsicOutcome? {
            val height = blockHash.removePrefix("block-").toInt()
            visited += height
            if (height == failAt) error("RPC or dispatch events unavailable")
            return found.takeIf { height == includedAt }
        }

        override suspend fun expiry(chainId: String, extrinsic: SendableExtrinsic): BigInteger = BigInteger.valueOf(110)
    }
}
