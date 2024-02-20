package io.novafoundation.nova.common.data.network.rpc

import io.novafoundation.nova.common.data.network.runtime.binding.BlockHash
import io.novasama.substrate_sdk_android.wsrpc.SocketService

suspend fun BulkRetriever.retrieveAllValues(socketService: SocketService, keyPrefix: String, at: BlockHash? = null): Map<String, String?> {
    val allKeys = retrieveAllKeys(socketService, keyPrefix, at)

    return queryKeys(socketService, allKeys, at)
}
