package io.novafoundation.nova.runtime.storage.source.query

import io.novafoundation.nova.common.data.network.runtime.binding.BlockHash
import io.novafoundation.nova.core.model.StorageEntry
import io.novafoundation.nova.core.storage.StorageCache
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import jp.co.soramitsu.fearless_utils.runtime.RuntimeSnapshot
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first

class LocalStorageQueryContext(
    private val storageCache: StorageCache,
    private val chainId: ChainId,
    at: BlockHash?,
    runtime: RuntimeSnapshot
) : BaseStorageQueryContext(runtime, at) {

    override suspend fun queryKeysByPrefix(prefix: String): List<String> {
        return storageCache.getKeys(prefix, chainId)
    }

    override suspend fun queryEntriesByPrefix(prefix: String): Map<String, String?> {
        val entries = storageCache.observeEntries(prefix, chainId)
            .filter { it.isNotEmpty() }
            .first()

        return entries.associateBy(
            keySelector = StorageEntry::storageKey,
            valueTransform = StorageEntry::content
        )
    }

    override suspend fun queryKeys(keys: List<String>, at: BlockHash?): Map<String, String?> {
        return storageCache.getEntries(keys, chainId).associateBy(
            keySelector = StorageEntry::storageKey,
            valueTransform = StorageEntry::content
        )
    }

    override suspend fun queryKey(key: String, at: BlockHash?): String? {
        return storageCache.getEntry(key, chainId).content
    }
}
