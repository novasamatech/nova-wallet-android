package io.novafoundation.nova.common.data.network.runtime.calls

import io.novasama.substrate_sdk_android.wsrpc.request.runtime.RuntimeRequest

class GetSystemPropertiesRequest : RuntimeRequest(
    method = "system_properties",
    params = emptyList()
)
