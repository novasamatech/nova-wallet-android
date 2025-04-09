package io.novafoundation.nova.runtime.storage.source.query

import io.novafoundation.nova.common.data.network.runtime.binding.BlockHash
import io.novafoundation.nova.common.data.network.runtime.binding.bindNumberOrZero
import io.novafoundation.nova.common.data.network.runtime.binding.fromHexOrIncompatible
import io.novafoundation.nova.common.data.network.runtime.binding.incompatible
import io.novafoundation.nova.common.utils.ComponentHolder
import io.novafoundation.nova.common.utils.createStorageKey
import io.novafoundation.nova.common.utils.mapValuesNotNull
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novafoundation.nova.runtime.storage.source.StorageEntries
import io.novafoundation.nova.runtime.storage.source.multi.MultiQueryBuilder
import io.novafoundation.nova.runtime.storage.source.multi.MultiQueryBuilderImpl
import io.novasama.substrate_sdk_android.runtime.RuntimeSnapshot
import io.novasama.substrate_sdk_android.runtime.definitions.types.fromByteArray
import io.novasama.substrate_sdk_android.runtime.definitions.types.fromHex
import io.novasama.substrate_sdk_android.runtime.definitions.types.primitives.u16
import io.novasama.substrate_sdk_android.runtime.definitions.types.toByteArray
import io.novasama.substrate_sdk_android.runtime.metadata.StorageEntryModifier
import io.novasama.substrate_sdk_android.runtime.metadata.module.Module
import io.novasama.substrate_sdk_android.runtime.metadata.module.StorageEntry
import io.novasama.substrate_sdk_android.runtime.metadata.module.StorageEntryType
import io.novasama.substrate_sdk_android.runtime.metadata.splitKey
import io.novasama.substrate_sdk_android.runtime.metadata.storageKey
import io.novasama.substrate_sdk_android.runtime.metadata.storageKeys
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.math.BigInteger
import io.novafoundation.nova.core.model.StorageEntry as StorageEntryValue

