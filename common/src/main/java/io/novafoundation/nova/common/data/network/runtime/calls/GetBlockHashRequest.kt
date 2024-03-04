package io.novafoundation.nova.common.data.network.runtime.calls

import io.novafoundation.nova.common.data.network.runtime.binding.BlockNumber
import io.novasama.substrate_sdk_android.wsrpc.request.runtime.RuntimeRequest

class GetBlockHashRequest(blockNumber: BlockNumber?) : RuntimeRequest(
    method = "chain_getBlockHash",
    params = listOfNotNull(
        blockNumber
    )
)
