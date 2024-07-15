package io.novafoundation.nova.feature_settings_impl.presentation.networkManagement.main

import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.feature_settings_impl.SettingsRouter

class NetworkManagementListViewModel(
    private val router: SettingsRouter
) : BaseViewModel() {

    fun backClicked() {
        router.back()
    }

    fun addNetworkClicked() {
        router.addNetwork()
    }
}
