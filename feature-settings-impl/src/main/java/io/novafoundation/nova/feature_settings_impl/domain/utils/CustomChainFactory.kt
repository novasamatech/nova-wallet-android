package io.novafoundation.nova.feature_settings_impl.domain.utils

import io.novafoundation.nova.common.data.network.coingecko.CoinGeckoLinkParser
import io.novafoundation.nova.common.data.network.runtime.model.SystemProperties
import io.novafoundation.nova.common.data.network.runtime.model.firstTokenDecimals
import io.novafoundation.nova.common.utils.DEFAULT_PREFIX
import io.novafoundation.nova.common.utils.Precision
import io.novafoundation.nova.common.utils.asPrecision
import io.novafoundation.nova.common.utils.asTokenSymbol
import io.novafoundation.nova.common.utils.orFalse
import io.novafoundation.nova.feature_settings_impl.data.NodeChainIdRepositoryFactory
import io.novafoundation.nova.feature_settings_impl.domain.model.CustomNetworkPayload
import io.novafoundation.nova.runtime.explorer.BlockExplorerLinkFormatter
import io.novafoundation.nova.runtime.ext.EVM_DEFAULT_TOKEN_DECIMALS
import io.novafoundation.nova.runtime.ext.evmChainIdFrom
import io.novafoundation.nova.runtime.ext.utilityAsset
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain.Nodes.NodeSelectionStrategy
import io.novafoundation.nova.runtime.multiNetwork.connection.node.connection.NodeConnection
import io.novafoundation.nova.runtime.multiNetwork.connection.node.connection.NodeConnectionFactory
import io.novafoundation.nova.runtime.network.rpc.systemProperties
import io.novafoundation.nova.runtime.util.fetchRuntimeSnapshot
import io.novafoundation.nova.runtime.util.isEthereumAddress
import io.novasama.substrate_sdk_android.ss58.SS58Encoder
import kotlinx.coroutines.CoroutineScope

