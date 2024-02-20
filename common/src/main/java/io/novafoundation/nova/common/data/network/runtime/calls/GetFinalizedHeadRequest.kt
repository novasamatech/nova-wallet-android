package io.novafoundation.nova.common.data.network.runtime.calls

import io.novasama.substrate_sdk_android.wsrpc.request.runtime.RuntimeRequest

object GetFinalizedHeadRequest : RuntimeRequest(
    method = "chain_getFinalizedHead",
    params = emptyList()
)
