package io.novafoundation.nova.runtime.network.rpc

import io.novasama.substrate_sdk_android.wsrpc.request.runtime.RuntimeRequest

private const val METHOD = "state_getRuntimeVersion"

class StateRuntimeVersionRequest : RuntimeRequest(METHOD, listOf())
