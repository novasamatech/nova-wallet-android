package io.novafoundation.nova.common.domain.interactor

import io.novafoundation.nova.common.data.model.AssetViewMode
import io.novafoundation.nova.common.data.repository.AssetsViewModeRepository
import kotlinx.coroutines.flow.Flow

interface AssetViewModeInteractor {

    fun getAssetViewMode(): AssetViewMode

    fun assetsViewModeFlow(): Flow<AssetViewMode>

    suspend fun setAssetsViewMode(assetsViewMode: AssetViewMode)
}

class RealAssetViewModeInteractor(
    private val assetsViewModeRepository: AssetsViewModeRepository
) : AssetViewModeInteractor {
    override fun getAssetViewMode(): AssetViewMode {
        return assetsViewModeRepository.getAssetViewMode()
    }

    override fun assetsViewModeFlow(): Flow<AssetViewMode> {
        return assetsViewModeRepository.assetsViewModeFlow()
    }

    override suspend fun setAssetsViewMode(assetsViewMode: AssetViewMode) {
        assetsViewModeRepository.setAssetsViewMode(assetsViewMode)
    }
}
