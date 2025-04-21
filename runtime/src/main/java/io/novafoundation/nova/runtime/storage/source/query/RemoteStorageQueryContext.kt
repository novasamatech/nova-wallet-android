package io.novafoundation.nova.runtime.storage.source.query

import io.novafoundation.nova.common.data.network.rpc.BulkRetriever
import io.novafoundation.nova.common.data.network.rpc.queryKey
import io.novafoundation.nova.common.data.network.rpc.retrieveAllValues
import io.novafoundation.nova.common.data.network.runtime.binding.BlockHash
import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.common.utils.flowOfAll
import io.novafoundation.nova.common.utils.toMultiSubscription
import io.novafoundation.nova.common.utils.withFlowScope
import io.novafoundation.nova.core.updater.SubstrateSubscriptionBuilder
import io.novafoundation.nova.runtime.ethereum.StorageSharedRequestsBuilderFactory
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novasama.substrate_sdk_android.runtime.RuntimeSnapshot
import io.novasama.substrate_sdk_android.wsrpc.SocketService
import io.novasama.substrate_sdk_android.wsrpc.request.runtime.storage.SubscribeStorageRequest
import io.novasama.substrate_sdk_android.wsrpc.request.runtime.storage.storageChange
import io.novasama.substrate_sdk_android.wsrpc.subscriptionFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlin.coroutines.coroutineContext

class RemoteStorageQueryContext(
    private val bulkRetriever: BulkRetriever,
    private val socketService: SocketService,
    private val subscriptionBuilder: SubstrateSubscriptionBuilder?,
    chainId: ChainId,
    at: BlockHash?,
    runtime: RuntimeSnapshot,
    applyStorageDefault: Boolean
) : BaseStorageQueryContext(chainId, runtime, at, applyStorageDefault) {

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
    override fun observeKey(key: String): Flow<StorageUpdate> {
        return if (subscriptionBuilder != null) {
            subscriptionBuilder.subscribe(key).map {
                StorageUpdate(
                    value = it.value,
                    at = it.block
                )
            }
        } else {
            socketService.subscriptionFlow(SubscribeStorageRequest(key))
                .map {
                    val storageChange = it.storageChange()

                    StorageUpdate(
                        value = storageChange.getSingleChange(),
                        at = storageChange.block
                    )
                }
        }
    }

    // TODO To this is not quite efficient implementation as we are de-multiplexing arrived keys into multiple flows (in sdk) and them merging them back
    // Instead, we should allow batch subscriptions on sdk level
    override fun observeKeys(keys: List<String>): Flow<Map<String, String?>> {
        requireNotNull(subscriptionBuilder) {
            "Cannot perform batched subscription without a builder. Have you forgot to call 'subscribeBatched()` instead of `subscribe()?`"
        }

        return keys.map { key ->
            subscriptionBuilder.subscribe(key).map { key to it.value }
        }.toMultiSubscription(keys.size)
    }

    override suspend fun observeKeysByPrefix(prefix: String): Flow<Map<String, String?>> {
        TODO("Not yet supported")
    }
}
