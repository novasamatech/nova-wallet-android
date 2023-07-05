package io.novafoundation.nova.runtime.storage.source.query.api

import io.novafoundation.nova.runtime.storage.source.query.StorageQueryContext
import jp.co.soramitsu.fearless_utils.runtime.metadata.module.StorageEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNotNull

typealias QueryableStorageBinder1<K, V> = (dynamicInstance: Any, key: K) -> V

interface QueryableStorageEntry1<I, T : Any> {

    context(StorageQueryContext)
    suspend fun query(argument: I): T?

    context(StorageQueryContext)
    suspend fun queryRaw(argument: I): String?

    context(StorageQueryContext)
    suspend fun observe(argument: I): Flow<T?>
}

context(StorageQueryContext)
suspend fun <I, T : Any> QueryableStorageEntry1<I, T>.observeNonNull(argument: I): Flow<T> = observe(argument).filterNotNull()

internal class RealQueryableStorageEntry1<I, T : Any>(
    private val storageEntry: StorageEntry,
    private val binding: QueryableStorageBinder1<I, T>
) : QueryableStorageEntry1<I, T> {

    context(StorageQueryContext)
    override suspend fun query(argument: I): T? {
        return storageEntry.query(argument, binding = { decoded -> decoded?.let { binding(it, argument) } })
    }

    context(StorageQueryContext)
    override suspend fun observe(argument: I): Flow<T?> {
        return storageEntry.observe(argument, binding = { decoded -> decoded?.let { binding(it, argument) } })
    }

    context(StorageQueryContext)
    override suspend fun queryRaw(argument: I): String? {
        return storageEntry.queryRaw(argument)
    }
}
