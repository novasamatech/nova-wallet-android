package io.novafoundation.nova.feature_settings_impl.presentation.networkManagement.add.main

import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.feature_settings_impl.SettingsRouter

class AddNetworkMainViewModel(
    private val router: SettingsRouter
) : BaseViewModel() {

    fun backClicked() {
        router.back()
    }
}
