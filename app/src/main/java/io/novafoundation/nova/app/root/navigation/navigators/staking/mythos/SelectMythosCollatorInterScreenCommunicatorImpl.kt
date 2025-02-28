package io.novafoundation.nova.app.root.navigation.navigators.staking.mythos

import io.novafoundation.nova.app.R
import io.novafoundation.nova.app.root.navigation.NavStackInterScreenCommunicator
import io.novafoundation.nova.app.root.navigation.navigators.NavigationHoldersRegistry
import io.novafoundation.nova.feature_staking_impl.presentation.mythos.SelectMythosInterScreenCommunicator
import io.novafoundation.nova.feature_staking_impl.presentation.mythos.SelectMythosInterScreenCommunicator.Request
import io.novafoundation.nova.feature_staking_impl.presentation.mythos.start.selectCollator.model.MythosCollatorParcel

class SelectMythosCollatorInterScreenCommunicatorImpl(navigationHoldersRegistry: NavigationHoldersRegistry) :
    SelectMythosInterScreenCommunicator,
    NavStackInterScreenCommunicator<Request, MythosCollatorParcel>(navigationHoldersRegistry) {

    override fun respond(response: MythosCollatorParcel) {
        val responseEntry = navController.getBackStackEntry(R.id.startMythosStakingFragment)
        saveResultTo(responseEntry, response)
    }

    override fun openRequest(request: Request) {
        super.openRequest(request)

        navController.navigate(R.id.action_startMythosStakingFragment_to_selectMythosCollatorFragment)
    }
}
