package io.novafoundation.nova.feature_assets.domain.tokens.manage

import io.novafoundation.nova.common.utils.isSubsetOf
import io.novafoundation.nova.feature_assets.domain.common.searchTokens
import io.novafoundation.nova.feature_assets.domain.tokens.AssetsDataCleaner
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.ChainAssetRepository
import io.novafoundation.nova.runtime.ext.defaultComparator
import io.novafoundation.nova.runtime.ext.fullId
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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

interface ManageTokenInteractor {

    fun multiChainTokensFlow(queryFlow: Flow<String>): Flow<List<MultiChainToken>>

    fun multiChainTokenFlow(id: String): Flow<MultiChainToken>

    suspend fun updateEnabledState(enabled: Boolean, assetIds: List<FullChainAssetId>)
}

class RealManageTokenInteractor(
    private val chainRegistry: ChainRegistry,
    private val chainAssetRepository: ChainAssetRepository,
    private val assetsDataCleaner: AssetsDataCleaner,
) : ManageTokenInteractor {

    private val changeTokensMutex = Mutex(false)

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
        changeTokensMutex.withLock {
            if (!enabled && canNotDisableAssets(assetIds)) {
                return@withLock
            }

            chainAssetRepository.setAssetsEnabled(enabled, assetIds)

            if (!enabled) {
                assetsDataCleaner.clearAssetsData(assetIds)
            }
        }
    }

    private suspend fun canNotDisableAssets(assetIds: List<FullChainAssetId>): Boolean {
        val enabledAssets = chainAssetRepository.getEnabledAssets()
            .map { it.fullId }
        return assetIds.containsAll(enabledAssets)
    }

    private fun constructMultiChainTokens(chains: List<Chain>): List<MultiChainToken> {
        val chainComparator = Chain.defaultComparator()
        val assetsWithChains = chains.sortedWith(chainComparator).flatMap { chain ->
            chain.assets.map { asset -> ChainWithAsset(chain, asset) }
        }

        val enabledAssets = assetsWithChains.filter { it.asset.enabled }
            .map { it.asset.fullId }

        return assetsWithChains.groupBy { (_, asset) -> asset.unifiedSymbol() }
            .map { (symbol, chainsWithAssets) ->
                val (_, firstAsset) = chainsWithAssets.first()
                val tokenAssets = chainsWithAssets.filter { it.asset.enabled }
                    .map { it.asset.fullId }
                val isLastTokenEnabled = enabledAssets.isSubsetOf(tokenAssets)
                val isLastAssetEnabled = isLastTokenEnabled && tokenAssets.size == 1

                MultiChainToken(
                    id = symbol,
                    symbol = symbol,
                    icon = firstAsset.iconUrl,
                    isSwitchable = !isLastTokenEnabled,
                    instances = chainsWithAssets.map { (chain, asset) ->
                        MultiChainToken.ChainTokenInstance(
                            chain = chain,
                            chainAssetId = asset.id,
                            isEnabled = asset.enabled,
                            isSwitchable = !asset.enabled || !isLastAssetEnabled
                        )
                    }
                )
            }
    }
}
