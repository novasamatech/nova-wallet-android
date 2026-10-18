package io.novafoundation.nova.feature_staking_impl.data.bittensor

import io.novafoundation.nova.runtime.storage.source.query.StorageQueryContext
import io.novasama.substrate_sdk_android.runtime.metadata.createRequest
import io.novasama.substrate_sdk_android.runtime.metadata.decodeOutput
import io.novasama.substrate_sdk_android.runtime.metadata.method
import io.novasama.substrate_sdk_android.runtime.metadata.runtimeApi
import io.novasama.substrate_sdk_android.wsrpc.SocketService
import io.novasama.substrate_sdk_android.wsrpc.executeAsync
import io.novasama.substrate_sdk_android.wsrpc.mappers.pojo
import io.novasama.substrate_sdk_android.wsrpc.request.runtime.RuntimeRequest

/** Uses the query context's runtime metadata and the same block as its storage reads. */
internal suspend fun StorageQueryContext.callBittensorRuntimeApi(
    socket: SocketService,
    blockHash: String,
    section: String,
    method: String,
    arguments: List<Any?>
): Any? {
    val runtimeMethod = runtime.metadata.runtimeApi(section)
        .method(method)
    require(runtimeMethod.inputs.size == arguments.size)

    val namedArguments = runtimeMethod.inputs.zip(arguments).associate { (input, value) -> input.name to value }

    val request = runtimeMethod.createRequest(runtime, namedArguments)
    val atBlock = RuntimeRequest(request.method, request.params + blockHash)

    val encoded = requireNotNull(
        socket.executeAsync(atBlock, mapper = pojo<String>()).result
    )
    return runtimeMethod.decodeOutput(runtime, encoded)
}
