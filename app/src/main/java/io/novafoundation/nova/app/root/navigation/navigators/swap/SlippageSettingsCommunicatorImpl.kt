package io.novafoundation.nova.app.root.navigation.navigators.swap

import io.novafoundation.nova.app.R
import io.novafoundation.nova.app.root.navigation.NavStackInterScreenCommunicator
import io.novafoundation.nova.app.root.navigation.navigators.NavigationHoldersRegistry
import io.novafoundation.nova.feature_swap_api.presentation.model.SlippageSettingsPayload
import io.novafoundation.nova.feature_swap_api.presentation.navigation.SlippageSettingsCommunicator
import io.novafoundation.nova.feature_swap_api.presentation.navigation.SlippageSettingsCommunicator.Response
import io.novafoundation.nova.feature_swap_impl.presentation.options.SlippageSettingsBottomSheet

class SlippageSettingsCommunicatorImpl(registry: NavigationHoldersRegistry) :
    SlippageSettingsCommunicator,
    NavStackInterScreenCommunicator<SlippageSettingsPayload, Response>(
        registry,
        responseKey = "slippage.settings.response",
        requestKey = "slippage.settings.request"
    ) {

    override fun openRequest(request: SlippageSettingsPayload) {
        super.openRequest(request)
        navController.navigate(R.id.action_open_slippage_settings, SlippageSettingsBottomSheet.getBundle(request))
    }
}
