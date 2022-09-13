package io.novafoundation.nova.runtime.storage.source.query

import io.novafoundation.nova.common.utils.ComponentHolder
import io.novafoundation.nova.runtime.storage.source.multi.MultiQueryBuilder
import jp.co.soramitsu.fearless_utils.runtime.RuntimeSnapshot
import jp.co.soramitsu.fearless_utils.runtime.metadata.module.StorageEntry
import kotlinx.coroutines.flow.Flow

typealias StorageKeyComponents = ComponentHolder
typealias DynamicInstanceBinder<V> = (dynamicInstance: Any?) -> V
typealias DynamicInstanceBinderWithKey<K, V> = (dynamicInstance: Any?, key: K) -> V

interface StorageQueryContext {

    val runtime: RuntimeSnapshot

    suspend fun StorageEntry.keys(vararg prefixArgs: Any?): List<StorageKeyComponents>

    suspend fun <V> StorageEntry.observe(
        vararg keyArguments: Any?,
        binding: DynamicInstanceBinder<V>
    ): Flow<V>

    suspend fun <K, V> StorageEntry.observe(
        keysArguments: List<List<Any?>>,
        keyExtractor: (StorageKeyComponents) -> K,
        binding: DynamicInstanceBinderWithKey<K, V>
    ): Flow<Map<K, V>>

    suspend fun <K, V> StorageEntry.observeByPrefix(
        vararg prefixArgs: Any?,
        keyExtractor: (StorageKeyComponents) -> K,
        binding: DynamicInstanceBinderWithKey<K, V>
    ): Flow<Map<K, V>>

    suspend fun <K, V> StorageEntry.entries(
        vararg prefixArgs: Any?,
        keyExtractor: (StorageKeyComponents) -> K,
        binding: DynamicInstanceBinderWithKey<K, V>
    ): Map<K, V>

    suspend fun StorageEntry.entriesRaw(
        vararg prefixArgs: Any?,
    ): Map<String, String?>

    suspend fun StorageEntry.entriesRaw(
        keysArguments: List<List<Any?>>
    ): Map<String, String?>

    suspend fun <K, V> StorageEntry.entries(
        keysArguments: List<List<Any?>>,
        keyExtractor: (StorageKeyComponents) -> K,
        binding: DynamicInstanceBinderWithKey<K, V>
    ): Map<K, V>

    suspend fun <V> StorageEntry.query(
        vararg keyArguments: Any?,
        binding: DynamicInstanceBinder<V>
    ): V

    suspend fun StorageEntry.queryRaw(
        vararg keyArguments: Any?
    ): String?

    suspend fun multi(
        builderBlock: MultiQueryBuilder.() -> Unit
    ): Map<StorageEntry, Map<StorageKeyComponents, Any?>>

    // no keyExtractor short-cut
    suspend fun <V> StorageEntry.entries(
        vararg prefixArgs: Any?,
        binding: (Any?, StorageKeyComponents) -> V
    ): Map<StorageKeyComponents, V> = entries(
        *prefixArgs,
        keyExtractor = { it },
        binding = binding
    )

    suspend fun <K, V> StorageEntry.singleArgumentEntries(
        keysArguments: Collection<K>,
        binding: DynamicInstanceBinderWithKey<K, V>
    ): Map<K, V> = entries(
        keysArguments = keysArguments.wrapSingleArgumentKeys(),
        keyExtractor = { it.component1<Any?>() as K },
        binding = binding
    )
}

fun Map<StorageEntry, Map<StorageKeyComponents, Any?>>.singleValueOf(storageEntry: StorageEntry) = getValue(storageEntry).values.first()

fun Collection<*>.wrapSingleArgumentKeys(): List<List<Any?>> = map(::listOf)
