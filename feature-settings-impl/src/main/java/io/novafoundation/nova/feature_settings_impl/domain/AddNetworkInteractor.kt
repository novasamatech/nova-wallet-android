package io.novafoundation.nova.feature_settings_impl.domain

import io.novafoundation.nova.common.data.network.coingecko.CoinGeckoLinkParser
import io.novafoundation.nova.common.data.network.runtime.calls.GetSystemPropertiesRequest
import io.novafoundation.nova.common.data.network.runtime.model.SystemProperties
import io.novafoundation.nova.common.utils.asPrecision
import io.novafoundation.nova.common.utils.asTokenSymbol
import io.novafoundation.nova.common.validation.ValidationSystem
import io.novafoundation.nova.feature_assets.domain.tokens.add.validations.CoinGeckoLinkValidationFactory
import io.novafoundation.nova.feature_settings_impl.data.NodeChainIdRepositoryFactory
import io.novafoundation.nova.feature_settings_impl.domain.validation.customNetwork.CustomNetworkValidationSystem
import io.novafoundation.nova.feature_settings_impl.domain.validation.NodeChainIdSingletonProvider
import io.novafoundation.nova.feature_settings_impl.domain.validation.customNetwork.validCoinGeckoLink
import io.novafoundation.nova.feature_settings_impl.domain.validation.customNetwork.validateNetworkNodeIsAlive
import io.novafoundation.nova.feature_settings_impl.domain.validation.customNetwork.validateNetworkNotAdded
import io.novafoundation.nova.feature_settings_impl.domain.validation.customNetwork.validateNodeSupportedByNetwork
import io.novafoundation.nova.runtime.explorer.BlockExplorerLinkFormatter
import io.novafoundation.nova.runtime.ext.EVM_DEFAULT_TOKEN_DECIMALS
import io.novafoundation.nova.runtime.ext.evmChainIdFrom
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.NetworkType
import io.novafoundation.nova.runtime.multiNetwork.connection.node.connection.NodeConnection
import io.novafoundation.nova.runtime.multiNetwork.connection.node.connection.NodeConnectionFactory
import io.novafoundation.nova.runtime.repository.ChainRepository
import io.novasama.substrate_sdk_android.wsrpc.executeAsync
import io.novasama.substrate_sdk_android.wsrpc.mappers.nonNull
import io.novasama.substrate_sdk_android.wsrpc.mappers.pojo
import kotlinx.coroutines.CoroutineScope

interface AddNetworkInteractor {

    suspend fun createSubstrateNetwork(
        iconUrl: String?,
        nodeUrl: String,
        nodeName: String,
        chainName: String,
        tokenSymbol: String,
        blockExplorer: Pair<String, String>?,
        coingeckoLink: String?,
        coroutineScope: CoroutineScope
    ): Result<Unit>

    suspend fun createEvmNetwork(
        chainId: Int,
        iconUrl: String?,
        nodeUrl: String,
        nodeName: String,
        chainName: String,
        tokenSymbol: String,
        blockExplorer: Pair<String, String>?,
        coingeckoLink: String?
    ): Result<Unit>

    suspend fun updateChain(
        chainId: String,
        chainName: String,
        tokenSymbol: String,
        blockExplorerNameAndUrl: Pair<String, String>?,
        coingeckoLinkUrl: String?
    ): Result<Unit>

    fun getSubstrateValidationSystem(coroutineScope: CoroutineScope): CustomNetworkValidationSystem

    fun getEvmValidationSystem(coroutineScope: CoroutineScope): CustomNetworkValidationSystem
}

