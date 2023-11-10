package io.novafoundation.nova.runtime.network.rpc

import jp.co.soramitsu.fearless_utils.wsrpc.request.runtime.RuntimeRequest

private const val METHOD = "state_getRuntimeVersion"

class StateRuntimeVersionRequest : RuntimeRequest(METHOD, listOf())
