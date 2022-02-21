package io.novafoundation.nova.runtime.storage.source

import io.novafoundation.nova.common.data.network.runtime.binding.Binder
import io.novafoundation.nova.common.data.network.runtime.binding.BlockHash
import io.novafoundation.nova.common.data.network.runtime.binding.NonNullBinder
import io.novafoundation.nova.common.utils.ComponentHolder
import jp.co.soramitsu.fearless_utils.runtime.RuntimeSnapshot
import jp.co.soramitsu.fearless_utils.runtime.metadata.module.StorageEntry
import kotlinx.coroutines.flow.Flow
import java.io.OutputStream

typealias StorageKey = String
typealias ChildKeyBuilder = suspend OutputStream.(RuntimeSnapshot) -> Unit

typealias StorageKeyComponents = ComponentHolder

interface MultiQueryBuilder {

    fun StorageEntry.queryKey(vararg args: Any?) // singe key

    fun StorageEntry.queryKeys(keysArgs: List<List<Any?>>) // multiple keys

    fun StorageEntry.querySingleArgKeys(singleArgKeys: List<Any?>) = queryKeys(singleArgKeys.map(::listOf))
}

interface StorageQueryContext {

    val runtime: RuntimeSnapshot

    suspend fun StorageEntry.keys(vararg prefixArgs: Any?): List<StorageKeyComponents>

    suspend fun <K, V> StorageEntry.entries(
        vararg prefixArgs: Any?,
        keyExtractor: (StorageKeyComponents) -> K,
        binding: (String?, K) -> V
    ): Map<K, V>

    suspend fun <K, V> StorageEntry.entries(
        keysArguments: List<List<Any?>>,
        keyExtractor: (StorageKeyComponents) -> K,
        at: BlockHash? = null,
        binding: (String?, K) -> V
    ): Map<K, V>

    suspend fun multi(
        at: BlockHash? = null,
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
}

interface StorageDataSource {

    suspend fun <T> query(
        chainId: String,
        keyBuilder: (RuntimeSnapshot) -> StorageKey,
        at: BlockHash? = null,
        binding: Binder<T>,
    ): T

    @Deprecated("Use query() statement instead")
    suspend fun <K, T> queryKeys(
        chainId: String,
        keysBuilder: (RuntimeSnapshot) -> Map<StorageKey, K>,
        at: BlockHash? = null,
        binding: Binder<T>,
    ): Map<K, T>

    fun <T> observe(
        chainId: String,
        keyBuilder: (RuntimeSnapshot) -> StorageKey,
        binder: Binder<T>,
    ): Flow<T>

    suspend fun <T> queryChildState(
        chainId: String,
        storageKeyBuilder: (RuntimeSnapshot) -> StorageKey,
        childKeyBuilder: ChildKeyBuilder,
        binder: Binder<T>
    ): T

    suspend fun <R> query(
        chainId: String,
        query: suspend StorageQueryContext.() -> R
    ): R
}

suspend inline fun <T> StorageDataSource.queryNonNull(
    chainId: String,
    noinline keyBuilder: (RuntimeSnapshot) -> String,
    crossinline binding: NonNullBinder<T>,
    at: BlockHash? = null
) = query(chainId, keyBuilder, at) { scale, runtime -> binding(scale!!, runtime) }

inline fun <T> StorageDataSource.observeNonNull(
    chainId: String,
    noinline keyBuilder: (RuntimeSnapshot) -> String,
    crossinline binding: NonNullBinder<T>,
) = observe(chainId, keyBuilder) { scale, runtime -> binding(scale!!, runtime) }
