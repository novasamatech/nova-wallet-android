package io.novafoundation.nova.feature_assets.domain.assets.search

import io.novafoundation.nova.common.data.model.AssetViewMode
import io.novafoundation.nova.common.data.repository.AssetsViewModeRepository
import io.novafoundation.nova.feature_buy_api.presentation.trade.TradeTokenRegistry
import io.novafoundation.nova.feature_gift_api.domain.AvailableGiftAssetsUseCase
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry

class AssetViewModeAssetSearchInteractorFactory(
    private val assetViewModeRepository: AssetsViewModeRepository,
    private val assetSearchUseCase: AssetSearchUseCase,
    private val chainRegistry: ChainRegistry,
    private val tradeTokenRegistry: TradeTokenRegistry,
    private val availableGiftAssetsUseCase: AvailableGiftAssetsUseCase
) : AssetSearchInteractorFactory {

    override fun createByAssetViewMode(): AssetSearchInteractor {
        return when (assetViewModeRepository.getAssetViewMode()) {
            AssetViewMode.TOKENS -> ByTokensAssetSearchInteractor(assetSearchUseCase, chainRegistry, tradeTokenRegistry, availableGiftAssetsUseCase)
            AssetViewMode.NETWORKS -> ByNetworkAssetSearchInteractor(assetSearchUseCase, chainRegistry, tradeTokenRegistry, availableGiftAssetsUseCase)
        }
    }
}
