package io.novafoundation.nova.runtime.storage.source.query

import io.novafoundation.nova.common.data.network.runtime.binding.BlockHash
import io.novafoundation.nova.common.data.network.runtime.binding.fromHexOrIncompatible
import io.novafoundation.nova.common.data.network.runtime.binding.incompatible
import io.novafoundation.nova.common.utils.ComponentHolder
import io.novafoundation.nova.common.utils.splitKeyToComponents
import io.novafoundation.nova.runtime.storage.source.multi.MultiQueryBuilder
import io.novafoundation.nova.runtime.storage.source.multi.MultiQueryBuilderImpl
import jp.co.soramitsu.fearless_utils.runtime.RuntimeSnapshot
import jp.co.soramitsu.fearless_utils.runtime.definitions.types.fromHex
import jp.co.soramitsu.fearless_utils.runtime.metadata.module.StorageEntry
import jp.co.soramitsu.fearless_utils.runtime.metadata.splitKey
import jp.co.soramitsu.fearless_utils.runtime.metadata.storageKey
import jp.co.soramitsu.fearless_utils.runtime.metadata.storageKeys
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

abstract class BaseStorageQueryContext(
    override val runtime: RuntimeSnapshot,
    private val at: BlockHash?,
) : StorageQueryContext {

    protected abstract suspend fun queryKeysByPrefix(prefix: String): List<String>

    protected abstract suspend fun queryEntriesByPrefix(prefix: String): Map<String, String?>

    protected abstract suspend fun queryKeys(keys: List<String>, at: BlockHash?): Map<String, String?>

    protected abstract suspend fun queryKey(key: String, at: BlockHash?): String?

    protected abstract suspend fun observeKey(key: String): Flow<String?>

    override suspend fun StorageEntry.keys(vararg prefixArgs: Any?): List<StorageKeyComponents> {
        val prefix = storageKey(runtime, *prefixArgs)

        return queryKeysByPrefix(prefix).map { ComponentHolder(splitKey(runtime, it)) }
    }

    override suspend fun <K, V> StorageEntry.entries(
        vararg prefixArgs: Any?,
        keyExtractor: (StorageKeyComponents) -> K,
        binding: (Any?, K) -> V
    ): Map<K, V> {
        val prefix = storageKey(runtime, *prefixArgs)
        val returnType = type.value ?: incompatible()

        val entries = queryEntriesByPrefix(prefix)

        return applyMappersToEntries(
            entries = entries,
            storageEntry = this,
            keyExtractor = keyExtractor,
            binding = { scale, key ->
                val decoded = scale?.let { returnType.fromHexOrIncompatible(scale, runtime) }

                binding(decoded, key)
            }
        )
    }

    override suspend fun <K, V> StorageEntry.entries(
        keysArguments: List<List<Any?>>,
        keyExtractor: (StorageKeyComponents) -> K,
        binding: (String?, K) -> V
    ): Map<K, V> {
        val entries = queryKeys(storageKeys(runtime, keysArguments), at)

        return applyMappersToEntries(entries, storageEntry = this, keyExtractor, binding)
    }

    override suspend fun <V> StorageEntry.query(
        vararg keyArguments: Any?,
        binding: (instance: Any?) -> V
    ): V {
        val storageKey = storageKeyWith(keyArguments)
        val scaleResult = queryKey(storageKey, at)
        val decoded = scaleResult?.let { type.value?.fromHex(runtime, scaleResult) }

        return binding(decoded)
    }

    override suspend fun <V> StorageEntry.observe(
        vararg keyArguments: Any?,
        binding: (dynamicInstance: Any?) -> V
    ): Flow<V> {
        val storageKey = storageKeyWith(keyArguments)

        return observeKey(storageKey).map { scale ->
            val dynamicInstance = scale?.let {
                type.value?.fromHex(runtime, scale)
            }

            binding(dynamicInstance)
        }
    }

    override suspend fun multi(
        builderBlock: MultiQueryBuilder.() -> Unit
    ): Map<StorageEntry, Map<StorageKeyComponents, Any?>> {
        val keysByStorageEntry = MultiQueryBuilderImpl(runtime).apply(builderBlock).build()

        val keys = keysByStorageEntry.flatMap { (_, keys) -> keys }
        val values = queryKeys(keys, at)

        return keysByStorageEntry.mapValues { (storageEntry, keys) ->
            val valueType = storageEntry.type.value!!

            keys.associateBy(
                keySelector = { key -> storageEntry.splitKeyToComponents(runtime, key) },
                valueTransform = { key -> values[key]?.let { valueType.fromHex(runtime, it) } }
            )
        }
    }

    private fun StorageEntry.storageKeyWith(keyArguments: Array<out Any?>): String {
        return if (keyArguments.isEmpty()) {
            storageKey()
        } else {
            storageKey(runtime, *keyArguments)
        }
    }

    private fun <K, V> applyMappersToEntries(
        entries: Map<String, String?>,
        storageEntry: StorageEntry,
        keyExtractor: (StorageKeyComponents) -> K,
        binding: (String?, K) -> V,
    ): Map<K, V> {
        return entries.mapKeys { (key, _) ->
            val keyComponents = ComponentHolder(storageEntry.splitKey(runtime, key))

            keyExtractor(keyComponents)
        }.mapValues { (key, value) -> binding(value, key) }
    }
}
