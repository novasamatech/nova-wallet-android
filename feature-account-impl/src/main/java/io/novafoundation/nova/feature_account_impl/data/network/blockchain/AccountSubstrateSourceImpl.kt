package io.novafoundation.nova.feature_account_impl.data.network.blockchain

import io.novafoundation.nova.common.data.network.rpc.SocketSingleRequestExecutor
import jp.co.soramitsu.fearless_utils.wsrpc.mappers.nonNull
import jp.co.soramitsu.fearless_utils.wsrpc.mappers.pojo
import jp.co.soramitsu.fearless_utils.wsrpc.request.base.RpcRequest
import jp.co.soramitsu.fearless_utils.wsrpc.request.runtime.system.NodeNetworkTypeRequest

class AccountSubstrateSourceImpl(
    private val socketRequestExecutor: SocketSingleRequestExecutor
) : AccountSubstrateSource {

    override suspend fun getNodeNetworkType(nodeHost: String): String {
        val request = NodeNetworkTypeRequest()

        return socketRequestExecutor.executeRequest(RpcRequest.Rpc2(request), nodeHost, pojo<String>().nonNull())
    }
}
