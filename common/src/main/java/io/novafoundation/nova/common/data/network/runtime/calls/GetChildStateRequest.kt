package io.novafoundation.nova.common.data.network.runtime.calls

import io.novasama.substrate_sdk_android.wsrpc.request.runtime.RuntimeRequest

class GetChildStateRequest(
    storageKey: String,
    childKey: String
) : RuntimeRequest(
    method = "childstate_getStorage",
    params = listOf(childKey, storageKey)
)
