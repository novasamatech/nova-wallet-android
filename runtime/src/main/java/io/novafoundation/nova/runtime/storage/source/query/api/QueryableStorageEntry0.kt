package io.novafoundation.nova.runtime.storage.source.query.api

import io.novafoundation.nova.common.utils.RuntimeContext
import io.novafoundation.nova.common.utils.createStorageKey
import io.novafoundation.nova.runtime.storage.source.query.StorageQueryContext
import io.novafoundation.nova.runtime.storage.source.query.WithRawValue
import io.novasama.substrate_sdk_android.runtime.metadata.module.StorageEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNotNull

typealias QueryableStorageBinder0<V> = (dynamicInstance: Any) -> V

interface QueryableStorageEntry0<T : Any> {

    context(StorageQueryContext)
    suspend fun query(): T?

    context(StorageQueryContext)
    suspend fun queryRaw(): String?

    context(StorageQueryContext)
    fun observe(): Flow<T?>

    context(StorageQueryContext)
    fun observeWithRaw(): Flow<WithRawValue<T?>>

    fun storageKey(): String
}

context(StorageQueryContext)
fun <T : Any> QueryableStorageEntry0<T>.observeNonNull(): Flow<T> = observe().filterNotNull()

context(StorageQueryContext)
suspend fun <T : Any> QueryableStorageEntry0<T>.queryNonNull(): T = requireNotNull(query())

internal class RealQueryableStorageEntry0<T : Any>(
    private val storageEntry: StorageEntry,
    private val binding: QueryableStorageBinder0<T>,
    runtimeContext: RuntimeContext
) : QueryableStorageEntry0<T>, RuntimeContext by runtimeContext {

    context(StorageQueryContext)
    override suspend fun query(): T? {
        return storageEntry.query(binding = { decoded -> decoded?.let(binding) })
    }

    context(StorageQueryContext)
    override fun observe(): Flow<T?> {
        return storageEntry.observe(binding = { decoded -> decoded?.let(binding) })
    }

    context(StorageQueryContext) override fun observeWithRaw(): Flow<WithRawValue<T?>> {
        return storageEntry.observeWithRaw(binding = { decoded -> decoded?.let(binding) })
    }

    context(StorageQueryContext)
    override suspend fun queryRaw(): String? {
        return storageEntry.queryRaw()
    }

    override fun storageKey(): String {
        return storageEntry.createStorageKey()
    }
}
