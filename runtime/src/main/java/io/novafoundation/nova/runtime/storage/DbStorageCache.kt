package io.novafoundation.nova.runtime.storage

import io.novafoundation.nova.common.utils.mapList
import io.novafoundation.nova.core.model.StorageEntry
import io.novafoundation.nova.core.storage.StorageCache
import io.novafoundation.nova.core_db.dao.StorageDao
import io.novafoundation.nova.core_db.model.StorageEntryLocal
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class DbStorageCache(
    private val storageDao: StorageDao
) : StorageCache {

    override suspend fun isPrefixInCache(prefixKey: String, chainId: String): Boolean {
        return storageDao.isPrefixInCache(chainId, prefixKey)
    }

    override suspend fun isFullKeyInCache(fullKey: String, chainId: String): Boolean {
        return storageDao.isFullKeyInCache(chainId, fullKey)
    }

    override suspend fun insert(entry: StorageEntry, chainId: String) = withContext(Dispatchers.IO) {
        storageDao.insert(mapStorageEntryToLocal(entry, chainId))
    }

    override suspend fun insert(entries: List<StorageEntry>, chainId: String) = withContext(Dispatchers.IO) {
        val mapped = entries.map { mapStorageEntryToLocal(it, chainId) }

        storageDao.insert(mapped)
    }

    override suspend fun insertPrefixEntries(entries: List<StorageEntry>, prefixKey: String, chainId: String) {
        val mapped = entries.map { mapStorageEntryToLocal(it, chainId) }

        storageDao.insertPrefixedEntries(mapped, prefix = prefixKey, chainId = chainId)
    }

    override suspend fun removeByPrefix(prefixKey: String, chainId: String) {
        storageDao.removeByPrefix(prefix = prefixKey, chainId = chainId)
    }

    override suspend fun removeByPrefixExcept(prefixKey: String, fullKeyExceptions: List<String>, chainId: String) {
        storageDao.removeByPrefixExcept(prefixKey, fullKeyExceptions, chainId)
    }

    override fun observeEntry(key: String, chainId: String): Flow<StorageEntry> {
        return storageDao.observeEntry(chainId, key)
            .filterNotNull()
            .map { mapStorageEntryFromLocal(it) }
            .distinctUntilChangedBy(StorageEntry::content)
    }

    override suspend fun observeEntries(keys: List<String>, chainId: String): Flow<List<StorageEntry>> {
        return storageDao.observeEntries(chainId, keys)
            .filter { it.size == keys.size }
            .mapList { mapStorageEntryFromLocal(it) }
    }

    override suspend fun observeEntries(keyPrefix: String, chainId: String): Flow<List<StorageEntry>> {
        return storageDao.observeEntries(chainId, keyPrefix)
            .mapList { mapStorageEntryFromLocal(it) }
    }

    override suspend fun getEntry(key: String, chainId: String): StorageEntry = observeEntry(key, chainId).first()

    override suspend fun filterKeysInCache(keys: List<String>, chainId: String): List<String> {
        return storageDao.filterKeysInCache(chainId, keys)
    }

    override suspend fun getEntries(fullKeys: List<String>, chainId: String): List<StorageEntry> {
        return observeEntries(fullKeys, chainId).first()
    }

    override suspend fun getKeys(keyPrefix: String, chainId: String): List<String> {
        return storageDao.getKeys(chainId, keyPrefix)
    }
}

private fun mapStorageEntryToLocal(
    storageEntry: StorageEntry,
    chainId: String
) = with(storageEntry) {
    StorageEntryLocal(
        storageKey = storageKey,
        content = content,
        chainId = chainId
    )
}

private fun mapStorageEntryFromLocal(
    storageEntryLocal: StorageEntryLocal
) = with(storageEntryLocal) {
    StorageEntry(
        storageKey = storageKey,
        content = content
    )
}
