package io.novafoundation.nova.feature_assets.domain.assets.search

import io.novafoundation.nova.common.utils.flowOfAll
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_assets.domain.common.searchTokens
import io.novafoundation.nova.feature_swap_api.domain.swap.SwapService
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.WalletRepository
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.ChainsById
import io.novafoundation.nova.runtime.multiNetwork.asset
import io.novafoundation.nova.runtime.multiNetwork.chain.model.FullChainAssetId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import io.novafoundation.nova.feature_wallet_api.domain.model.onlyVisible
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.AssetVisibilityUseCase

class AssetSearchUseCase(
    private val walletRepository: WalletRepository,
    private val accountRepository: AccountRepository,
    private val chainRegistry: ChainRegistry,
    private val swapService: SwapService,
    private val assetVisibilityUseCase: AssetVisibilityUseCase
) {

    /**
     * @param onlyVisible whether the wallet's own show/hide choices apply. Pickers that ask what
     * to *spend* respect them; pickers that ask what to *receive* must not, or a token could never
     * be chosen as a destination until it was already held.
     */
    fun filteredAssetFlow(filterFlow: Flow<AssetSearchFilter?>, onlyVisible: Boolean): Flow<List<Asset>> {
        val assetsFlow = accountRepository.selectedMetaAccountFlow()
            .flatMapLatest { walletRepository.syncedAssetsFlow(it.id) }

        val visibilityFlow = if (onlyVisible) assetVisibilityUseCase.visibilityFlow() else flowOf(null)

        return combine(assetsFlow, filterFlow, visibilityFlow) { assets, filter, visibility ->
            var result = assets

            if (visibility != null) result = result.onlyVisible(visibility)
            if (filter != null) result = result.filter { filter(it) }

            result
        }
    }

    fun filterAssetsByQuery(query: String, assets: List<Asset>, chainsById: ChainsById): List<Asset> {
        return assets.searchTokens(
            query = query,
            chainsById = chainsById,
            tokenSymbol = { it.token.configuration.symbol.value },
            relevantToChains = { asset, chainIds -> asset.token.configuration.chainId in chainIds }
        )
    }

    fun getAvailableSwapAssets(asset: FullChainAssetId?, coroutineScope: CoroutineScope): Flow<Set<FullChainAssetId>> {
        return flowOfAll {
            val chainAsset = asset?.let { chainRegistry.asset(it) }

            if (chainAsset == null) {
                swapService.assetsAvailableForSwap(coroutineScope)
            } else {
                swapService.availableSwapDirectionsFor(chainAsset, coroutineScope)
            }
        }
    }
}
