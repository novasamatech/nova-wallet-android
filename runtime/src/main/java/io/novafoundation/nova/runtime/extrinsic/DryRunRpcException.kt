package io.novafoundation.nova.runtime.extrinsic

import io.novasama.substrate_sdk_android.wsrpc.response.RpcResponse

class DryRunRpcException(val diagnostic: String) : Exception(diagnostic)

internal fun RpcResponse.requireDryRunRpcSuccess(method: String) {
    val failure = error ?: return
    // Never expose error.data, requests or signed extrinsics echoed by a node.
    val reason = failure.message
        .replace(Regex("(?i)0x[0-9a-f]+"), "[redacted]")
        .replace(Regex("[A-Za-z0-9+/=_-]{40,}"), "[redacted]")
        .replace(Regex("[\\p{Cntrl}\\p{Cf}]"), " ")
        .take(400)
    throw DryRunRpcException("$method: RPC ${failure.code}: $reason")
}
