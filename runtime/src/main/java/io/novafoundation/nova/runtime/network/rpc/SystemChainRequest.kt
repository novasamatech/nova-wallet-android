package io.novafoundation.nova.runtime.network.rpc

import io.novasama.substrate_sdk_android.wsrpc.request.runtime.RuntimeRequest

class SystemChainRequest : RuntimeRequest("system_chain", listOf())

