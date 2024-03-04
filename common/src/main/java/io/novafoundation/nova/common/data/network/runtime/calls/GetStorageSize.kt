package io.novafoundation.nova.common.data.network.runtime.calls

import io.novasama.substrate_sdk_android.wsrpc.request.runtime.RuntimeRequest

class GetStorageSize(key: String) : RuntimeRequest(
    method = "state_getStorageSize",
    params = listOfNotNull(key)
)
