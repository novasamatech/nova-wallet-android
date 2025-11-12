@file:Suppress("UNCHECKED_CAST")

package io.novafoundation.nova.runtime.storage.source.query.api

import io.novafoundation.nova.common.utils.ComponentHolder
import io.novafoundation.nova.common.utils.filterNotNull
import io.novafoundation.nova.common.utils.mapToSet
import io.novafoundation.nova.runtime.storage.source.query.StorageQueryContext
import io.novasama.substrate_sdk_android.runtime.metadata.module.StorageEntry

typealias QueryableStorageBinder3<K1, K2, K3, V> = (dynamicInstance: Any, key1: K1, key2: K2, key3: K3) -> V

interface QueryableStorageEntry3<I1, I2, I3, T : Any> {

    context(StorageQueryContext)
    suspend fun entries(keys: List<Triple<I1, I2, I3>>): Map<Triple<I1, I2, I3>, T>

    context(StorageQueryContext)
    suspend fun keys(): Set<Triple<I1, I2, I3>>
}

internal class RealQueryableStorageEntry3<I1, I2, I3, T : Any>(
    private val storageEntry: StorageEntry,
    private val binding: QueryableStorageBinder3<I1, I2, I3, T>,

    private val key1ToInternalConverter: QueryableStorageKeyToInternalBinder<I1>? = null,
    private val key2ToInternalConverter: QueryableStorageKeyToInternalBinder<I2>? = null,
    private val key3ToInternalConverter: QueryableStorageKeyToInternalBinder<I3>? = null,

    private val key1FromInternalConverter: QueryableStorageKeyFromInternalBinder<I1>? = null,
    private val key2FromInternalConverter: QueryableStorageKeyFromInternalBinder<I2>? = null,
    private val key3FromInternalConverter: QueryableStorageKeyFromInternalBinder<I3>? = null,
) : QueryableStorageEntry3<I1, I2, I3, T> {

    context(StorageQueryContext)
    override suspend fun entries(keys: List<Triple<I1, I2, I3>>): Map<Triple<I1, I2, I3>, T> {
        return storageEntry.entries(
            keysArguments = keys.toInternal(),
            keyExtractor = { components -> convertKeyFromInternal(components) },
            binding = { decoded, key -> decoded?.let { binding(it, key.first, key.second, key.third) } }
        ).filterNotNull()
    }

    context(StorageQueryContext)
    override suspend fun keys(): Set<Triple<I1, I2, I3>> {
        return storageEntry.keys().mapToSet(::convertKeyFromInternal)
    }

    private fun List<Triple<I1, I2, I3>>.toInternal(): List<List<Any?>> {
        return map(::convertKeyToInternal)
    }

    private fun convertKey1ToInternal(key1: I1): Any? {
        return key1ToInternalConverter?.invoke(key1) ?: key1
    }

    private fun convertKey2ToInternal(key2: I2): Any? {
        return key2ToInternalConverter?.invoke(key2) ?: key2
    }

    private fun convertKey3ToInternal(key3: I3): Any? {
        return key3ToInternalConverter?.invoke(key3) ?: key3
    }

    private fun convertKeyToInternal(key: Triple<I1, I2, I3>): List<Any?> {
        return listOf(
            convertKey1ToInternal(key.first),
            convertKey2ToInternal(key.second),
            convertKey3ToInternal(key.third)
        )
    }

    private fun convertKeyFromInternal(componentHolder: ComponentHolder): Triple<I1, I2, I3> {
        val first = key1FromInternalConverter?.invoke(componentHolder.component1()) ?: componentHolder.component1<Any?>() as I1
        val second = key2FromInternalConverter?.invoke(componentHolder.component2()) ?: componentHolder.component2<Any?>() as I2
        val third = key3FromInternalConverter?.invoke(componentHolder.component3()) ?: componentHolder.component3<Any?>() as I3

        return Triple(first, second, third)
    }
}