class RealAddNetworkInteractor(
    private val chainRepository: ChainRepository,
    private val chainRegistry: ChainRegistry,
    private val nodeChainIdRepositoryFactory: NodeChainIdRepositoryFactory,
    private val coinGeckoLinkValidationFactory: CoinGeckoLinkValidationFactory,
    private val coinGeckoLinkParser: CoinGeckoLinkParser,
    private val blockExplorerLinkFormatter: BlockExplorerLinkFormatter,
    private val nodeConnectionFactory: NodeConnectionFactory,
) : AddNetworkInteractor {

    override suspend fun createSubstrateNetwork(
        iconUrl: String?,
        nodeUrl: String,
        nodeName: String,
        chainName: String,
        tokenSymbol: String,
        blockExplorer: Pair<String, String>?, // name, url
        coingeckoLink: String?,
        coroutineScope: CoroutineScope
    ) = runCatching {
        val nodeConnection = nodeConnectionFactory.createNodeConnection(nodeUrl, coroutineScope)
        val substrateNodeIdRequester = nodeChainIdRepositoryFactory.substrate(nodeConnection)

        val chainProperties = getSubstrateChainProperties(nodeConnection)

        val tokenDecimals = if (chainProperties.tokenSymbol == tokenSymbol) {
            chainProperties.tokenDecimals
        } else {
            null
        }

        val chain = createChain(
            chainId = substrateNodeIdRequester.requestChainId(),
            icon = iconUrl,
            nodeUrl = nodeUrl,
            nodeName = nodeName,
            chainName = chainName,
            tokenSymbol = tokenSymbol,
            blockExplorer = blockExplorer,
            coingeckoLink = coingeckoLink,
            addressPrefix = chainProperties.SS58Prefix ?: chainProperties.ss58Format ?: 1,
            isEthereumBased = chainProperties.isEthereum ?: false,
            hasSubstrateRuntime = true,
            assetDecimals = tokenDecimals,
            assetType = Chain.Asset.Type.Native,
        )

        chainRepository.addChain(chain)
    }

    override suspend fun createEvmNetwork(
        chainId: Int,
        iconUrl: String?,
        nodeUrl: String,
        nodeName: String,
        chainName: String,
        tokenSymbol: String,
        blockExplorer: Pair<String, String>?,
        coingeckoLink: String?
    ) = runCatching {
        val evmChainId = evmChainIdFrom(chainId)

        val chain = createChain(
            chainId = evmChainId,
            icon = iconUrl,
            nodeUrl = nodeUrl,
            nodeName = nodeName,
            chainName = chainName,
            tokenSymbol = tokenSymbol,
            blockExplorer = blockExplorer,
            coingeckoLink = coingeckoLink,
            addressPrefix = chainId,
            isEthereumBased = true,
            hasSubstrateRuntime = false,
            assetDecimals = EVM_DEFAULT_TOKEN_DECIMALS,
            assetType = Chain.Asset.Type.EvmNative,
        )

        chainRepository.addChain(chain)
    }

    private fun createChain(
        chainId: String,
        icon: String?,
        nodeUrl: String,
        nodeName: String,
        chainName: String,
        tokenSymbol: String,
        blockExplorer: Pair<String, String>?, // name, url
        coingeckoLink: String?,
        addressPrefix: Int,
        isEthereumBased: Boolean,
        hasSubstrateRuntime: Boolean,
        assetDecimals: Int?,
        assetType: Chain.Asset.Type
    ): Chain {
        val priceId = coingeckoLink?.let { coinGeckoLinkParser.parse(it).getOrNull()?.priceId }

        val asset = Chain.Asset(
            id = 0,
            name = chainName,
            enabled = true,
            iconUrl = null,
            priceId = priceId,
            chainId = chainId,
            symbol = tokenSymbol.asTokenSymbol(),
            precision = assetDecimals?.asPrecision() ?: 1.asPrecision(),
            buyProviders = emptyMap(),
            staking = listOf(),
            type = assetType,
            source = Chain.Asset.Source.MANUAL,
        )

        val node = Chain.Node(
            chainId = chainId,
            unformattedUrl = nodeUrl,
            name = nodeName,
            orderId = 0,
            isCustom = true,
        )

        val explorer = getChainExplorer(blockExplorer, chainId)

        return Chain(
            id = chainId,
            parentId = null,
            name = chainName,
            assets = listOf(asset),
            nodes = Chain.Nodes(Chain.Nodes.NodeSelectionStrategy.AutoBalance.ROUND_ROBIN, listOf(node)),
            explorers = listOfNotNull(explorer),
            externalApis = listOf(),
            icon = icon,
            addressPrefix = addressPrefix,
            types = null,
            isEthereumBased = isEthereumBased,
            isTestNet = false,
            source = Chain.Source.CUSTOM,
            hasSubstrateRuntime = hasSubstrateRuntime,
            pushSupport = false,
            hasCrowdloans = false,
            supportProxy = false,
            governance = listOf(),
            swap = listOf(),
            connectionState = Chain.ConnectionState.FULL_SYNC,
            additional = null,
        )
    }

    override fun getSubstrateValidationSystem(coroutineScope: CoroutineScope): CustomNetworkValidationSystem {
        return ValidationSystem {
            validCoinGeckoLink(coinGeckoLinkValidationFactory)

            // Use singleton here to receive chain id only one time for all vaildations
            val chainIdRequestSingleton = NodeChainIdSingletonProvider(nodeChainIdRepositoryFactory, coroutineScope)
            validateNetworkNodeIsAlive { chainIdRequestSingleton.getChainId(NetworkType.SUBSTRATE, it.nodeUrl) }
            validateNetworkNotAdded(chainRegistry) { chainIdRequestSingleton.getChainId() }
        }
    }

    override fun getEvmValidationSystem(coroutineScope: CoroutineScope): CustomNetworkValidationSystem {
        return ValidationSystem {
            validCoinGeckoLink(coinGeckoLinkValidationFactory)

            validateNetworkNotAdded(chainRegistry) { evmChainIdFrom(it.evmChainId!!) }

            // Use singletone here to receive chain id only one time for all vaildations
            val chainIdRequestSingleton = NodeChainIdSingletonProvider(nodeChainIdRepositoryFactory, coroutineScope)
            validateNetworkNodeIsAlive { chainIdRequestSingleton.getChainId(NetworkType.EVM, it.nodeUrl) }
            validateNodeSupportedByNetwork { chainIdRequestSingleton.getChainId() }
        }
    }

    override suspend fun updateChain(
        chainId: String,
        chainName: String,
        tokenSymbol: String,
        blockExplorerNameAndUrl: Pair<String, String>?,
        coingeckoLinkUrl: String?
    ): Result<Unit> {
        return runCatching {
            val blockExplorer = getChainExplorer(blockExplorer = blockExplorerNameAndUrl, chainId = chainId)
            val priceId = coingeckoLinkUrl?.let { coinGeckoLinkParser.parse(it).getOrNull()?.priceId }

            chainRepository.editChain(chainId, chainName, tokenSymbol, blockExplorer, priceId)
        }
    }

    private suspend fun getSubstrateChainProperties(nodeConnection: NodeConnection): SystemProperties {
        return nodeConnection.getSocketService()
            .executeAsync(GetSystemPropertiesRequest(), mapper = pojo<SystemProperties>().nonNull())
    }

    private fun getChainExplorer(blockExplorer: Pair<String, String>?, chainId: String): Chain.Explorer? {
        return blockExplorer?.let { (name, url) ->
            val links = blockExplorerLinkFormatter.format(url)
            Chain.Explorer(
                chainId = chainId,
                name = name,
                account = links?.account,
                extrinsic = links?.extrinsic,
                event = links?.event,
            )
        }
    }
}
