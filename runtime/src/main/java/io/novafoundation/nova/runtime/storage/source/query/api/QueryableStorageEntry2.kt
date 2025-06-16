@file:Suppress("UNCHECKED_CAST")

package io.novafoundation.nova.runtime.storage.source.query.api

import io.novafoundation.nova.common.utils.ComponentHolder
import io.novafoundation.nova.common.utils.filterNotNull
import io.novafoundation.nova.runtime.storage.source.StorageEntries
import io.novafoundation.nova.runtime.storage.source.query.StorageQueryContext
import io.novasama.substrate_sdk_android.runtime.metadata.module.StorageEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

typealias QueryableStorageBinder2<K1, K2, V> = (dynamicInstance: Any, key1: K1, key2: K2) -> V

interface QueryableStorageEntry2<I1, I2, T : Any> {

    context(StorageQueryContext)
    fun observe(argument1: I1, argument2: I2): Flow<T?>

    context(StorageQueryContext)
    suspend fun query(argument1: I1, argument2: I2): T?

    context(StorageQueryContext)
    suspend fun observe(keys: List<Pair<I1, I2>>): Flow<Map<Pair<I1, I2>, T?>>

    context(StorageQueryContext)
    suspend fun keys(argument1: I1): List<Pair<I1, I2>>

    context(StorageQueryContext)
    suspend fun entriesRaw(keys: List<Pair<I1, I2>>): StorageEntries

    context(StorageQueryContext)
    suspend fun entries(keys: List<Pair<I1, I2>>): Map<Pair<I1, I2>, T>
}

context(StorageQueryContext)
suspend fun <I1, I2, T : Any> QueryableStorageEntry2<I1, I2, T>.observeNotNull(keys: List<Pair<I1, I2>>): Flow<Map<Pair<I1, I2>, T>> {
    return observe(keys).map { it.filterNotNull() }
}

internal class RealQueryableStorageEntry2<I1, I2, T : Any>(
    private val storageEntry: StorageEntry,
    private val binding: QueryableStorageBinder2<I1, I2, T>,

    private val key1ToInternalConverter: QueryableStorageKeyToInternalBinder<I1>? = null,
    private val key2ToInternalConverter: QueryableStorageKeyToInternalBinder<I2>? = null,

    private val key1FromInternalConverter: QueryableStorageKeyFromInternalBinder<I1>? = null,
    private val key2FromInternalConverter: QueryableStorageKeyFromInternalBinder<I2>? = null,
) : QueryableStorageEntry2<I1, I2, T> {

    context(StorageQueryContext)
    override fun observe(argument1: I1, argument2: I2): Flow<T?> {
        return storageEntry.observe(
            convertKey1ToInternal(argument1),
            convertKey2ToInternal(argument2),
            binding = { decoded -> decoded?.let { binding(it, argument1, argument2) } }
        )
    }

    context(StorageQueryContext)
    override suspend fun observe(keys: List<Pair<I1, I2>>): Flow<Map<Pair<I1, I2>, T?>> {
        return storageEntry.observe(
            keysArguments = keys.toInternal(),
            keyExtractor = { components -> convertKeyFromInternal(components) },
            binding = { decoded, key -> decoded?.let { binding(it, key.first, key.second) } }
        )
    }

    context(StorageQueryContext)
    override suspend fun entriesRaw(keys: List<Pair<I1, I2>>): StorageEntries {
        return storageEntry.entriesRaw(keysArguments = keys.toInternal())
    }

    context(StorageQueryContext)
    override suspend fun entries(keys: List<Pair<I1, I2>>): Map<Pair<I1, I2>, T> {
        return storageEntry.entries(
            keysArguments = keys.toInternal(),
            keyExtractor = { components -> convertKeyFromInternal(components) },
            binding = { decoded, key -> decoded?.let { binding(it, key.first, key.second) } }
        ).filterNotNull()
    }

    context(StorageQueryContext)
    override suspend fun keys(argument1: I1): List<Pair<I1, I2>> {
        return storageEntry.keys(convertKey1ToInternal(argument1))
            .map { convertKeyFromInternal(it) }
    }

    context(StorageQueryContext)
    override suspend fun query(argument1: I1, argument2: I2): T? {
        return storageEntry.query(
            convertKey1ToInternal(argument1),
            convertKey2ToInternal(argument2),
            binding = { decoded -> decoded?.let { binding(it, argument1, argument2) } }
        )
    }

    private fun List<Pair<I1, I2>>.toInternal(): List<List<Any?>> {
        return map(::convertKeyToInternal)
    }

    private fun convertKey1ToInternal(key1: I1): Any? {
        return key1ToInternalConverter?.invoke(key1) ?: key1
    }

    private fun convertKey2ToInternal(key2: I2): Any? {
        return key2ToInternalConverter?.invoke(key2) ?: key2
    }

    private fun convertKeyToInternal(key: Pair<I1, I2>): List<Any?> {
        return listOf(
            convertKey1ToInternal(key.first),
            convertKey2ToInternal(key.second)
        )
    }

    private fun convertKeyFromInternal(componentHolder: ComponentHolder): Pair<I1, I2> {
        val first = key1FromInternalConverter?.invoke(componentHolder.component1()) ?: componentHolder.component1<Any?>() as I1
        val second = key2FromInternalConverter?.invoke(componentHolder.component2()) ?: componentHolder.component2<Any?>() as I2

        return first to second
    }
}
