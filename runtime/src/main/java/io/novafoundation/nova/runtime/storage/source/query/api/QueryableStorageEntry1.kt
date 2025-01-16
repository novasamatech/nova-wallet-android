package io.novafoundation.nova.runtime.storage.source.query.api

import io.novafoundation.nova.common.utils.RuntimeContext
import io.novafoundation.nova.common.utils.createStorageKey
import io.novafoundation.nova.runtime.storage.source.query.StorageKeyComponents
import io.novafoundation.nova.runtime.storage.source.query.StorageQueryContext
import io.novafoundation.nova.runtime.storage.source.query.WithRawValue
import io.novafoundation.nova.runtime.storage.source.query.wrapSingleArgumentKeys
import io.novasama.substrate_sdk_android.runtime.metadata.module.StorageEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNotNull

typealias QueryableStorageBinder1<K, V> = (dynamicInstance: Any, key: K) -> V

interface QueryableStorageEntry1<I, T> {

    context(StorageQueryContext)
    suspend fun keys(): List<I>

    context(StorageQueryContext)
    suspend fun entries(): Map<I, T>

    context(StorageQueryContext)
    suspend fun query(argument: I): T?

    context(StorageQueryContext)
    suspend fun <K> multi(keys: List<I>, keyTransform: (I) -> K): Map<K, T?>

    context(StorageQueryContext)
    suspend fun multi(keys: List<I>): Map<I, T?>

    context(StorageQueryContext)
    suspend fun queryRaw(argument: I): String?

    context(StorageQueryContext)
    fun observe(argument: I): Flow<T?>

    context(StorageQueryContext)
    fun observeWithRaw(argument: I): Flow<WithRawValue<T?>>

    fun storageKey(argument: I): String
}

context(StorageQueryContext)
fun <I, T : Any> QueryableStorageEntry1<I, T>.observeNonNull(argument: I): Flow<T> = observe(argument).filterNotNull()

context(StorageQueryContext)
suspend fun <I, T : Any> QueryableStorageEntry1<I, T>.queryNonNull(argument: I): T = requireNotNull(query(argument))

internal class RealQueryableStorageEntry1<I, T>(
    private val storageEntry: StorageEntry,
    private val binding: QueryableStorageBinder1<I, T>,
    runtimeContext: RuntimeContext,
    @Suppress("UNCHECKED_CAST") private val keyBinding: QueryableStorageKeyBinder<I>? = null
) : QueryableStorageEntry1<I, T>, RuntimeContext by runtimeContext {

    context(StorageQueryContext)
    override suspend fun query(argument: I): T? {
        return storageEntry.query(argument, binding = { decoded -> decoded?.let { binding(it, argument) } })
    }

    context(StorageQueryContext)
    override fun observe(argument: I): Flow<T?> {
        return storageEntry.observe(argument, binding = { decoded -> decoded?.let { binding(it, argument) } })
    }

    context(StorageQueryContext)
    override suspend fun queryRaw(argument: I): String? {
        return storageEntry.queryRaw(argument)
    }

    override fun storageKey(argument: I): String {
        return storageEntry.createStorageKey(argument)
    }

    context(StorageQueryContext)
    override fun observeWithRaw(argument: I): Flow<WithRawValue<T?>> {
        return storageEntry.observeWithRaw(argument, binding = { decoded -> decoded?.let { binding(it, argument) } })
    }

    context(StorageQueryContext)
    @Suppress("UNCHECKED_CAST")
    override suspend fun <K> multi(keys: List<I>, keyTransform: (I) -> K): Map<K, T?> {
        val reverseKeyLookup = keys.associateBy(keyTransform)

        return storageEntry.entries(
            keysArguments = keys.wrapSingleArgumentKeys(),
            keyExtractor = { (key: Any?) -> keyTransform(key as I) },
            binding = { decoded, key -> decoded?.let { binding(it, reverseKeyLookup.getValue(key)) } }
        )
    }

    context(StorageQueryContext)
    override suspend fun multi(keys: List<I>): Map<I, T?> {
        return storageEntry.singleArgumentEntries(
            keysArguments = keys,
            binding = { decoded, key -> decoded?.let { binding(it, key) } }
        )
    }

    context(StorageQueryContext)
    override suspend fun keys(): List<I> {
        return storageEntry.keys().map(::bindKey)
    }

    context(StorageQueryContext)
    override suspend fun entries(): Map<I, T> {
        return storageEntry.entries(
            keyExtractor = ::bindKey,
            binding = { decoded, key -> decoded?.let { binding(it, key) } as T }
        )
    }

    private fun bindKey(storageKeyComponents: StorageKeyComponents): I {
        val firstComponent = storageKeyComponents.component1<Any?>()

        @Suppress("UNCHECKED_CAST")
        return if (firstComponent != null && keyBinding != null) {
            keyBinding.invoke(firstComponent)
        } else {
            firstComponent as I
        }
    }
}
