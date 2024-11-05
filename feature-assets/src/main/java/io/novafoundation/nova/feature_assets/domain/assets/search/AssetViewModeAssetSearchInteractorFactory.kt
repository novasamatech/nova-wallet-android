package io.novafoundation.nova.feature_assets.domain.assets.search

import io.novafoundation.nova.common.data.model.AssetViewMode
import io.novafoundation.nova.common.data.repository.AssetsViewModeRepository
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry

class AssetViewModeAssetSearchInteractorFactory(
    private val assetViewModeRepository: AssetsViewModeRepository,
    private val assetSearchUseCase: AssetSearchUseCase,
    private val chainRegistry: ChainRegistry
) : AssetSearchInteractorFactory {

    override fun createByAssetViewMode(): AssetSearchInteractor {
        return when (assetViewModeRepository.getAssetViewMode()) {
            AssetViewMode.TOKENS -> ByTokensAssetSearchInteractor(assetSearchUseCase, chainRegistry)
            AssetViewMode.NETWORKS -> ByNetworkAssetSearchInteractor(assetSearchUseCase, chainRegistry)
        }
    }
}
