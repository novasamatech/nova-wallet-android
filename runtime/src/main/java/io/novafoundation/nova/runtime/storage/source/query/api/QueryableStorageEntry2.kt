package io.novafoundation.nova.runtime.storage.source.query.api

import io.novafoundation.nova.runtime.storage.source.query.StorageQueryContext
import jp.co.soramitsu.fearless_utils.runtime.metadata.module.StorageEntry
import kotlinx.coroutines.flow.Flow

typealias QueryableStorageBinder2<K1, K2, V> = (dynamicInstance: Any, key1: K1, key2: K2) -> V

interface QueryableStorageEntry2<I1, I2, T : Any> {

    context(StorageQueryContext)
    fun observe(argument1: I1, argument2: I2): Flow<T?>
}

internal class RealQueryableStorageEntry2<I1, I2, T : Any>(
    private val storageEntry: StorageEntry,
    private val binding: QueryableStorageBinder2<I1, I2, T>,
) : QueryableStorageEntry2<I1, I2, T> {

    context(StorageQueryContext)
    override fun observe(argument1: I1, argument2: I2): Flow<T?> {
        return storageEntry.observe(argument1, argument2, binding = { decoded -> decoded?.let { binding(it, argument1, argument2) } })
    }
}
