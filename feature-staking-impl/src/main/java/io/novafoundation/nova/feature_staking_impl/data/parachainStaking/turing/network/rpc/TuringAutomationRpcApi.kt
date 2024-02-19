package io.novafoundation.nova.feature_staking_impl.data.parachainStaking.turing.network.rpc

import com.google.gson.Gson
import io.novafoundation.nova.common.utils.asGsonParsedNumberOrNull
import io.novafoundation.nova.feature_staking_api.data.parachainStaking.turing.repository.AutomationAction
import io.novafoundation.nova.feature_staking_api.data.parachainStaking.turing.repository.OptimalAutomationRequest
import io.novafoundation.nova.feature_staking_api.data.parachainStaking.turing.repository.OptimalAutomationResponse
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novafoundation.nova.runtime.multiNetwork.getSocket
import io.novasama.substrate_sdk_android.wsrpc.executeAsync
import io.novasama.substrate_sdk_android.wsrpc.mappers.NullableMapper
import io.novasama.substrate_sdk_android.wsrpc.mappers.nonNull
import io.novasama.substrate_sdk_android.wsrpc.mappers.pojo
import io.novasama.substrate_sdk_android.wsrpc.request.runtime.RuntimeRequest
import io.novasama.substrate_sdk_android.wsrpc.response.RpcResponse

interface TuringAutomationRpcApi {

    suspend fun getTimeAutomationFees(chainId: ChainId, action: AutomationAction, executions: Int): Balance

    suspend fun calculateOptimalAutomation(chainId: ChainId, request: OptimalAutomationRequest): OptimalAutomationResponse
}

class RealTuringAutomationRpcApi(
    private val chainRegistry: ChainRegistry,
) : TuringAutomationRpcApi {

    override suspend fun getTimeAutomationFees(chainId: ChainId, action: AutomationAction, executions: Int): Balance {
        val socket = chainRegistry.getSocket(chainId)
        val rpcRequest = RuntimeRequest(
            method = "automationTime_getTimeAutomationFees",
            params = listOf(action.rpcParamName, executions),
        )

        return socket.executeAsync(rpcRequest, mapper = GsonNumberMapper().nonNull())
    }

    override suspend fun calculateOptimalAutomation(chainId: ChainId, request: OptimalAutomationRequest): OptimalAutomationResponse {
        val socket = chainRegistry.getSocket(chainId)
        val rpcRequest = RuntimeRequest(
            method = "automationTime_calculateOptimalAutostaking",
            params = listOf(request.amount, request.collator),
        )

        return socket.executeAsync(rpcRequest, mapper = pojo<OptimalAutomationResponse>().nonNull())
    }

    private class GsonNumberMapper : NullableMapper<Balance>() {
        override fun mapNullable(rpcResponse: RpcResponse, jsonMapper: Gson): Balance? {
            return rpcResponse.result.asGsonParsedNumberOrNull()
        }
    }
}
