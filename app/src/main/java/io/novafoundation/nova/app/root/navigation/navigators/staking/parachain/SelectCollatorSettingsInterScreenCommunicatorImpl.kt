package io.novafoundation.nova.app.root.navigation.navigators.staking.parachain

import io.novafoundation.nova.app.R
import io.novafoundation.nova.app.root.navigation.NavStackInterScreenCommunicator
import io.novafoundation.nova.app.root.navigation.navigators.NavigationHoldersRegistry
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.collator.settings.SelectCollatorSettingsFragment
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.collator.settings.SelectCollatorSettingsInterScreenCommunicator
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.collator.settings.SelectCollatorSettingsInterScreenCommunicator.Request
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.collator.settings.SelectCollatorSettingsInterScreenCommunicator.Response

class SelectCollatorSettingsInterScreenCommunicatorImpl(navigationHoldersRegistry: NavigationHoldersRegistry) :
    SelectCollatorSettingsInterScreenCommunicator,
    NavStackInterScreenCommunicator<Request, Response>(navigationHoldersRegistry) {

    override fun openRequest(request: Request) {
        val bundle = SelectCollatorSettingsFragment.getBundle(request.currentConfig)
        navController.navigate(R.id.action_selectCollatorFragment_to_selectCollatorSettingsFragment, bundle)
    }
}
