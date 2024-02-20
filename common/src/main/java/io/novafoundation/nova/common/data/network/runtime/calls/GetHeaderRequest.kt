package io.novafoundation.nova.common.data.network.runtime.calls

import io.novasama.substrate_sdk_android.wsrpc.request.runtime.RuntimeRequest

class GetHeaderRequest(blockHash: String? = null) : RuntimeRequest(
    method = "chain_getHeader",
    params = listOfNotNull(
        blockHash
    )
)
