package io.novafoundation.nova.common.data.network.runtime.calls

import io.novasama.substrate_sdk_android.wsrpc.request.runtime.RuntimeRequest

class GetChainRequest : RuntimeRequest(
    method = "system_chain",
    params = emptyList()
)
