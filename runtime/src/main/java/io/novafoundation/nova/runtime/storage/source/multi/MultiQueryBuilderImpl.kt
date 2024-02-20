package io.novafoundation.nova.runtime.storage.source.multi

import io.novafoundation.nova.common.utils.splitKeyToComponents
import io.novafoundation.nova.runtime.storage.source.multi.MultiQueryBuilder.Descriptor
import io.novafoundation.nova.runtime.storage.source.query.DynamicInstanceBinder
import io.novafoundation.nova.runtime.storage.source.query.StorageKeyComponents
import io.novasama.substrate_sdk_android.runtime.RuntimeSnapshot
import io.novasama.substrate_sdk_android.runtime.definitions.types.fromHex
import io.novasama.substrate_sdk_android.runtime.metadata.module.StorageEntry
import io.novasama.substrate_sdk_android.runtime.metadata.storageKey
import io.novasama.substrate_sdk_android.runtime.metadata.storageKeys

class MultiQueryBuilderImpl(
    private val runtime: RuntimeSnapshot
) : MultiQueryBuilder {

    private val descriptors: MutableMap<Descriptor<*, *>, List<String>> = mutableMapOf()
    private val keys: MutableMap<StorageEntry, MutableList<String>> = mutableMapOf()

    override fun <V> StorageEntry.queryKey(
        vararg args: Any?,
        binding: DynamicInstanceBinder<V>
    ): Descriptor<StorageKeyComponents, V> {
        val key = storageKey(runtime, *args)

        keysForEntry(this).add(key)
        return registerDescriptor(listOf(key), this, keyExtractor = { it }, binding)
    }

    override fun <K, V> StorageEntry.queryKeys(
        keysArgs: List<List<Any?>>,
        keyExtractor: (StorageKeyComponents) -> K,
        binding: DynamicInstanceBinder<V>
    ): Descriptor<K, V> {
        val keys = storageKeys(runtime, keysArgs)

        keysForEntry(this).addAll(keys)
        return registerDescriptor(keys, this, keyExtractor, binding)
    }

    fun descriptors(): Map<Descriptor<*, *>, List<String>> {
        return descriptors
    }

    fun keys(): Map<StorageEntry, List<String>> {
        return keys
    }

    private fun keysForEntry(entry: StorageEntry) = keys.getOrPut(entry, ::mutableListOf)

    private fun <K, V> registerDescriptor(
        keys: List<String>,
        storageEntry: StorageEntry,
        keyExtractor: (StorageKeyComponents) -> K,
        binding: DynamicInstanceBinder<V>
    ): Descriptor<K, V> {
        val newDescriptor = RealDescriptor(storageEntry, keyExtractor, binding)
        descriptors[newDescriptor] = keys

        return newDescriptor
    }

    private inner class RealDescriptor<K, V>(
        private val storageEntry: StorageEntry,
        private val keyExtractor: (StorageKeyComponents) -> K,
        private val valueBinding: (decoded: Any?) -> V
    ) : Descriptor<K, V> {
        override fun parseKey(key: String): K {
            val keyComponents = storageEntry.splitKeyToComponents(runtime, key)

            return keyExtractor(keyComponents)
        }

        override fun parseValue(value: String?): V {
            val valueType = storageEntry.type.value!!
            val decoded = value?.let { valueType.fromHex(runtime, value) }

            return valueBinding(decoded)
        }
    }
}
