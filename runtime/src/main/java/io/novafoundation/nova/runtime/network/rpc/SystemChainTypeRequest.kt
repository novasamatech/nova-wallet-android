package io.novafoundation.nova.runtime.network.rpc

import io.novasama.substrate_sdk_android.wsrpc.request.runtime.RuntimeRequest

class SystemChainTypeRequest : RuntimeRequest("system_chainType", listOf())

