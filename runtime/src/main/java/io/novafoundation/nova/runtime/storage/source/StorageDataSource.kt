package io.novafoundation.nova.runtime.storage.source

import io.novafoundation.nova.common.data.network.runtime.binding.Binder
import io.novafoundation.nova.common.data.network.runtime.binding.BlockHash
import io.novafoundation.nova.common.data.network.runtime.binding.NonNullBinder
import io.novafoundation.nova.runtime.storage.source.query.StorageQueryContext
import jp.co.soramitsu.fearless_utils.runtime.RuntimeSnapshot
import kotlinx.coroutines.flow.Flow
import java.io.OutputStream

typealias StorageKey = String
typealias ChildKeyBuilder = suspend OutputStream.(RuntimeSnapshot) -> Unit

interface StorageDataSource {

    suspend fun <T> query(
        chainId: String,
        keyBuilder: (RuntimeSnapshot) -> StorageKey,
        at: BlockHash? = null,
        binding: Binder<T>,
    ): T

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
        at: BlockHash? = null,
        query: suspend StorageQueryContext.() -> R
    ): R

    fun <R> subscribe(
        chainId: String,
        at: BlockHash? = null,
        subscribe: suspend StorageQueryContext.() -> Flow<R>
    ): Flow<R>
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
