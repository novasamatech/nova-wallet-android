package io.novafoundation.nova.common.data.network.runtime.calls

import io.novafoundation.nova.common.data.network.runtime.binding.BlockHash
import io.novasama.substrate_sdk_android.wsrpc.request.runtime.RuntimeRequest

class GetReadProof(keys: List<String>, at: BlockHash?) : RuntimeRequest(
    method = "state_getReadProof",
    params = listOfNotNull(keys, at)
)
