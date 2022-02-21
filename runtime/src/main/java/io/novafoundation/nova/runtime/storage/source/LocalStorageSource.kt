package io.novafoundation.nova.runtime.storage.source

import io.novafoundation.nova.common.data.network.runtime.binding.BlockHash
import io.novafoundation.nova.core.model.StorageEntry
import io.novafoundation.nova.core.storage.StorageCache
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import jp.co.soramitsu.fearless_utils.runtime.RuntimeSnapshot
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class LocalStorageQueryContext(
    private val storageCache: StorageCache,
    private val chainId: ChainId,
    runtime: RuntimeSnapshot
) : BaseStorageQueryContext(runtime) {

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
}

class LocalStorageSource(
    chainRegistry: ChainRegistry,
    private val storageCache: StorageCache,
) : BaseStorageSource(chainRegistry) {

    override suspend fun query(key: String, chainId: String, at: BlockHash?): String? {
        requireWithoutAt(at)

        return storageCache.getEntry(key, chainId).content
    }

    override suspend fun queryKeys(keys: List<String>, chainId: String, at: BlockHash?): Map<String, String?> {
        requireWithoutAt(at)

        return storageCache.getEntries(keys, chainId).associateBy(
            keySelector = StorageEntry::storageKey,
            valueTransform = StorageEntry::content
        )
    }

    override suspend fun observe(key: String, chainId: String): Flow<String?> {
        return storageCache.observeEntry(key, chainId)
            .map { it.content }
    }

    override suspend fun queryChildState(storageKey: String, childKey: String, chainId: String): String? {
        throw NotImplementedError("Child state queries are not yet supported in local storage")
    }

    override suspend fun createQueryContext(chainId: String, runtime: RuntimeSnapshot): StorageQueryContext {
        return LocalStorageQueryContext(storageCache, chainId, runtime)
    }

    private fun requireWithoutAt(at: BlockHash?) = require(at == null) {
        "`At` parameter is not supported in local storage"
    }
}
