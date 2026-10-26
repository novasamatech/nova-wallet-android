package io.novafoundation.nova.runtime.extrinsic

import io.novasama.substrate_sdk_android.wsrpc.subscription.response.SubscriptionChange
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExtrinsicStatusTest {

    private val extrinsicHash = "0xoriginal"

    @Test
    fun `invalid and dropped are terminal failures`() {
        val invalid = decode("invalid")
        val dropped = decode("dropped")

        assertTrue(invalid is ExtrinsicStatus.Invalid)
        assertTrue(dropped is ExtrinsicStatus.Dropped)
        listOf(invalid, dropped).forEach {
            assertTrue(it.terminal)
            assertEquals(extrinsicHash, it.extrinsicHash)
        }
    }

    @Test
    fun `usurped preserves original and replacement hashes`() {
        val status = decode(mapOf("usurped" to "0xreplacement")) as ExtrinsicStatus.Usurped

        assertTrue(status.terminal)
        assertEquals(extrinsicHash, status.extrinsicHash)
        assertEquals("0xreplacement", status.replacementHash)
    }

    @Test
    fun `finality timeout is a watch failure not finalized`() {
        val status = decode(mapOf("finalityTimeout" to "0xblock"))

        assertTrue(status is ExtrinsicStatus.FinalityTimeout)
        assertFalse(status is ExtrinsicStatus.Finalized)
        assertTrue(status.terminal)
        assertEquals("0xblock", (status as ExtrinsicStatus.FinalityTimeout).blockHash)
        assertEquals(extrinsicHash, status.extrinsicHash)
    }

    @Test
    fun `inclusion and actual finalization retain their semantics`() {
        val included = decode(mapOf("inBlock" to "0xblock")) as ExtrinsicStatus.InBlock
        val finalized = decode(mapOf("finalized" to "0xblock")) as ExtrinsicStatus.Finalized

        assertFalse(included.terminal)
        assertTrue(finalized.terminal)
        assertEquals("0xblock", included.blockHash)
        assertEquals("0xblock", finalized.blockHash)
    }

    @Test
    fun `progress retraction and unknown statuses remain non terminal`() {
        assertTrue(decode("ready") is ExtrinsicStatus.Ready)
        assertTrue(decode(mapOf("broadcast" to listOf("peer"))) is ExtrinsicStatus.Broadcast)
        listOf("future", mapOf("retracted" to "0xblock"), mapOf("future" to "value")).forEach {
            val status = decode(it)
            assertTrue(status is ExtrinsicStatus.Other)
            assertFalse(status.terminal)
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun `malformed usurped payload is rejected`() {
        decode(mapOf("usurped" to 123))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `malformed finality timeout payload is rejected`() {
        decode(mapOf("finalityTimeout" to null))
    }

    private fun decode(result: Any): ExtrinsicStatus {
        val params = SubscriptionChange.Params(result, "subscription")
        val change = SubscriptionChange("2.0", "author_extrinsicUpdate", params)
        return change.asExtrinsicStatus(extrinsicHash)
    }
}
