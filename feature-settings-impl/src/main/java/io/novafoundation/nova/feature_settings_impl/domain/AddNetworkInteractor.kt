package io.novafoundation.nova.feature_settings_impl.domain

import io.novafoundation.nova.common.data.network.coingecko.CoinGeckoLinkParser
import io.novafoundation.nova.common.data.network.runtime.model.firstTokenSymbol
import io.novafoundation.nova.common.validation.ValidationSystem
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_assets.domain.tokens.add.validations.CoinGeckoLinkValidationFactory
import io.novafoundation.nova.feature_settings_impl.data.NodeChainIdRepositoryFactory
import io.novafoundation.nova.feature_settings_impl.domain.model.CustomNetworkPayload
import io.novafoundation.nova.feature_settings_impl.domain.utils.CustomChainFactory
import io.novafoundation.nova.feature_settings_impl.domain.validation.customNetwork.CustomNetworkValidationSystem
import io.novafoundation.nova.feature_settings_impl.domain.validation.NodeChainIdSingletonHelper
import io.novafoundation.nova.feature_settings_impl.domain.validation.NodeConnectionSingletonHelper
import io.novafoundation.nova.feature_settings_impl.domain.validation.customNetwork.validateTokenSymbol
import io.novafoundation.nova.feature_settings_impl.domain.validation.customNetwork.validCoinGeckoLink
import io.novafoundation.nova.feature_settings_impl.domain.validation.customNetwork.validateNetworkNodeIsAlive
import io.novafoundation.nova.feature_settings_impl.domain.validation.customNetwork.validateNetworkNotAdded
import io.novafoundation.nova.feature_settings_impl.domain.validation.customNetwork.validateNodeSupportedByNetwork
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.AssetVisibilityRepository
import io.novafoundation.nova.runtime.ext.evmChainIdFrom
import io.novafoundation.nova.runtime.ext.fullId
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.NetworkType
import io.novafoundation.nova.runtime.multiNetwork.connection.node.connection.NodeConnectionFactory
import io.novafoundation.nova.runtime.network.rpc.systemProperties
import io.novafoundation.nova.runtime.repository.ChainRepository
import kotlinx.coroutines.CoroutineScope

interface AddNetworkInteractor {

    suspend fun createSubstrateNetwork(
        payload: CustomNetworkPayload,
        prefilledChain: Chain?,
        coroutineScope: CoroutineScope
    ): Result<Unit>

    suspend fun createEvmNetwork(
        payload: CustomNetworkPayload,
        prefilledChain: Chain?
    ): Result<Unit>

    suspend fun updateChain(
        chainId: String,
        chainName: String,
        tokenSymbol: String,
        blockExplorerModel: CustomNetworkPayload.BlockExplorer?,
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
    private val nodeConnectionFactory: NodeConnectionFactory,
    private val customChainFactory: CustomChainFactory,
    private val accountRepository: AccountRepository,
    private val assetVisibilityRepository: AssetVisibilityRepository
) : AddNetworkInteractor {

    override suspend fun createSubstrateNetwork(
        payload: CustomNetworkPayload,
        prefilledChain: Chain?,
        coroutineScope: CoroutineScope
    ) = runCatching {
        val chain = customChainFactory.createSubstrateChain(payload, prefilledChain, coroutineScope)

        addChainAndShowAssets(chain)
    }

    override suspend fun createEvmNetwork(
        payload: CustomNetworkPayload,
        prefilledChain: Chain?
    ) = runCatching {
        val chain = customChainFactory.createEvmChain(payload, prefilledChain)

        addChainAndShowAssets(chain)
    }

    override fun getSubstrateValidationSystem(coroutineScope: CoroutineScope): CustomNetworkValidationSystem {
        return ValidationSystem {
            validCoinGeckoLink(coinGeckoLinkValidationFactory)

            // Using singleton her to receive chain id only one time for all vaildations
            val nodeConnectionHelper = getNodeConnectionSingletonHelper(coroutineScope)
            val chainIdHelper = getChainIdSingletonHelper(nodeConnectionHelper)
            validateNetworkNodeIsAlive { chainIdHelper.getChainId(NetworkType.SUBSTRATE, it.nodeUrl) }
            validateNetworkNotAdded(chainRegistry) { chainIdHelper.getChainId() }
            validateTokenSymbol {
                val systemProperties = nodeConnectionHelper.getNodeConnection()
                    .getSocketService()
                    .systemProperties()

                systemProperties.firstTokenSymbol()
            }
        }
    }

    override fun getEvmValidationSystem(coroutineScope: CoroutineScope): CustomNetworkValidationSystem {
        return ValidationSystem {
            validCoinGeckoLink(coinGeckoLinkValidationFactory)

            validateNetworkNotAdded(chainRegistry) { evmChainIdFrom(it.evmChainId!!) }

            // Using singleton here to receive chain id only one time for all vaildations
            val nodeConnectionHelper = getNodeConnectionSingletonHelper(coroutineScope)
            val chainIdHelper = getChainIdSingletonHelper(nodeConnectionHelper)
            validateNetworkNodeIsAlive { chainIdHelper.getChainId(NetworkType.EVM, it.nodeUrl) }
            validateNodeSupportedByNetwork { chainIdHelper.getChainId() }
        }
    }

    override suspend fun updateChain(
        chainId: String,
        chainName: String,
        tokenSymbol: String,
        blockExplorerModel: CustomNetworkPayload.BlockExplorer?,
        coingeckoLinkUrl: String?
    ): Result<Unit> {
        return runCatching {
            val blockExplorer = customChainFactory.getChainExplorer(blockExplorer = blockExplorerModel, chainId = chainId)
            val priceId = coingeckoLinkUrl?.let { coinGeckoLinkParser.parse(it).getOrNull()?.priceId }

            chainRepository.editChain(chainId, chainName, tokenSymbol, blockExplorer, priceId)
        }
    }

    /**
     * A network the user adds is never in the curated default list, so without an explicit decision its tokens
     * would stay hidden right after it was added. Shown in the wallet the user added it from - other wallets
     * still pick the tokens up through balance discovery once they hold them.
     *
     * Visibility rows reference the chain assets, so they can only be written once the chain is inserted.
     */
    private suspend fun addChainAndShowAssets(chain: Chain) {
        chainRepository.addChain(chain)

        val selectedMetaId = accountRepository.getSelectedMetaAccount().id
        val choices = chain.assets.associate { it.fullId to true }

        assetVisibilityRepository.setVisibility(selectedMetaId, choices)
    }

    private fun getNodeConnectionSingletonHelper(coroutineScope: CoroutineScope): NodeConnectionSingletonHelper {
        return NodeConnectionSingletonHelper(nodeConnectionFactory, coroutineScope)
    }

    private fun getChainIdSingletonHelper(helper: NodeConnectionSingletonHelper): NodeChainIdSingletonHelper {
        return NodeChainIdSingletonHelper(helper, nodeChainIdRepositoryFactory)
    }
}
