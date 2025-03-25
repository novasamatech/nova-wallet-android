package io.novafoundation.nova.runtime.storage.source.query

import io.novafoundation.nova.common.utils.ComponentHolder
import io.novafoundation.nova.common.utils.RuntimeContext
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novafoundation.nova.runtime.storage.source.StorageEntries
import io.novafoundation.nova.runtime.storage.source.StorageKey
import io.novafoundation.nova.runtime.storage.source.StorageValue
import io.novafoundation.nova.runtime.storage.source.multi.MultiQueryBuilder
import io.novasama.substrate_sdk_android.runtime.RuntimeSnapshot
import io.novasama.substrate_sdk_android.runtime.metadata.RuntimeMetadata
import io.novasama.substrate_sdk_android.runtime.metadata.module.Constant
import io.novasama.substrate_sdk_android.runtime.metadata.module.Module
import io.novasama.substrate_sdk_android.runtime.metadata.module.StorageEntry
import kotlinx.coroutines.flow.Flow
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

typealias StorageKeyComponents = ComponentHolder
typealias DynamicInstanceBinder<V> = (dynamicInstance: Any?) -> V
typealias DynamicInstanceBinderWithKey<K, V> = (dynamicInstance: Any?, key: K) -> V

interface StorageQueryContext : RuntimeContext {

    val chainId: ChainId

    override val runtime: RuntimeSnapshot

    suspend fun StorageEntry.keys(vararg prefixArgs: Any?): List<StorageKeyComponents>

    fun <V> StorageEntry.observe(
        vararg keyArguments: Any?,
        binding: DynamicInstanceBinder<V>
    ): Flow<V>

    fun <V> StorageEntry.observeWithRaw(
        vararg keyArguments: Any?,
        binding: DynamicInstanceBinder<V>
    ): Flow<WithRawValue<V>>

    fun <K, V> StorageEntry.observe(
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
        binding: DynamicInstanceBinderWithKey<K, V>,
        recover: (exception: Exception, rawValue: String?) -> Unit = { exception, _ -> throw exception }
    ): Map<K, V>

    suspend fun StorageEntry.entriesRaw(
        vararg prefixArgs: Any?,
    ): Map<StorageKey, StorageValue>

    suspend fun StorageEntry.entriesRaw(
        keysArguments: List<List<Any?>>
    ): StorageEntries

    suspend fun <K, V> StorageEntry.entries(
        keysArguments: List<List<Any?>>,
        keyExtractor: (StorageKeyComponents) -> K,
        binding: DynamicInstanceBinderWithKey<K, V>,
        recover: (exception: Exception, rawValue: String?) -> Unit = { exception, _ -> throw exception }
    ): Map<K, V>

    suspend fun <V> StorageEntry.query(
        vararg keyArguments: Any?,
        binding: DynamicInstanceBinder<V>
    ): V

    suspend fun StorageEntry.queryRaw(
        vararg keyArguments: Any?
    ): StorageValue

    @Deprecated("Use multi for better smart-casting", replaceWith = ReplaceWith(expression = "multi(builderBlock)"))
    suspend fun multiInternal(
        builderBlock: MultiQueryBuilder.() -> Unit
    ): MultiQueryBuilder.Result

    // no keyExtractor short-cut
    suspend fun <V> StorageEntry.entries(
        vararg prefixArgs: Any?,
        binding: (Any?, StorageKeyComponents) -> V
    ): Map<StorageKeyComponents, V> = entries(
        *prefixArgs,
        keyExtractor = { it },
        binding = binding
    )

    suspend fun Module.palletVersionOrThrow(): Int

    suspend fun <K, V> StorageEntry.singleArgumentEntries(
        keysArguments: Collection<K>,
        binding: DynamicInstanceBinderWithKey<K, V>
    ): Map<K, V> = entries(
        keysArguments = keysArguments.wrapSingleArgumentKeys(),
        keyExtractor = { it.component1<Any?>() as K },
        binding = binding
    )

    suspend fun <V> Constant.getAs(binding: DynamicInstanceBinder<V>): V
}

@Suppress("DEPRECATION")
@OptIn(ExperimentalContracts::class)
suspend fun StorageQueryContext.multi(
    builderBlock: MultiQueryBuilder.() -> Unit
): MultiQueryBuilder.Result {
    contract {
        callsInPlace(builderBlock, InvocationKind.EXACTLY_ONCE)
    }

    return multiInternal(builderBlock)
}

fun Iterable<*>.wrapSingleArgumentKeys(): List<List<Any?>> = map(::listOf)
