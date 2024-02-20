package io.novafoundation.nova.feature_account_impl.data.network.blockchain

import io.novafoundation.nova.common.data.network.rpc.SocketSingleRequestExecutor
import io.novasama.substrate_sdk_android.wsrpc.mappers.nonNull
import io.novasama.substrate_sdk_android.wsrpc.mappers.pojo
import io.novasama.substrate_sdk_android.wsrpc.request.base.RpcRequest
import io.novasama.substrate_sdk_android.wsrpc.request.runtime.system.NodeNetworkTypeRequest

class AccountSubstrateSourceImpl(
    private val socketRequestExecutor: SocketSingleRequestExecutor
) : AccountSubstrateSource {

    override suspend fun getNodeNetworkType(nodeHost: String): String {
        val request = NodeNetworkTypeRequest()

        return socketRequestExecutor.executeRequest(RpcRequest.Rpc2(request), nodeHost, pojo<String>().nonNull())
    }
}
