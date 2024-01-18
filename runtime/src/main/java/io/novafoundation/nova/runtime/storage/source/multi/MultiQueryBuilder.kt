package io.novafoundation.nova.runtime.storage.source.multi

import io.novafoundation.nova.runtime.storage.source.query.DynamicInstanceBinder
import io.novafoundation.nova.runtime.storage.source.query.StorageKeyComponents
import io.novafoundation.nova.runtime.storage.source.query.wrapSingleArgumentKeys
import jp.co.soramitsu.fearless_utils.runtime.metadata.module.StorageEntry

interface MultiQueryBuilder {

    interface Descriptor<K, V> {

        fun parseKey(key: String): K

        fun parseValue(value: String?): V
    }

    interface Result {

        operator fun <K, V> get(descriptor: Descriptor<K, V>): Map<K, V>
    }

    fun <V> StorageEntry.queryKey(
        vararg args: Any?,
        binding: DynamicInstanceBinder<V>
    ): Descriptor<StorageKeyComponents, V>

    fun <K, V> StorageEntry.queryKeys(
        keysArgs: List<List<Any?>>,
        keyExtractor: (StorageKeyComponents) -> K,
        binding: DynamicInstanceBinder<V>
    ): Descriptor<K, V>

    fun <K, V> StorageEntry.querySingleArgKeys(
        keysArgs: Iterable<Any?>,
        keyExtractor: (StorageKeyComponents) -> K,
        binding: DynamicInstanceBinder<V>
    ): Descriptor<K, V> = queryKeys(keysArgs.wrapSingleArgumentKeys(), keyExtractor, binding)
}

fun <V> MultiQueryBuilder.Result.singleValueOf(descriptor: MultiQueryBuilder.Descriptor<*, V>): V = get(descriptor).values.first()
