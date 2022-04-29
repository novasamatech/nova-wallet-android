package io.novafoundation.nova.runtime.storage.source.query

import io.novafoundation.nova.common.utils.ComponentHolder
import io.novafoundation.nova.runtime.storage.source.multi.MultiQueryBuilder
import jp.co.soramitsu.fearless_utils.runtime.RuntimeSnapshot
import jp.co.soramitsu.fearless_utils.runtime.metadata.module.StorageEntry
import kotlinx.coroutines.flow.Flow

typealias StorageKeyComponents = ComponentHolder

interface StorageQueryContext {

    val runtime: RuntimeSnapshot

    suspend fun StorageEntry.keys(vararg prefixArgs: Any?): List<StorageKeyComponents>

    suspend fun <V> StorageEntry.observe(
        vararg keyArguments: Any?,
        binding: (dynamicInstance: Any?) -> V
    ): Flow<V>

    suspend fun <K, V> StorageEntry.entries(
        vararg prefixArgs: Any?,
        keyExtractor: (StorageKeyComponents) -> K,
        binding: (String?, K) -> V
    ): Map<K, V>

    suspend fun <K, V> StorageEntry.entries(
        keysArguments: List<List<Any?>>,
        keyExtractor: (StorageKeyComponents) -> K,
        binding: (String?, K) -> V
    ): Map<K, V>

    suspend fun <V> StorageEntry.query(
        vararg keyArguments: Any?,
        binding: (scale: String?) -> V
    ): V

    suspend fun multi(
        builderBlock: MultiQueryBuilder.() -> Unit
    ): Map<StorageEntry, Map<StorageKeyComponents, Any?>>

    // no keyExtractor short-cut
    suspend fun <V> StorageEntry.entries(
        vararg prefixArgs: Any?,
        binding: (String?, StorageKeyComponents) -> V
    ): Map<StorageKeyComponents, V> = entries(
        *prefixArgs,
        keyExtractor = { it },
        binding = binding
    )

    suspend fun <K, V> StorageEntry.singleArgumentEntries(
        keysArguments: Collection<K>,
        binding: (String?, K) -> V
    ): Map<K, V> = entries(
        keysArguments = keysArguments.wrapSingleArgumentKeys(),
        keyExtractor = { it.component1<Any?>() as K },
        binding = binding
    )
}

fun Map<StorageEntry, Map<StorageKeyComponents, Any?>>.singleValueOf(storageEntry: StorageEntry) = getValue(storageEntry).values.first()

fun Collection<*>.wrapSingleArgumentKeys(): List<List<Any?>> = map(::listOf)
