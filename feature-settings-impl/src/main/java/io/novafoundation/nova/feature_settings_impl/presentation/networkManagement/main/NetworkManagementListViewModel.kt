package io.novafoundation.nova.feature_settings_impl.presentation.networkManagement.main

import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_settings_impl.SettingsRouter

class NetworkManagementListViewModel(
    private val router: SettingsRouter,
    private val resourceManager: ResourceManager,
) : BaseViewModel() {

    fun backClicked() {
        router.back()
    }
}
