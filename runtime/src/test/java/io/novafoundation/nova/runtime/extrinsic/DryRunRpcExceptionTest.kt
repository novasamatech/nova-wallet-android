package io.novafoundation.nova.runtime.extrinsic

import io.novasama.substrate_sdk_android.wsrpc.response.RpcError
import io.novasama.substrate_sdk_android.wsrpc.response.RpcResponse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DryRunRpcExceptionTest {

    @Test
    fun `preserves method code and reason`() {
        val error = RpcError(-32601, "Method not found")
        val response = RpcResponse("2.0", null, 1, error)
        val exception = runCatching {
            response.requireDryRunRpcSuccess("system_dryRun")
        }.exceptionOrNull() as DryRunRpcException
        assertEquals("system_dryRun: RPC -32601: Method not found", exception.diagnostic)
    }

    @Test
    fun `redacts echoed payloads and limits message`() {
        val payload = "0x" + "ab".repeat(200)
        val address = "5DkTDSQZU3r6oFA1ywyNJos14iqg2NF4WJrBBZRjg1rRNqsn"
        val error = RpcError(-32602, "Invalid params\n$payload $address " + "reason ".repeat(200))
        val response = RpcResponse("2.0", null, 1, error)
        val exception = runCatching {
            response.requireDryRunRpcSuccess("system_dryRun")
        }.exceptionOrNull() as DryRunRpcException
        assertTrue(exception.diagnostic.contains("Invalid params"))
        assertFalse(exception.diagnostic.contains(payload))
        assertFalse(exception.diagnostic.contains(address))
        assertFalse(exception.diagnostic.contains('\n'))
        assertTrue(exception.diagnostic.length < 450)
    }

    @Test
    fun `successful response is not rejected`() {
        val response = RpcResponse("2.0", "0x0000", 1, null)
        response.requireDryRunRpcSuccess("system_dryRun")
    }
}
