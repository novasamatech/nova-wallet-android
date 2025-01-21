package io.novafoundation.nova.app.root.navigation.navigators.staking.mythos

import io.novafoundation.nova.app.R
import io.novafoundation.nova.app.root.navigation.NavStackInterScreenCommunicator
import io.novafoundation.nova.app.root.navigation.navigators.NavigationHoldersRegistry
import io.novafoundation.nova.feature_staking_impl.presentation.mythos.start.selectCollatorSettings.SelectMythCollatorSettingsFragment
import io.novafoundation.nova.feature_staking_impl.presentation.mythos.start.selectCollatorSettings.SelectMythCollatorSettingsInterScreenCommunicator
import io.novafoundation.nova.feature_staking_impl.presentation.mythos.start.selectCollatorSettings.model.MythCollatorRecommendationConfigParcel
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.collator.settings.SelectCollatorSettingsFragment
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.collator.settings.SelectCollatorSettingsInterScreenCommunicator
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.collator.settings.SelectCollatorSettingsInterScreenCommunicator.Request
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.collator.settings.SelectCollatorSettingsInterScreenCommunicator.Response

class SelectMythCollatorSettingsInterScreenCommunicatorImpl(navigationHoldersRegistry: NavigationHoldersRegistry) :
    SelectMythCollatorSettingsInterScreenCommunicator,
    NavStackInterScreenCommunicator<MythCollatorRecommendationConfigParcel, MythCollatorRecommendationConfigParcel>(navigationHoldersRegistry) {

    override fun openRequest(request: MythCollatorRecommendationConfigParcel) {
        super.openRequest(request)

        val bundle = SelectMythCollatorSettingsFragment.getBundle(request)
        navController.navigate(R.id.action_selectMythosCollatorFragment_to_selectMythCollatorSettingsFragment, bundle)
    }
}
