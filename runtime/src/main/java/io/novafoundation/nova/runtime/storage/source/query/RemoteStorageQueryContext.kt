package io.novafoundation.nova.runtime.storage.source.query

import io.novafoundation.nova.common.data.network.rpc.BulkRetriever
import io.novafoundation.nova.common.data.network.rpc.retrieveAllValues
import io.novafoundation.nova.common.data.network.runtime.binding.BlockHash
import jp.co.soramitsu.fearless_utils.runtime.RuntimeSnapshot
import jp.co.soramitsu.fearless_utils.wsrpc.SocketService

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
}
