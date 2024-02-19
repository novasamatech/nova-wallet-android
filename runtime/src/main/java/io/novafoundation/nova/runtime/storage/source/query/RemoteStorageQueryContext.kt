package io.novafoundation.nova.runtime.storage.source.query

import io.novafoundation.nova.common.data.network.rpc.BulkRetriever
import io.novafoundation.nova.common.data.network.rpc.queryKey
import io.novafoundation.nova.common.data.network.rpc.retrieveAllValues
import io.novafoundation.nova.common.data.network.runtime.binding.BlockHash
import io.novafoundation.nova.core.updater.SubstrateSubscriptionBuilder
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novasama.substrate_sdk_android.runtime.RuntimeSnapshot
import io.novasama.substrate_sdk_android.wsrpc.SocketService
import io.novasama.substrate_sdk_android.wsrpc.request.runtime.storage.SubscribeStorageRequest
import io.novasama.substrate_sdk_android.wsrpc.request.runtime.storage.storageChange
import io.novasama.substrate_sdk_android.wsrpc.subscriptionFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RemoteStorageQueryContext(
    private val bulkRetriever: BulkRetriever,
    private val socketService: SocketService,
    private val subscriptionBuilder: SubstrateSubscriptionBuilder?,
    chainId: ChainId,
    at: BlockHash?,
    runtime: RuntimeSnapshot
) : BaseStorageQueryContext(chainId, runtime, at) {

    override suspend fun queryKeysByPrefix(prefix: String, at: BlockHash?): List<String> {
        return bulkRetriever.retrieveAllKeys(socketService, prefix, at)
    }

    override suspend fun queryEntriesByPrefix(prefix: String, at: BlockHash?): Map<String, String?> {
        return bulkRetriever.retrieveAllValues(socketService, prefix, at)
    }

    override suspend fun queryKeys(keys: List<String>, at: BlockHash?): Map<String, String?> {
        return bulkRetriever.queryKeys(socketService, keys, at)
    }

    override suspend fun queryKey(key: String, at: BlockHash?): String? {
        return bulkRetriever.queryKey(socketService, key, at)
    }

    @Suppress("IfThenToElvis")
    override fun observeKey(key: String): Flow<String?> {
        return if (subscriptionBuilder != null) {
            subscriptionBuilder.subscribe(key).map { it.value }
        } else {
            socketService.subscriptionFlow(SubscribeStorageRequest(key))
                .map { it.storageChange().getSingleChange() }
        }
    }

    override suspend fun observeKeys(keys: List<String>): Flow<Map<String, String?>> {
        TODO("Not yet needed")
    }

    override suspend fun observeKeysByPrefix(prefix: String): Flow<Map<String, String?>> {
        TODO("Not yet supported")
    }
}
