package io.novafoundation.nova.runtime.storage.source.query

import io.novafoundation.nova.common.data.network.rpc.BulkRetriever
import io.novafoundation.nova.common.data.network.rpc.queryKey
import io.novafoundation.nova.common.data.network.rpc.retrieveAllValues
import io.novafoundation.nova.common.data.network.runtime.binding.BlockHash
import jp.co.soramitsu.fearless_utils.runtime.RuntimeSnapshot
import jp.co.soramitsu.fearless_utils.wsrpc.SocketService
import jp.co.soramitsu.fearless_utils.wsrpc.request.runtime.storage.SubscribeStorageRequest
import jp.co.soramitsu.fearless_utils.wsrpc.request.runtime.storage.storageChange
import jp.co.soramitsu.fearless_utils.wsrpc.subscriptionFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RemoteStorageQueryContext(
    private val bulkRetriever: BulkRetriever,
    private val socketService: SocketService,
    at: BlockHash?,
    runtime: RuntimeSnapshot
) : BaseStorageQueryContext(runtime, at) {

    override suspend fun queryKeysByPrefix(prefix: String): List<String> {
        return bulkRetriever.retrieveAllKeys(socketService, prefix)
    }

    override suspend fun queryEntriesByPrefix(prefix: String): Map<String, String?> {
        return bulkRetriever.retrieveAllValues(socketService, prefix)
    }

    override suspend fun queryKeys(keys: List<String>, at: BlockHash?): Map<String, String?> {
        return bulkRetriever.queryKeys(socketService, keys, at)
    }

    override suspend fun queryKey(key: String, at: BlockHash?): String? {
        return bulkRetriever.queryKey(socketService, key, at)
    }

    override suspend fun observeKey(key: String): Flow<String?> {
        return socketService.subscriptionFlow(SubscribeStorageRequest(key))
            .map { it.storageChange().getSingleChange() }
    }
}
