package io.novafoundation.nova.runtime.network.rpc

import jp.co.soramitsu.fearless_utils.wsrpc.request.runtime.RuntimeRequest

class StateCallRequest(
    runtimeRpcName: String,
    vararg params: Any
): RuntimeRequest(
    "state_call",
    listOf(runtimeRpcName) + params
)
