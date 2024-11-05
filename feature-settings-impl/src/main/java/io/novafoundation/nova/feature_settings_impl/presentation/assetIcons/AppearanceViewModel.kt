package io.novafoundation.nova.feature_settings_impl.presentation.assetIcons

import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.data.model.AssetIconMode
import io.novafoundation.nova.feature_settings_impl.SettingsRouter
import io.novafoundation.nova.feature_settings_impl.domain.AppearanceInteractor
import kotlinx.coroutines.flow.map

class AssetIconsStateModel(
    val whiteActive: Boolean,
    val coloredActive: Boolean
)

class AppearanceViewModel(
    private val interactor: AppearanceInteractor,
    private val router: SettingsRouter
) : BaseViewModel() {

    val assetIconsStateFlow = interactor.assetIconModeFlow()
        .map {
            AssetIconsStateModel(
                whiteActive = it == AssetIconMode.WHITE,
                coloredActive = it == AssetIconMode.COLORED
            )
        }

    fun selectWhiteIcon() {
        interactor.setIconMode(AssetIconMode.WHITE)
        router.returnToWallet()
    }

    fun selectColoredIcon() {
        interactor.setIconMode(AssetIconMode.COLORED)
        router.returnToWallet()
    }

    fun backClicked() {
        router.back()
    }
}
