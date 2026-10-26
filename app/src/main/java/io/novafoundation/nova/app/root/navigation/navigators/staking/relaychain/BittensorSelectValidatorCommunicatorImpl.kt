package io.novafoundation.nova.app.root.navigation.navigators.staking.relaychain

import io.novafoundation.nova.app.R
import io.novafoundation.nova.app.root.navigation.NavStackInterScreenCommunicator
import io.novafoundation.nova.app.root.navigation.navigators.NavigationHoldersRegistry
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.subnet.BittensorSelectValidatorCommunicator
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.subnet.BittensorSelectValidatorFragment
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.subnet.BittensorSelectValidatorCommunicator.Request
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.subnet.BittensorSelectValidatorCommunicator.Response

class BittensorSelectValidatorCommunicatorImpl(registry: NavigationHoldersRegistry) :
    BittensorSelectValidatorCommunicator,
    NavStackInterScreenCommunicator<Request, Response>(
        registry,
        responseKey = "bittensor.validator.response",
        requestKey = "bittensor.validator.request"
    ) {

    override fun openRequest(request: Request) {
        super.openRequest(request)
        navController.navigate(R.id.action_open_bittensor_select_validator, BittensorSelectValidatorFragment.getBundle(request))
    }
}
