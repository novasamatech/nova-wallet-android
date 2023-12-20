package io.novafoundation.nova.common.data.network.runtime.calls

import jp.co.soramitsu.fearless_utils.wsrpc.request.runtime.RuntimeRequest

class GetStorageSize(key: String) : RuntimeRequest(
    method = "state_getStorageSize",
    params = listOfNotNull(key)
)
