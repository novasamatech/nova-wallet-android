package io.novafoundation.nova.runtime.multiNetwork.runtime

import io.novafoundation.nova.runtime.ext.typesUsage
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.runtime.types.BaseTypeSynchronizer
import java.util.concurrent.ConcurrentHashMap

class RuntimeProviderPool(
    private val runtimeFactory: RuntimeFactory,
    private val runtimeSyncService: RuntimeSyncService,
    private val runtimeFilesCache: RuntimeFilesCache,
    private val baseTypeSynchronizer: BaseTypeSynchronizer
) {

    private val pool = ConcurrentHashMap<String, RuntimeProvider>()

    fun getRuntimeProvider(chainId: String) = pool.getValue(chainId)

    fun setupRuntimeProvider(chain: Chain): RuntimeProvider {
        val provider = pool.getOrPut(chain.id) {
            RuntimeProvider(runtimeFactory, runtimeSyncService, baseTypeSynchronizer, runtimeFilesCache, chain)
        }

        provider.considerUpdatingTypesUsage(chain.typesUsage)

        return provider
    }

    fun removeRuntimeProvider(chainId: String) {
        pool.remove(chainId)?.apply { finish() }
    }
}
