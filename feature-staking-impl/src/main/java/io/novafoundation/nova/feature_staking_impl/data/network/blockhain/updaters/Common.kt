package io.novafoundation.nova.feature_staking_impl.data.network.blockhain.updaters

import io.novafoundation.nova.common.data.network.rpc.BulkRetriever
import io.novafoundation.nova.common.utils.staking
import io.novafoundation.nova.core.model.StorageEntry
import io.novafoundation.nova.core.storage.StorageCache
import io.novasama.substrate_sdk_android.runtime.metadata.RuntimeMetadata
import io.novasama.substrate_sdk_android.runtime.metadata.storage
import io.novasama.substrate_sdk_android.runtime.metadata.storageKey
import io.novasama.substrate_sdk_android.wsrpc.SocketService

fun RuntimeMetadata.activeEraStorageKey() = staking().storage("ActiveEra").storageKey()

suspend fun BulkRetriever.fetchValuesToCache(
    socketService: SocketService,
    keys: List<String>,
    storageCache: StorageCache,
    chainId: String,
) {
    val allValues = queryKeys(socketService, keys)

    val toInsert = allValues.map { (key, value) -> StorageEntry(key, value) }

    storageCache.insert(toInsert, chainId)
}

/**
 * @return number of fetched keys
 */
suspend fun BulkRetriever.fetchPrefixValuesToCache(
    socketService: SocketService,
    prefix: String,
    storageCache: StorageCache,
    chainId: String
): Int {
    val allKeys = retrieveAllKeys(socketService, prefix)

    if (allKeys.isNotEmpty()) {
        fetchValuesToCache(socketService, allKeys, storageCache, chainId)
    }

    return allKeys.size
}
