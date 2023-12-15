package io.novafoundation.nova.runtime.storage.source

import io.novafoundation.nova.common.data.network.rpc.BulkRetriever
import io.novafoundation.nova.common.data.network.rpc.queryKey
import io.novafoundation.nova.common.data.network.runtime.binding.BlockHash
import io.novafoundation.nova.common.data.network.runtime.calls.GetChildStateRequest
import io.novafoundation.nova.core.updater.SubstrateSubscriptionBuilder
import io.novafoundation.nova.runtime.ethereum.StorageSharedRequestsBuilderFactory
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.getSocket
import io.novafoundation.nova.runtime.storage.source.query.RemoteStorageQueryContext
import io.novafoundation.nova.runtime.storage.source.query.StorageQueryContext
import jp.co.soramitsu.fearless_utils.runtime.RuntimeSnapshot
import jp.co.soramitsu.fearless_utils.wsrpc.executeAsync
import jp.co.soramitsu.fearless_utils.wsrpc.request.runtime.storage.SubscribeStorageRequest
import jp.co.soramitsu.fearless_utils.wsrpc.request.runtime.storage.storageChange
import jp.co.soramitsu.fearless_utils.wsrpc.subscriptionFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RemoteStorageSource(
    chainRegistry: ChainRegistry,
    sharedRequestsBuilderFactory: StorageSharedRequestsBuilderFactory,
    private val bulkRetriever: BulkRetriever,
) : BaseStorageSource(chainRegistry, sharedRequestsBuilderFactory) {

    override suspend fun query(key: String, chainId: String, at: BlockHash?): String? {
        return bulkRetriever.queryKey(getSocketService(chainId), key, at)
    }

    override suspend fun observe(key: String, chainId: String): Flow<String?> {
        return getSocketService(chainId).subscriptionFlow(SubscribeStorageRequest(key))
            .map { it.storageChange().getSingleChange() }
    }

    override suspend fun queryChildState(storageKey: String, childKey: String, chainId: String): String? {
        val response = getSocketService(chainId).executeAsync(GetChildStateRequest(storageKey, childKey))

        return response.result as? String?
    }

    override suspend fun createQueryContext(
        chainId: String,
        at: BlockHash?,
        runtime: RuntimeSnapshot,
        subscriptionBuilder: SubstrateSubscriptionBuilder?
    ): StorageQueryContext {
        return RemoteStorageQueryContext(
            bulkRetriever = bulkRetriever,
            socketService = getSocketService(chainId),
            subscriptionBuilder = subscriptionBuilder,
            chainId = chainId,
            at = at,
            runtime = runtime
        )
    }

    private suspend fun getSocketService(chainId: String) = chainRegistry.getSocket(chainId)
}
