package io.novafoundation.nova.app.root.navigation.staking.parachain

import io.novafoundation.nova.app.R
import io.novafoundation.nova.app.root.navigation.NavStackInterScreenCommunicator
import io.novafoundation.nova.app.root.navigation.NavigationHolder
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.collator.settings.SelectCollatorSettingsFragment
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.collator.settings.SelectCollatorSettingsInterScreenCommunicator
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.collator.settings.SelectCollatorSettingsInterScreenCommunicator.Request
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.collator.settings.SelectCollatorSettingsInterScreenCommunicator.Response

class SelectCollatorSettingsInterScreenCommunicatorImpl(navigationHolder: NavigationHolder) :
    SelectCollatorSettingsInterScreenCommunicator,
    NavStackInterScreenCommunicator<Request, Response>(navigationHolder) {

    override fun openRequest(request: Request) {
        val bundle = SelectCollatorSettingsFragment.getBundle(request.currentConfig)
        navController.navigate(R.id.action_selectCollatorFragment_to_selectCollatorSettingsFragment, bundle)
    }
}
