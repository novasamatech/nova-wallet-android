package io.novafoundation.nova.feature_settings_impl.domain

import io.novafoundation.nova.common.data.model.AssetIconMode
import io.novafoundation.nova.common.data.repository.AssetsIconModeRepository
import kotlinx.coroutines.flow.Flow

interface AppearanceInteractor {

    fun assetIconModeFlow(): Flow<AssetIconMode>

    fun setIconMode(iconMode: AssetIconMode)
}

class RealAppearanceInteractor(
    private val assetsIconModeRepository: AssetsIconModeRepository
) : AppearanceInteractor {

    override fun assetIconModeFlow() = assetsIconModeRepository.assetsIconModeFlow()

    override fun setIconMode(iconMode: AssetIconMode) {
        assetsIconModeRepository.setAssetsIconMode(iconMode)
    }
}
