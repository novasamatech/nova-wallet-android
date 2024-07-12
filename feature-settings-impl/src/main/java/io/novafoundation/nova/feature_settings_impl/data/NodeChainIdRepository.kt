package io.novafoundation.nova.feature_settings_impl.data

import io.novafoundation.nova.common.data.network.runtime.calls.GetBlockHashRequest
import io.novafoundation.nova.common.utils.removeHexPrefix
import io.novafoundation.nova.core.ethereum.Web3Api
import io.novafoundation.nova.runtime.ethereum.Web3ApiFactory
import io.novafoundation.nova.runtime.ethereum.sendSuspend
import io.novafoundation.nova.runtime.ext.evmChainIdFrom
import io.novafoundation.nova.runtime.multiNetwork.chain.model.NetworkType
import io.novafoundation.nova.runtime.multiNetwork.connection.node.connection.NodeConnection
import io.novafoundation.nova.runtime.multiNetwork.connection.node.connection.NodeConnectionFactory
import io.novasama.substrate_sdk_android.wsrpc.executeAsync
import io.novasama.substrate_sdk_android.wsrpc.mappers.nonNull
import io.novasama.substrate_sdk_android.wsrpc.mappers.pojo
import java.math.BigInteger
import kotlinx.coroutines.CoroutineScope

interface NodeChainIdRepository {

    suspend fun requestChainId(): String
}

class NodeChainIdRepositoryFactory(
    private val nodeConnectionFactory: NodeConnectionFactory,
    private val web3ApiFactory: Web3ApiFactory
) {

    fun create(networkType: NetworkType, nodeUrl: String, coroutineScope: CoroutineScope): NodeChainIdRepository {
        val nodeConnection = nodeConnectionFactory.createNodeConnection(nodeUrl, coroutineScope)

        return when (networkType) {
            NetworkType.SUBSTRATE -> substrate(nodeConnection)

            NetworkType.EVM -> evm(nodeConnection)
        }
    }

    fun substrate(nodeConnection: NodeConnection): SubstrateNodeChainIdRepository {
        return SubstrateNodeChainIdRepository(nodeConnection)
    }

    fun evm(nodeConnection: NodeConnection): EthereumNodeChainIdRepository {
        return EthereumNodeChainIdRepository(nodeConnection, web3ApiFactory)
    }
}

class SubstrateNodeChainIdRepository(
    private val nodeConnection: NodeConnection
) : NodeChainIdRepository {

    override suspend fun requestChainId(): String {
        val genesisHash = nodeConnection.getSocketService().executeAsync(
            GetBlockHashRequest(BigInteger.ZERO),
            mapper = pojo<String>().nonNull()
        )

        return genesisHash.removeHexPrefix()
    }
}

class EthereumNodeChainIdRepository(
    private val nodeConnection: NodeConnection,
    private val web3ApiFactory: Web3ApiFactory
) : NodeChainIdRepository {

    private val web3Api = createWeb3Api()

    override suspend fun requestChainId(): String {
        val chainId = web3Api.ethChainId().sendSuspend().chainId

        return evmChainIdFrom(chainId)
    }

    private fun createWeb3Api(): Web3Api {
        return web3ApiFactory.createWss(nodeConnection.getSocketService())
    }
}
