package io.novafoundation.nova.runtime.call

import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novafoundation.nova.runtime.multiNetwork.getRuntime
import io.novafoundation.nova.runtime.multiNetwork.getSocket

interface MultiChainRuntimeCallsApi {

    suspend fun forChain(chainId: ChainId): RuntimeCallsApi
}

internal class RealMultiChainRuntimeCallsApi(
    private val chainRegistry: ChainRegistry
): MultiChainRuntimeCallsApi {

    override suspend fun forChain(chainId: ChainId): RuntimeCallsApi {
        val runtime = chainRegistry.getRuntime(chainId)
        val socket = chainRegistry.getSocket(chainId)

        return RealRuntimeCallsApi(runtime, socket)
    }
}
