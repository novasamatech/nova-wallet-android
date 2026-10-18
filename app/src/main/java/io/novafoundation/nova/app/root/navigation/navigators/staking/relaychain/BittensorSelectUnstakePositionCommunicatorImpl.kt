package io.novafoundation.nova.app.root.navigation.navigators.staking.relaychain

import io.novafoundation.nova.app.R
import io.novafoundation.nova.app.root.navigation.NavStackInterScreenCommunicator
import io.novafoundation.nova.app.root.navigation.navigators.NavigationHoldersRegistry
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.details.select.BittensorSelectUnstakePositionCommunicator
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.details.select.BittensorSelectUnstakePositionFragment
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.details.select.BittensorSelectUnstakePositionCommunicator.Request
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.details.select.BittensorSelectUnstakePositionCommunicator.Response

class BittensorSelectUnstakePositionCommunicatorImpl(registry: NavigationHoldersRegistry) :
    BittensorSelectUnstakePositionCommunicator,
    NavStackInterScreenCommunicator<Request, Response>(
        registry,
        responseKey = "bittensor.unstake.position.response",
        requestKey = "bittensor.unstake.position.request"
    ) {

    override fun openRequest(request: Request) {
        super.openRequest(request)
        navController.navigate(R.id.action_open_bittensor_select_unstake_position, BittensorSelectUnstakePositionFragment.getBundle(request))
    }
}
