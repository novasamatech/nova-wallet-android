package io.novafoundation.nova.runtime.network.rpc

import io.novasama.substrate_sdk_android.wsrpc.request.runtime.RuntimeRequest

class StateCallRequest(
    runtimeRpcName: String,
    vararg params: Any
) : RuntimeRequest(
    "state_call",
    listOf(runtimeRpcName) + params
)
