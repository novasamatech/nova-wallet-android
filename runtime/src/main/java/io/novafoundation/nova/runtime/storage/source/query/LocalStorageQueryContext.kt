package io.novafoundation.nova.runtime.storage.source.query

import io.novafoundation.nova.common.data.network.runtime.binding.BlockHash
import io.novafoundation.nova.core.model.StorageEntry
import io.novafoundation.nova.core.storage.StorageCache
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novasama.substrate_sdk_android.runtime.RuntimeSnapshot
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class LocalStorageQueryContext(
    private val storageCache: StorageCache,
    chainId: ChainId,
    at: BlockHash?,
    runtime: RuntimeSnapshot,
    applyStorageDefault: Boolean
) : BaseStorageQueryContext(chainId, runtime, at, applyStorageDefault) {

    override suspend fun queryKeysByPrefix(prefix: String, at: BlockHash?): List<String> {
        return storageCache.getKeys(prefix, chainId)
    }

    override suspend fun queryEntriesByPrefix(prefix: String, at: BlockHash?): Map<String, String?> {
        return observeKeysByPrefix(prefix)
            .filter { it.isNotEmpty() }
            .first()
    }

    override suspend fun queryKeys(keys: List<String>, at: BlockHash?): Map<String, String?> {
        return storageCache.getEntries(keys, chainId).toMap()
    }

    override suspend fun queryKey(key: String, at: BlockHash?): String? {
        return storageCache.getEntry(key, chainId).content
    }

    override fun observeKey(key: String): Flow<StorageUpdate> {
        return storageCache.observeEntry(key, chainId).map {
            StorageUpdate(it.content, at = null)
        }
    }

    override fun observeKeys(keys: List<String>): Flow<Map<String, String?>> {
        return storageCache.observeEntries(keys, chainId).map { it.toMap() }
    }

    override suspend fun observeKeysByPrefix(prefix: String): Flow<Map<String, String?>> {
        return storageCache.observeEntries(prefix, chainId)
            .map { storageEntries ->
                storageEntries.associateBy(
                    keySelector = StorageEntry::storageKey,
                    valueTransform = StorageEntry::content
                )
            }
    }

    private fun List<StorageEntry>.toMap() = associateBy(
        keySelector = StorageEntry::storageKey,
        valueTransform = StorageEntry::content
    )
}
