package io.novafoundation.nova.feature_staking_impl.data.parachainStaking.turing.network.rpc

import io.novafoundation.nova.feature_staking_api.data.parachainStaking.turing.repository.OptimalAutomationRequest
import io.novafoundation.nova.feature_staking_api.data.parachainStaking.turing.repository.OptimalAutomationResponse
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novafoundation.nova.runtime.multiNetwork.getSocket
import jp.co.soramitsu.fearless_utils.wsrpc.executeAsync
import jp.co.soramitsu.fearless_utils.wsrpc.mappers.nonNull
import jp.co.soramitsu.fearless_utils.wsrpc.mappers.pojo
import jp.co.soramitsu.fearless_utils.wsrpc.request.runtime.RuntimeRequest

interface TuringAutomationRpcApi {

    suspend fun calculateOptimalAutomation(chainId: ChainId, request: OptimalAutomationRequest): OptimalAutomationResponse
}

class RealTuringAutomationRpcApi(
    private val chainRegistry: ChainRegistry,
): TuringAutomationRpcApi {

    override suspend fun calculateOptimalAutomation(chainId: ChainId, request: OptimalAutomationRequest): OptimalAutomationResponse {
        val socket = chainRegistry.getSocket(chainId)
        val rpcRequest = RuntimeRequest(
            method = "automationTime_calculateOptimalAutostaking",
            params = listOf(request.amount, request.collator),
        )

        return socket.executeAsync(rpcRequest, mapper = pojo<OptimalAutomationResponse>().nonNull())
    }
}
