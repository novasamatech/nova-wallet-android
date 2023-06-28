package io.novafoundation.nova.app.root.navigation.staking.parachain

import io.novafoundation.nova.app.R
import io.novafoundation.nova.app.root.navigation.NavStackInterScreenCommunicator
import io.novafoundation.nova.app.root.navigation.NavigationHolder
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.collator.common.SelectCollatorInterScreenCommunicator
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.collator.common.SelectCollatorInterScreenCommunicator.Request
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.collator.common.SelectCollatorInterScreenCommunicator.Response

class SelectCollatorInterScreenCommunicatorImpl(navigationHolder: NavigationHolder) :
    SelectCollatorInterScreenCommunicator,
    NavStackInterScreenCommunicator<Request, Response>(navigationHolder) {

    override fun respond(response: Response) {
        val responseEntry = navController.getBackStackEntry(R.id.startParachainStakingFragment)

        saveResultTo(responseEntry, response)
    }

    override fun openRequest(request: Request) {
        super.openRequest(request)

        navController.navigate(R.id.action_startParachainStakingFragment_to_selectCollatorFragment)
    }
}
