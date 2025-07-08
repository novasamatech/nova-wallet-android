package io.novafoundation.nova.runtime.call

import io.novafoundation.nova.common.utils.hasDetectedRuntimeApi
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novafoundation.nova.runtime.multiNetwork.getRuntime
import io.novafoundation.nova.runtime.multiNetwork.getSocket

interface MultiChainRuntimeCallsApi {

    suspend fun forChain(chainId: ChainId): RuntimeCallsApi

    suspend fun isSupported(chainId: ChainId, section: String, method: String): Boolean
}

internal class RealMultiChainRuntimeCallsApi(
    private val chainRegistry: ChainRegistry
) : MultiChainRuntimeCallsApi {

    override suspend fun forChain(chainId: ChainId): RuntimeCallsApi {
        val runtime = chainRegistry.getRuntime(chainId)
        val socket = chainRegistry.getSocket(chainId)

        return RealRuntimeCallsApi(runtime, chainId, socket)
    }

    override suspend fun isSupported(chainId: ChainId, section: String, method: String): Boolean {
        val runtime = chainRegistry.getRuntime(chainId)
        // Avoid extra allocations of RealRuntimeCallsApi and socket retrieval - check directly from the metadata
        return runtime.metadata.hasDetectedRuntimeApi(section, method)
    }
}
