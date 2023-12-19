package io.novafoundation.nova.runtime.storage.source.query

import io.novafoundation.nova.common.data.network.runtime.binding.BlockHash
import io.novafoundation.nova.common.data.network.runtime.binding.bindNumberOrZero
import io.novafoundation.nova.common.data.network.runtime.binding.fromByteArrayOrIncompatible
import io.novafoundation.nova.common.data.network.runtime.binding.fromHexOrIncompatible
import io.novafoundation.nova.common.data.network.runtime.binding.incompatible
import io.novafoundation.nova.common.utils.ComponentHolder
import io.novafoundation.nova.common.utils.mapValuesNotNull
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novafoundation.nova.runtime.storage.source.multi.MultiQueryBuilder
import io.novafoundation.nova.runtime.storage.source.multi.MultiQueryBuilderImpl
import jp.co.soramitsu.fearless_utils.runtime.RuntimeSnapshot
import jp.co.soramitsu.fearless_utils.runtime.definitions.types.fromHex
import jp.co.soramitsu.fearless_utils.runtime.definitions.types.primitives.u16
import jp.co.soramitsu.fearless_utils.runtime.definitions.types.toByteArray
import jp.co.soramitsu.fearless_utils.runtime.metadata.StorageEntryModifier
import jp.co.soramitsu.fearless_utils.runtime.metadata.module.Constant
import jp.co.soramitsu.fearless_utils.runtime.metadata.module.Module
import jp.co.soramitsu.fearless_utils.runtime.metadata.module.StorageEntry
import jp.co.soramitsu.fearless_utils.runtime.metadata.module.StorageEntryType
import jp.co.soramitsu.fearless_utils.runtime.metadata.splitKey
import jp.co.soramitsu.fearless_utils.runtime.metadata.storageKey
import jp.co.soramitsu.fearless_utils.runtime.metadata.storageKeys
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.math.BigInteger
import io.novafoundation.nova.core.model.StorageEntry as StorageEntryValue

abstract class BaseStorageQueryContext(
    override val chainId: ChainId,
    override val runtime: RuntimeSnapshot,
    private val at: BlockHash?,
) : StorageQueryContext {

    protected abstract suspend fun queryKeysByPrefix(prefix: String, at: BlockHash?): List<String>

    protected abstract suspend fun queryEntriesByPrefix(prefix: String, at: BlockHash?): Map<String, String?>

    protected abstract suspend fun queryKeys(keys: List<String>, at: BlockHash?): Map<String, String?>

    protected abstract suspend fun queryKey(key: String, at: BlockHash?): String?

    protected abstract fun observeKey(key: String): Flow<String?>

    protected abstract suspend fun observeKeys(keys: List<String>): Flow<Map<String, String?>>

    protected abstract suspend fun observeKeysByPrefix(prefix: String): Flow<Map<String, String?>>

    override fun StorageEntry.createStorageKey(vararg keyArguments: Any?): String {
        return storageKeyWith(keyArguments)
    }

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

    override suspend fun StorageEntry.entriesRaw(vararg prefixArgs: Any?): Map<String, String?> {
        return queryEntriesByPrefix(storageKey(runtime, *prefixArgs), at)
    }

    override suspend fun StorageEntry.entriesRaw(keysArguments: List<List<Any?>>): Map<String, String?> {
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
        val storageKey = storageKeyWith(keyArguments)
        val scaleResult = queryKey(storageKey, at)
        val decoded = scaleResult?.let { type.value?.fromHex(runtime, scaleResult) }

        return binding(decoded)
    }

    override suspend fun StorageEntry.queryRaw(vararg keyArguments: Any?): String? {
        val storageKey = storageKeyWith(keyArguments)

        return queryKey(storageKey, at)
    }

    override fun <V> StorageEntry.observe(
        vararg keyArguments: Any?,
        binding: DynamicInstanceBinder<V>
    ): Flow<V> {
        val storageKey = storageKeyWith(keyArguments)

        return observeKey(storageKey).map { scale ->
            decodeStorageValue(scale, binding)
        }
    }

    override fun <V> StorageEntry.observeWithRaw(
        vararg keyArguments: Any?,
        binding: DynamicInstanceBinder<V>
    ): Flow<WithRawValue<V>> {
        val storageKey = storageKeyWith(keyArguments)

        return observeKey(storageKey).map { scale ->
            val decoded = decodeStorageValue(scale, binding)

            WithRawValue(
                raw = StorageEntryValue(storageKey, scale),
                chainId = chainId,
                value = decoded
            )
        }
    }

    override suspend fun <K, V> StorageEntry.observe(
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

    override suspend fun <V> Constant.getAs(binding: DynamicInstanceBinder<V>): V {
        val rawValue = type!!.fromByteArrayOrIncompatible(value, runtime)

        return binding(rawValue)
    }

    private fun <V> StorageEntry.decodeStorageValue(
        scale: String?,
        binding: DynamicInstanceBinder<V>
    ): V {
        val dynamicInstance = scale?.let {
            type.value?.fromHex(runtime, scale)
        }

        return binding(dynamicInstance)
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

    @JvmInline
    private value class MultiQueryResult(val delegate: Map<MultiQueryBuilder.Descriptor<*, *>, Map<Any?, Any?>>) : MultiQueryBuilder.Result {
        @Suppress("UNCHECKED_CAST")
        override fun <K, V> get(descriptor: MultiQueryBuilder.Descriptor<K, V>): Map<K, V> {
            return delegate.getValue(descriptor) as Map<K, V>
        }
    }
}
