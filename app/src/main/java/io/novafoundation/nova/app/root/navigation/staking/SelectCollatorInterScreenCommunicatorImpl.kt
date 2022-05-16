package io.novafoundation.nova.app.root.navigation.staking

import io.novafoundation.nova.app.R
import io.novafoundation.nova.app.root.navigation.BaseInterScreenCommunicator
import io.novafoundation.nova.app.root.navigation.NavigationHolder
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.collator.common.SelectCollatorInterScreenCommunicator
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.collator.common.SelectCollatorInterScreenCommunicator.Request
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.collator.common.SelectCollatorInterScreenCommunicator.Response

class SelectCollatorInterScreenCommunicatorImpl(private val navigationHolder: NavigationHolder)
    : SelectCollatorInterScreenCommunicator,
    BaseInterScreenCommunicator<Request, Response>(navigationHolder) {

    override fun respond(response: Response) {
        val responseEntry = navController.getBackStackEntry(R.id.startParachainStakingFragment)

        saveResultTo(responseEntry, response)
    }

    override fun openRequest(request: Request) {
        navController.navigate(R.id.action_startParachainStakingFragment_to_selectCollatorFragment)
    }
}