class CustomChainFactory(
    private val nodeConnectionFactory: NodeConnectionFactory,
    private val nodeChainIdRepositoryFactory: NodeChainIdRepositoryFactory,
    private val coinGeckoLinkParser: CoinGeckoLinkParser,
    private val blockExplorerLinkFormatter: BlockExplorerLinkFormatter,
) {

    suspend fun createSubstrateChain(
        payload: CustomNetworkPayload,
        prefilledChain: Chain?,
        coroutineScope: CoroutineScope
    ): Chain {
        val nodeConnection = nodeConnectionFactory.createNodeConnection(payload.nodeUrl, coroutineScope)
        val substrateNodeIdRequester = nodeChainIdRepositoryFactory.substrate(nodeConnection)
        val runtime = nodeConnection.getSocketService().fetchRuntimeSnapshot()

        val (precision, addressPrefix) = getMainTokenPrecisionAndAddressPrefix(prefilledChain, nodeConnection)

        return createChain(
            chainId = substrateNodeIdRequester.requestChainId(),
            addressPrefix = addressPrefix,
            isEthereumBased = runtime.isEthereumAddress(),
            hasSubstrateRuntime = true,
            assetDecimals = precision,
            assetType = prefilledChain?.utilityAsset?.type ?: Chain.Asset.Type.Native,
            payload = payload,
            prefilledChain = prefilledChain,
        )
    }

    fun createEvmChain(
        payload: CustomNetworkPayload,
        prefilledChain: Chain?
    ): Chain {
        val evmChainId = payload.evmChainId!!
        val chainId = evmChainIdFrom(evmChainId)

        return createChain(
            chainId = chainId,
            addressPrefix = evmChainId,
            isEthereumBased = true,
            hasSubstrateRuntime = false,
            assetDecimals = EVM_DEFAULT_TOKEN_DECIMALS.asPrecision(),
            assetType = Chain.Asset.Type.EvmNative,
            payload = payload,
            prefilledChain = prefilledChain,
        )
    }

    private fun createChain(
        chainId: String,
        addressPrefix: Int,
        isEthereumBased: Boolean,
        hasSubstrateRuntime: Boolean,
        assetDecimals: Precision,
        assetType: Chain.Asset.Type,
        payload: CustomNetworkPayload,
        prefilledChain: Chain?
    ): Chain {
        val priceId = payload.coingeckoLinkUrl?.let { coinGeckoLinkParser.parse(it).getOrNull()?.priceId }

        val prefilledUtilityAsset = prefilledChain?.utilityAsset

        val asset = Chain.Asset(
            id = 0,
            name = payload.chainName,
            enabled = true,
            icon = prefilledUtilityAsset?.icon,
            priceId = priceId,
            chainId = chainId,
            symbol = payload.tokenSymbol.asTokenSymbol(),
            precision = assetDecimals,
            buyProviders = prefilledUtilityAsset?.buyProviders.orEmpty(),
            staking = prefilledUtilityAsset?.staking.orEmpty(),
            type = assetType,
            source = Chain.Asset.Source.MANUAL,
        )

        val explorer = getChainExplorer(payload.blockExplorer, chainId)

        val nodes = Chain.Nodes(
            autoBalanceStrategy = prefilledChain?.nodes?.autoBalanceStrategy ?: Chain.Nodes.AutoBalanceStrategy.ROUND_ROBIN,
            wssNodeSelectionStrategy = NodeSelectionStrategy.AutoBalance,
            nodes = createNodeList(chainId, prefilledChain, payload)
        )

        return Chain(
            id = chainId,
            parentId = prefilledChain?.parentId,
            name = payload.chainName,
            assets = listOf(asset),
            nodes = nodes,
            explorers = explorer?.let(::listOf) ?: prefilledChain?.explorers.orEmpty(),
            externalApis = prefilledChain?.externalApis.orEmpty(),
            icon = prefilledChain?.icon,
            addressPrefix = addressPrefix,
            types = prefilledChain?.types,
            isEthereumBased = isEthereumBased,
            isTestNet = prefilledChain?.isTestNet.orFalse(),
            source = Chain.Source.CUSTOM,
            hasSubstrateRuntime = hasSubstrateRuntime,
            pushSupport = prefilledChain?.pushSupport.orFalse(),
            hasCrowdloans = prefilledChain?.hasCrowdloans.orFalse(),
            supportProxy = prefilledChain?.supportProxy.orFalse(),
            governance = prefilledChain?.governance.orEmpty(),
            swap = prefilledChain?.swap.orEmpty(),
            customFee = prefilledChain?.customFee.orEmpty(),
            connectionState = Chain.ConnectionState.FULL_SYNC,
            additional = prefilledChain?.additional
        )
    }

    private fun createNodeList(
        chainId: String,
        prefilledChain: Chain?,
        input: CustomNetworkPayload
    ): List<Chain.Node> {
        val inputNode = Chain.Node(
            chainId = chainId,
            unformattedUrl = input.nodeUrl,
            name = input.nodeName,
            orderId = 0,
            isCustom = true,
        )

        val prefilledNodes = prefilledChain?.nodes?.nodes.orEmpty()
        val prefilledExceptInput = prefilledNodes.mapNotNull {
            val differentFromInput = it.unformattedUrl != inputNode.unformattedUrl

            if (differentFromInput) {
                // Consider prefilled nodes as custom
                it.copy(isCustom = true)
            } else {
                null
            }
        }

        return buildList {
            add(inputNode)
            addAll(prefilledExceptInput)
        }
    }

    fun getChainExplorer(blockExplorer: CustomNetworkPayload.BlockExplorer?, chainId: String): Chain.Explorer? {
        return blockExplorer?.let {
            val links = blockExplorerLinkFormatter.format(it.url)
            Chain.Explorer(
                chainId = chainId,
                name = it.name,
                account = links?.account,
                extrinsic = links?.extrinsic,
                event = links?.event,
            )
        }
    }

    private suspend fun getSubstrateChainProperties(nodeConnection: NodeConnection): SystemProperties {
        return nodeConnection.getSocketService().systemProperties()
    }

    private suspend fun getMainTokenPrecisionAndAddressPrefix(chain: Chain?, nodeConnection: NodeConnection): Pair<Precision, Int> {
        if (chain != null) {
            val asset = chain.utilityAsset
            return Pair(asset.precision, chain.addressPrefix)
        } else {
            val systemProperties = getSubstrateChainProperties(nodeConnection)
            return Pair(
                systemProperties.firstTokenDecimals().asPrecision(),
                systemProperties.ss58Format ?: systemProperties.SS58Prefix ?: SS58Encoder.DEFAULT_PREFIX.toInt()
            )
        }
    }
}
