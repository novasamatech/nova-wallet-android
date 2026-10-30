package io.novafoundation.nova.common.data.network

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NodeLogFilterTest {

    @Test
    fun `filters socket and state machine messages including tab prefixes`() {
        assertTrue(isNodeLog("[STATE MACHINE][TRANSITION] Connecting -> Connected"))
        assertTrue(isNodeLog("[STATE MACHINE][SIDE EFFECT] Send"))
        assertTrue(isNodeLog("\t[SOCKET][ERROR] Connection closed"))
        assertTrue(isNodeLog("[RPC] response"))
    }

    @Test
    fun `preserves operation diagnostics and application errors`() {
        assertFalse(isNodeLog("[BittensorExit] Submission failed"))
        assertFalse(isNodeLog("Unexpected error"))
        assertFalse(isNodeLog(null))
    }
}