abstract class BaseStorageQueryContext(
    override val chainId: ChainId,
    override val runtime: RuntimeSnapshot,
    private val at: BlockHash?,
    private val applyStorageDefault: Boolean
) : StorageQueryContext {

    protected abstract suspend fun queryKeysByPrefix(prefix: String, at: BlockHash?): List<String>

    protected abstract suspend fun queryEntriesByPrefix(prefix: String, at: BlockHash?): Map<String, String?>

    protected abstract suspend fun queryKeys(keys: List<String>, at: BlockHash?): Map<String, String?>

    protected abstract suspend fun queryKey(key: String, at: BlockHash?): String?

    protected abstract fun observeKey(key: String): Flow<StorageUpdate>

    protected abstract fun observeKeys(keys: List<String>): Flow<Map<String, String?>>

    protected abstract suspend fun observeKeysByPrefix(prefix: String): Flow<Map<String, String?>>

    override suspend fun StorageEntry.keys(vararg prefixArgs: Any?): List<StorageKeyComponents> {
        val prefix = storageKey(runtime, *prefixArgs)

        return queryKeysByPrefix(prefix, at).map { ComponentHolder(splitKey(runtime, it)) }
    }

    override suspend fun <K, V> StorageEntry.entries(
        vararg prefixArgs: Any?,
        keyExtractor: (StorageKeyComponents) -> K,
        binding: DynamicInstanceBinderWithKey<K, V>,
        recover: (exception: Exception, rawValue: String?) -> Unit
    ): Map<K, V> {
        val prefix = storageKey(runtime, *prefixArgs)

        val entries = queryEntriesByPrefix(prefix, at)

        return applyMappersToEntries(
            entries = entries,
            storageEntry = this,
            keyExtractor = keyExtractor,
            binding = binding,
            recover = recover
        )
    }

    override suspend fun <K, V> StorageEntry.entries(
        keysArguments: List<List<Any?>>,
        keyExtractor: (StorageKeyComponents) -> K,
        binding: DynamicInstanceBinderWithKey<K, V>,
        recover: (exception: Exception, rawValue: String?) -> Unit
    ): Map<K, V> {
        val entries = queryKeys(storageKeys(runtime, keysArguments), at)

        return applyMappersToEntries(
            entries = entries,
            storageEntry = this,
            keyExtractor = keyExtractor,
            binding = binding,
            recover = recover
        )
    }

    override suspend fun <K, V> StorageEntry.observeByPrefix(
        vararg prefixArgs: Any?,
        keyExtractor: (StorageKeyComponents) -> K,
        binding: DynamicInstanceBinderWithKey<K, V>
    ): Flow<Map<K, V>> {
        val prefixKey = storageKey(runtime, *prefixArgs)

        return observeKeysByPrefix(prefixKey).map { valuesByKey ->
            applyMappersToEntries(
                entries = valuesByKey,
                storageEntry = this,
                keyExtractor = keyExtractor,
                binding = binding
            )
        }
    }

    override suspend fun StorageEntry.entriesRaw(vararg prefixArgs: Any?): StorageEntries {
        return queryEntriesByPrefix(storageKey(runtime, *prefixArgs), at)
    }

    override suspend fun StorageEntry.entriesRaw(keysArguments: List<List<Any?>>): StorageEntries {
        return queryKeys(storageKeys(runtime, keysArguments), at)
    }

    override suspend fun Module.palletVersionOrThrow(): Int {
        val manualStorageVersionEntry = StorageEntry(
            moduleName = name,
            name = ":__STORAGE_VERSION__:",
            modifier = StorageEntryModifier.Required,
            type = StorageEntryType.Plain(value = u16),
            default = u16.toByteArray(runtime, BigInteger.ZERO),
            documentation = emptyList()
        )

        return manualStorageVersionEntry.query(binding = ::bindNumberOrZero).toInt()
    }

    override suspend fun <V> StorageEntry.query(
        vararg keyArguments: Any?,
        binding: DynamicInstanceBinder<V>
    ): V {
        val storageKey = createStorageKey(keyArguments)
        val scaleResult = queryKey(storageKey, at)
        return decodeStorageValue(scaleResult, binding)
    }

    override suspend fun StorageEntry.queryRaw(vararg keyArguments: Any?): String? {
        val storageKey = createStorageKey(keyArguments)

        return queryKey(storageKey, at)
    }

    override fun <V> StorageEntry.observe(
        vararg keyArguments: Any?,
        binding: DynamicInstanceBinder<V>
    ): Flow<V> {
        val storageKey = createStorageKey(keyArguments)

        return observeKey(storageKey).map { storageUpdate ->
            decodeStorageValue(storageUpdate.value, binding)
        }
    }

    override fun <V> StorageEntry.observeWithRaw(
        vararg keyArguments: Any?,
        binding: DynamicInstanceBinder<V>
    ): Flow<WithRawValue<V>> {
        val storageKey = createStorageKey(keyArguments)

        return observeKey(storageKey).map { storageUpdate ->
            val decoded = decodeStorageValue(storageUpdate.value, binding)

            WithRawValue(
                raw = StorageEntryValue(storageKey, storageUpdate.value),
                chainId = chainId,
                value = decoded,
                at = storageUpdate.at,
            )
        }
    }

    override fun <K, V> StorageEntry.observe(
        keysArguments: List<List<Any?>>,
        keyExtractor: (StorageKeyComponents) -> K,
        binding: DynamicInstanceBinderWithKey<K, V>
    ): Flow<Map<K, V>> {
        val storageKeys = storageKeys(runtime, keysArguments)

        return observeKeys(storageKeys).map { valuesByKey ->
            applyMappersToEntries(
                entries = valuesByKey,
                storageEntry = this,
                keyExtractor = keyExtractor,
                binding = binding
            )
        }
    }

    @Suppress("OVERRIDE_DEPRECATION", "OverridingDeprecatedMember")
    override suspend fun multiInternal(
        builderBlock: MultiQueryBuilder.() -> Unit
    ): MultiQueryBuilder.Result {
        val builder = MultiQueryBuilderImpl(runtime).apply(builderBlock)

        val keys = builder.keys().flatMap { (_, keys) -> keys }
        val values = queryKeys(keys, at)

        val delegate = builder.descriptors().mapValues { (descriptor, keys) ->
            keys.associateBy(
                keySelector = { key -> descriptor.parseKey(key) },
                valueTransform = { key -> descriptor.parseValue(values[key]) }
            )
        }

        return MultiQueryResult(delegate)
    }

    private fun <V> StorageEntry.decodeStorageValue(
        scale: String?,
        binding: DynamicInstanceBinder<V>
    ): V {
        val dynamicInstance = scale?.let {
            type.value?.fromHex(runtime, scale)
        } ?: takeDefaultIfAllowed()

        return binding(dynamicInstance)
    }

    private fun <K, V> applyMappersToEntries(
        entries: Map<String, String?>,
        storageEntry: StorageEntry,
        keyExtractor: (StorageKeyComponents) -> K,
        binding: DynamicInstanceBinderWithKey<K, V>,
        recover: (exception: Exception, rawValue: String?) -> Unit = { exception, _ -> throw exception }
    ): Map<K, V> {
        val returnType = storageEntry.type.value ?: incompatible()

        return entries.mapKeys { (key, _) ->
            val keyComponents = ComponentHolder(storageEntry.splitKey(runtime, key))

            keyExtractor(keyComponents)
        }.mapValuesNotNull { (key, value) ->
            try {
                val decoded = value?.let { returnType.fromHexOrIncompatible(value, runtime) }
                binding(decoded, key)
            } catch (e: Exception) {
                recover(e, value)
                null
            }
        }
    }

    protected class StorageUpdate(
        val value: String?,
        // Might be null in case the source does not support identifying the block at which value was changed
        val at: BlockHash?
    )

    private fun StorageEntry.takeDefaultIfAllowed(): Any? {
        if (!applyStorageDefault) return null

        return type.value?.fromByteArray(runtime, default)
    }

    @JvmInline
    private value class MultiQueryResult(val delegate: Map<MultiQueryBuilder.Descriptor<*, *>, Map<Any?, Any?>>) : MultiQueryBuilder.Result {

        @Suppress("UNCHECKED_CAST")
        override fun <K, V> get(descriptor: MultiQueryBuilder.Descriptor<K, V>): Map<K, V> {
            return delegate.getValue(descriptor) as Map<K, V>
        }
    }
}
