package io.novafoundation.nova.feature_assets.domain.manageTokens

import io.novafoundation.nova.feature_wallet_api.domain.interfaces.ChainAssetRepository
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.WalletRepository
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.FullChainAssetId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

interface ManageTokenInteractor {

    fun multiChainTokensFlow(): Flow<List<MultiChainToken>>

    fun multiChainTokenFlow(id: String): Flow<MultiChainToken>

    suspend fun updateEnabledState(enabled: Boolean, assetIds: List<FullChainAssetId>)
}

class RealManageTokenInteractor(
    private val chainRegistry: ChainRegistry,
    private val walletRepository: WalletRepository,
    private val chainAssetRepository: ChainAssetRepository,
) : ManageTokenInteractor {

    override fun multiChainTokensFlow(): Flow<List<MultiChainToken>> {
        return chainRegistry.currentChains.map { chains ->
            constructMultiChainTokens(chains)
        }
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
        val assetsWithChains = chains.flatMap { chain ->
            chain.assets.map { asset -> chain to asset }
        }

        return assetsWithChains.groupBy { (_, asset) -> asset.symbol }
            .map { (symbol, chainsWithAssets) ->
                val (_, firstAsset) = chainsWithAssets.first()

                MultiChainToken(
                    id = symbol,
                    symbol = symbol,
                    icon = firstAsset.iconUrl,
                    instances = chainsWithAssets.map { (chain, asset) ->
                        MultiChainToken.ChainTokenInstance(
                            chain = chain,
                            chainAssetId = asset.id
                        )
                    }
                )
            }
    }
}
