package io.novafoundation.nova.core.storage

import io.novafoundation.nova.core.model.StorageEntry
import kotlinx.coroutines.flow.Flow

interface StorageCache {

    suspend fun isPrefixInCache(prefixKey: String, chainId: String): Boolean

    suspend fun isFullKeyInCache(fullKey: String, chainId: String): Boolean

    suspend fun insert(entry: StorageEntry, chainId: String)

    suspend fun insert(entries: List<StorageEntry>, chainId: String)

    suspend fun insertPrefixEntries(entries: List<StorageEntry>, prefixKey: String, chainId: String)

    suspend fun removeByPrefix(prefixKey: String, chainId: String)
    suspend fun removeByPrefixExcept(
        prefixKey: String,
        fullKeyExceptions: List<String>,
        chainId: String
    )

    fun observeEntry(key: String, chainId: String): Flow<StorageEntry>

    /**
     * First result will be emitted when all keys are found in the cache
     * Thus, result.size == fullKeys.size
     */
    fun observeEntries(keys: List<String>, chainId: String): Flow<List<StorageEntry>>

    suspend fun observeEntries(keyPrefix: String, chainId: String): Flow<List<StorageEntry>>

    /**
     * Should suspend until any matched result found
     */
    suspend fun getEntry(key: String, chainId: String): StorageEntry

    suspend fun filterKeysInCache(keys: List<String>, chainId: String): List<String>

    suspend fun getKeys(keyPrefix: String, chainId: String): List<String>

    /**
     * Should suspend until all keys will be found
     * Thus, result.size == fullKeys.size
     */
    suspend fun getEntries(fullKeys: List<String>, chainId: String): List<StorageEntry>
}

suspend fun StorageCache.insert(entries: Map<String, String?>, chainId: String) {
    val changes = entries.map { (key, value) -> StorageEntry(key, value) }

    insert(changes, chainId)
}

suspend fun StorageCache.insertPrefixEntries(entries: Map<String, String?>, prefix: String, chainId: String) {
    val changes = entries.map { (key, value) -> StorageEntry(key, value) }

    insertPrefixEntries(changes, prefixKey = prefix, chainId = chainId)
}
