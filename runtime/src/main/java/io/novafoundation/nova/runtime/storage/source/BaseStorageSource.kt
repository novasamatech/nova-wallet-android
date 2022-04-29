package io.novafoundation.nova.runtime.storage.source

import io.novafoundation.nova.common.data.network.rpc.childStateKey
import io.novafoundation.nova.common.data.network.runtime.binding.Binder
import io.novafoundation.nova.common.data.network.runtime.binding.BlockHash
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.getRuntime
import io.novafoundation.nova.runtime.storage.source.query.StorageQueryContext
import jp.co.soramitsu.fearless_utils.runtime.RuntimeSnapshot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

abstract class BaseStorageSource(
    protected val chainRegistry: ChainRegistry
) : StorageDataSource {

    protected abstract suspend fun query(key: String, chainId: String, at: BlockHash?): String?

    protected abstract suspend fun observe(key: String, chainId: String): Flow<String?>

    protected abstract suspend fun queryChildState(storageKey: String, childKey: String, chainId: String): String?

    protected abstract suspend fun createQueryContext(chainId: String, at: BlockHash?, runtime: RuntimeSnapshot): StorageQueryContext

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
        at: BlockHash?,
        query: suspend StorageQueryContext.() -> R
    ): R {
        val runtime = chainRegistry.getRuntime(chainId)
        val context = createQueryContext(chainId, at, runtime)

        return context.query()
    }

    override fun <R> subscribe(
        chainId: String,
        at: BlockHash?,
        subscribe: suspend StorageQueryContext.() -> Flow<R>
    ): Flow<R> {
        return flow {
            val runtime = chainRegistry.getRuntime(chainId)
            val context = createQueryContext(chainId, at, runtime)

            emitAll(context.subscribe())
        }
    }
}
