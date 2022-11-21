package io.novafoundation.nova.feature_assets.domain.manageTokens

import io.novafoundation.nova.feature_assets.domain.common.searchTokens
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.ChainAssetRepository
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.WalletRepository
import io.novafoundation.nova.runtime.ext.defaultComparator
import io.novafoundation.nova.runtime.ext.unifiedSymbol
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.ChainWithAsset
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.FullChainAssetId
import io.novafoundation.nova.runtime.multiNetwork.chainsById
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

interface ManageTokenInteractor {

    fun multiChainTokensFlow(queryFlow: Flow<String>): Flow<List<MultiChainToken>>

    fun multiChainTokenFlow(id: String): Flow<MultiChainToken>

    suspend fun updateEnabledState(enabled: Boolean, assetIds: List<FullChainAssetId>)
}

class RealManageTokenInteractor(
    private val chainRegistry: ChainRegistry,
    private val walletRepository: WalletRepository,
    private val chainAssetRepository: ChainAssetRepository,
) : ManageTokenInteractor {

    override fun multiChainTokensFlow(
        queryFlow: Flow<String>
    ): Flow<List<MultiChainToken>> {
        return combine(multiChainTokensFlow(), queryFlow) { tokens, query ->
            tokens.searchTokens(
                query = query,
                chainsById = chainRegistry.chainsById(),
                tokenSymbol = MultiChainToken::symbol,
                relevantToChains = { multiChainToken, chainIds ->
                    multiChainToken.instances.any { it.chain.id in chainIds }
                }
            )
        }
    }

    private fun multiChainTokensFlow() = chainRegistry.currentChains.map { chains ->
        constructMultiChainTokens(chains)
    }

    override fun multiChainTokenFlow(id: String): Flow<MultiChainToken> {
        return multiChainTokensFlow().map { multiChainTokens ->
            multiChainTokens.first { it.id == id }
        }
    }

    override suspend fun updateEnabledState(enabled: Boolean, assetIds: List<FullChainAssetId>) = withContext(Dispatchers.IO) {
        chainAssetRepository.setAssetsEnabled(enabled, assetIds)

        if (!enabled) {
            walletRepository.clearAssets(assetIds)
        }
    }

    private fun constructMultiChainTokens(chains: List<Chain>): List<MultiChainToken> {
        val chainComparator = Chain.defaultComparator()
        val assetsWithChains = chains.sortedWith(chainComparator).flatMap { chain ->
            chain.assets.map { asset -> ChainWithAsset(chain, asset) }
        }

        return assetsWithChains.groupBy { (_, asset) -> asset.unifiedSymbol() }
            .map { (symbol, chainsWithAssets) ->
                val (_, firstAsset) = chainsWithAssets.first()

                MultiChainToken(
                    id = symbol,
                    symbol = symbol,
                    icon = firstAsset.iconUrl,
                    instances = chainsWithAssets.map { (chain, asset) ->
                        MultiChainToken.ChainTokenInstance(
                            chain = chain,
                            chainAssetId = asset.id,
                            isEnabled = asset.enabled
                        )
                    }
                )
            }
    }
}
