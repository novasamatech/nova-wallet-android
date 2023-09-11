package io.novafoundation.nova.runtime.storage.cache

import io.novafoundation.nova.core.storage.StorageCache
import io.novafoundation.nova.runtime.storage.source.query.WithRawValue
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

interface StorageCachingContext {

    val storageCache: StorageCache
}

context(StorageCachingContext)
fun <T> Flow<WithRawValue<T>>.cacheValues(): Flow<T> {
    return map {
        storageCache.insert(it.raw, it.chainId)

        it.value
    }
}

fun StorageCachingContext(storageCache: StorageCache): StorageCachingContext {
    return InlineStorageCachingContext(storageCache)
}

@JvmInline
private value class InlineStorageCachingContext(override val storageCache: StorageCache) : StorageCachingContext
