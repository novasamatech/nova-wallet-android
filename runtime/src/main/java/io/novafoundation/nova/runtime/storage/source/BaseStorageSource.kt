package io.novafoundation.nova.runtime.storage.source

import io.novafoundation.nova.common.data.network.rpc.childStateKey
import io.novafoundation.nova.common.data.network.runtime.binding.Binder
import io.novafoundation.nova.common.data.network.runtime.binding.BlockHash
import io.novafoundation.nova.common.utils.ComponentHolder
import io.novafoundation.nova.common.utils.splitKeyToComponents
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.getRuntime
import jp.co.soramitsu.fearless_utils.runtime.RuntimeSnapshot
import jp.co.soramitsu.fearless_utils.runtime.definitions.types.fromHex
import jp.co.soramitsu.fearless_utils.runtime.metadata.module.StorageEntry
import jp.co.soramitsu.fearless_utils.runtime.metadata.splitKey
import jp.co.soramitsu.fearless_utils.runtime.metadata.storageKey
import jp.co.soramitsu.fearless_utils.runtime.metadata.storageKeys
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class MultiQueryBuilderImpl(
    private val runtime: RuntimeSnapshot
) : MultiQueryBuilder {

    private val keys: MutableMap<StorageEntry, MutableList<String>> = mutableMapOf()

    override fun StorageEntry.queryKey(vararg args: Any?) {
        keysForEntry(this).add(storageKey(runtime, *args))
    }

    override fun StorageEntry.queryKeys(keysArgs: List<List<Any?>>) {
        keysForEntry(this).addAll(storageKeys(runtime, keysArgs))
    }

    fun build(): Map<StorageEntry, List<String>> {
        return keys
    }

    private fun keysForEntry(entry: StorageEntry) = keys.getOrPut(entry, ::mutableListOf)
}

abstract class BaseStorageQueryContext(
    override val runtime: RuntimeSnapshot,
) : StorageQueryContext {

    protected abstract suspend fun queryKeysByPrefix(prefix: String): List<String>

    protected abstract suspend fun queryEntriesByPrefix(prefix: String): Map<String, String?>

    protected abstract suspend fun queryKeys(keys: List<String>, at: BlockHash?): Map<String, String?>

    override suspend fun StorageEntry.keys(vararg prefixArgs: Any?): List<StorageKeyComponents> {
        val prefix = storageKey(runtime, *prefixArgs)

        return queryKeysByPrefix(prefix).map { ComponentHolder(splitKey(runtime, it)) }
    }

    override suspend fun <K, V> StorageEntry.entries(
        vararg prefixArgs: Any?,
        keyExtractor: (StorageKeyComponents) -> K,
        binding: (String?, K) -> V
    ): Map<K, V> {
        val prefix = storageKey(runtime, prefixArgs)

        val entries = queryEntriesByPrefix(prefix)

        return applyMappersToEntries(entries, storageEntry = this, keyExtractor, binding)
    }

    override suspend fun <K, V> StorageEntry.entries(
        keysArguments: List<List<Any?>>,
        keyExtractor: (StorageKeyComponents) -> K,
        at: BlockHash?,
        binding: (String?, K) -> V
    ): Map<K, V> {
        val entries = queryKeys(storageKeys(runtime, keysArguments), at)

        return applyMappersToEntries(entries, storageEntry = this, keyExtractor, binding)
    }

    override suspend fun multi(
        at: BlockHash?,
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

    private fun <K, V> applyMappersToEntries(
        entries: Map<String, String?>,
        storageEntry: StorageEntry,
        keyExtractor: (StorageKeyComponents) -> K,
        binding: (String?, K) -> V
    ): Map<K, V> {
        return entries.mapKeys { (key, _) ->
            val keyComponents = ComponentHolder(storageEntry.splitKey(runtime, key))

            keyExtractor(keyComponents)
        }.mapValues { (key, value) -> binding(value, key) }
    }
}

abstract class BaseStorageSource(
    protected val chainRegistry: ChainRegistry
) : StorageDataSource {

    protected abstract suspend fun query(key: String, chainId: String, at: BlockHash?): String?

    protected abstract suspend fun queryKeys(keys: List<String>, chainId: String, at: BlockHash?): Map<String, String?>

    protected abstract suspend fun observe(key: String, chainId: String): Flow<String?>

    protected abstract suspend fun queryChildState(storageKey: String, childKey: String, chainId: String): String?

    protected abstract suspend fun createQueryContext(chainId: String, runtime: RuntimeSnapshot): StorageQueryContext

    override suspend fun <K, T> queryKeys(
        chainId: String,
        keysBuilder: (RuntimeSnapshot) -> Map<StorageKey, K>,
        at: BlockHash?,
        binding: Binder<T>,
    ): Map<K, T> = withContext(Dispatchers.Default) {
        val runtime = chainRegistry.getRuntime(chainId)

        val storageKeyToMapId = keysBuilder(runtime)

        val queryResults = queryKeys(storageKeyToMapId.keys.toList(), chainId, at)

        queryResults.mapKeys { (fullKey, _) -> storageKeyToMapId[fullKey]!! }
            .mapValues { (_, hexRaw) -> binding(hexRaw, runtime) }
    }

    override suspend fun <T> query(
        chainId: String,
        keyBuilder: (RuntimeSnapshot) -> String,
        at: BlockHash?,
        binding: Binder<T>,
    ) = withContext(Dispatchers.Default) {
        val runtime = chainRegistry.getRuntime(chainId)

        val key = keyBuilder(runtime)
        val rawResult = query(key, chainId, at)

        binding(rawResult, runtime)
    }

    override fun <T> observe(
        chainId: String,
        keyBuilder: (RuntimeSnapshot) -> String,
        binder: Binder<T>,
    ) = flow {
        val runtime = chainRegistry.getRuntime(chainId)
        val key = keyBuilder(runtime)

        emitAll(
            observe(key, chainId).map { binder(it, runtime) }
        )
    }

    override suspend fun <T> queryChildState(
        chainId: String,
        storageKeyBuilder: (RuntimeSnapshot) -> StorageKey,
        childKeyBuilder: ChildKeyBuilder,
        binder: Binder<T>
    ) = withContext(Dispatchers.Default) {
        val runtime = chainRegistry.getRuntime(chainId)

        val storageKey = storageKeyBuilder(runtime)

        val childKey = childStateKey {
            childKeyBuilder(runtime)
        }

        val scaleResult = queryChildState(storageKey, childKey, chainId)

        binder(scaleResult, runtime)
    }

    override suspend fun <R> query(
        chainId: String,
        query: suspend StorageQueryContext.() -> R
    ): R {
        val runtime = chainRegistry.getRuntime(chainId)
        val context = createQueryContext(chainId, runtime)

        return context.query()
    }
}
