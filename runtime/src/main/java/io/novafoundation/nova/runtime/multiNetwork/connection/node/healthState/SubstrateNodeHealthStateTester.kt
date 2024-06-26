package io.novafoundation.nova.runtime.multiNetwork.connection.node.healthState

import io.novafoundation.nova.common.data.network.rpc.BulkRetriever
import io.novafoundation.nova.common.data.network.rpc.queryKey
import io.novafoundation.nova.common.utils.awaitConnected
import io.novafoundation.nova.runtime.ext.emptyAccountId
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.connection.ConnectionSecrets
import io.novafoundation.nova.runtime.multiNetwork.connection.saturateNodeUrl
import io.novasama.substrate_sdk_android.extensions.toHexString
import io.novasama.substrate_sdk_android.hash.Hasher.xxHash128
import io.novasama.substrate_sdk_android.wsrpc.SocketService
import io.novasama.substrate_sdk_android.wsrpc.interceptor.WebSocketResponseInterceptor
import io.novasama.substrate_sdk_android.wsrpc.response.RpcResponse
import kotlin.time.ExperimentalTime
import kotlin.time.measureTime

class SubstrateNodeHealthStateTester(
    private val chain: Chain,
    private val node: Chain.Node,
    private val connectionSecrets: ConnectionSecrets,
    private val bulkRetriever: BulkRetriever,
    val socketService: SocketService
) : NodeHealthStateTester, WebSocketResponseInterceptor {

    init {
        val saturatedUrlNode = node.saturateNodeUrl(connectionSecrets)
        socketService.setInterceptor(this)

        saturatedUrlNode?.let {
            socketService.start(it.saturatedUrl)
        }
    }

    @OptIn(ExperimentalTime::class)
    override suspend fun testNodeHealthState(): Result<Long> {
        val storageKey = systemAccountStorageKey().toHexString(withPrefix = true)

        return runCatching {
            socketService.awaitConnected()

            val duration = measureTime {
                bulkRetriever.queryKey(socketService, storageKey)
            }

            duration.inWholeMilliseconds
        }
    }

    private fun systemAccountStorageKey(): ByteArray {
        return "System".toByteArray().xxHash128() + "Account".toByteArray().xxHash128() + chain.emptyAccountId()
    }

    override fun onRpcResponseReceived(rpcResponse: RpcResponse): WebSocketResponseInterceptor.ResponseDelivery {
        return WebSocketResponseInterceptor.ResponseDelivery.DELIVER_TO_SENDER
    }
}
