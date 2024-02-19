package io.novafoundation.nova.common.data.network.runtime.calls

import io.novasama.substrate_sdk_android.wsrpc.request.runtime.RuntimeRequest

class FeeCalculationRequest(extrinsicInHex: String) : RuntimeRequest(
    method = "payment_queryInfo",
    params = listOf(extrinsicInHex)
)
