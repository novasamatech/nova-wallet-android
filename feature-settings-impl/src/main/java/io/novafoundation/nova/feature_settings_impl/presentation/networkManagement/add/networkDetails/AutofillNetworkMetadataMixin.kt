package io.novafoundation.nova.feature_settings_impl.presentation.networkManagement.add.networkDetails

import io.novafoundation.nova.common.data.network.runtime.calls.GetChainNameRequest
import io.novafoundation.nova.common.data.network.runtime.calls.GetSystemPropertiesRequest
import io.novafoundation.nova.common.data.network.runtime.model.SystemProperties
import io.novafoundation.nova.core.ethereum.Web3Api
import io.novafoundation.nova.runtime.ethereum.Web3ApiFactory
import io.novafoundation.nova.runtime.ethereum.sendSuspend
import io.novafoundation.nova.runtime.multiNetwork.connection.node.connection.NodeConnection
import io.novafoundation.nova.runtime.multiNetwork.connection.node.connection.NodeConnectionFactory
import io.novasama.substrate_sdk_android.wsrpc.executeAsync
import io.novasama.substrate_sdk_android.wsrpc.mappers.nonNull
import io.novasama.substrate_sdk_android.wsrpc.mappers.pojo
import java.math.BigInteger
import kotlinx.coroutines.CoroutineScope

class AutofillNetworkData(
    val chainName: String?,
    val tokenSymbol: String?,
    val evmChainId: BigInteger?
)

class AutofillNetworkMetadataMixinFactory(
    private val nodeConnectionFactory: NodeConnectionFactory,
    private val web3ApiFactory: Web3ApiFactory
) {
    fun substrate(coroutineScope: CoroutineScope): SubstrateAutofillNetworkMetadataMixin {
        return SubstrateAutofillNetworkMetadataMixin(nodeConnectionFactory, coroutineScope)
    }

    fun evm(coroutineScope: CoroutineScope): EvmAutofillNetworkMetadataMixin {
        return EvmAutofillNetworkMetadataMixin(nodeConnectionFactory, coroutineScope, web3ApiFactory)
    }
}

interface AutofillNetworkMetadataMixin {

    suspend fun autofill(url: String): Result<AutofillNetworkData>
}

class SubstrateAutofillNetworkMetadataMixin(
    private val nodeConnectionFactory: NodeConnectionFactory,
    private val coroutineScope: CoroutineScope
) : AutofillNetworkMetadataMixin {

    private var nodeConnection: NodeConnection? = null

    override suspend fun autofill(url: String): Result<AutofillNetworkData> = runCatching {
        if (nodeConnection == null) {
            nodeConnection = nodeConnectionFactory.createNodeConnection(url, coroutineScope)
        } else {
            nodeConnection!!.switchUrl(url)
        }

        val properties = getSubstrateChainProperties(nodeConnection!!)
        val chainName = getSubstrateChainName(nodeConnection!!)

        AutofillNetworkData(
            chainName = chainName,
            tokenSymbol = properties.tokenSymbol,
            evmChainId = null
        )
    }

    private suspend fun getSubstrateChainProperties(nodeConnection: NodeConnection): SystemProperties {
        return nodeConnection.getSocketService()
            .executeAsync(GetSystemPropertiesRequest(), mapper = pojo<SystemProperties>().nonNull())
    }

    private suspend fun getSubstrateChainName(nodeConnection: NodeConnection): String {
        return nodeConnection.getSocketService()
            .executeAsync(GetChainNameRequest(), mapper = pojo<String>().nonNull())
    }
}

class EvmAutofillNetworkMetadataMixin(
    private val nodeConnectionFactory: NodeConnectionFactory,
    private val coroutineScope: CoroutineScope,
    private val web3ApiFactory: Web3ApiFactory
) : AutofillNetworkMetadataMixin {

    private var nodeConnection: NodeConnection? = null
    private var web3Api: Web3Api? = null

    override suspend fun autofill(url: String): Result<AutofillNetworkData> = runCatching {
        if (nodeConnection == null) {
            nodeConnection = nodeConnectionFactory.createNodeConnection(url, coroutineScope)
        } else {
            nodeConnection!!.switchUrl(url)
        }

        if (web3Api == null) {
            web3Api = web3ApiFactory.createWss(nodeConnection!!.getSocketService())
        }

        val chainId = web3Api!!.ethChainId().sendSuspend().chainId

        AutofillNetworkData(
            chainName = null,
            tokenSymbol = null,
            evmChainId = chainId
        )
    }
}
