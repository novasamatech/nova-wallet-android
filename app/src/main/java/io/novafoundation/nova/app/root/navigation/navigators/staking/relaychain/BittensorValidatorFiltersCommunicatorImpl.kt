package io.novafoundation.nova.app.root.navigation.navigators.staking.relaychain

import io.novafoundation.nova.app.R
import io.novafoundation.nova.app.root.navigation.NavStackInterScreenCommunicator
import io.novafoundation.nova.app.root.navigation.navigators.NavigationHoldersRegistry
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.validator.filters.BittensorValidatorFiltersCommunicator
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.validator.filters.BittensorValidatorFiltersCommunicator.Request
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.validator.filters.BittensorValidatorFiltersCommunicator.Response
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.validator.filters.BittensorValidatorFiltersFragment

class BittensorValidatorFiltersCommunicatorImpl(registry: NavigationHoldersRegistry) :
    BittensorValidatorFiltersCommunicator,
    NavStackInterScreenCommunicator<Request, Response>(
        registry,
        responseKey = "bittensor.validator.filters.response",
        requestKey = "bittensor.validator.filters.request"
    ) {

    override fun openRequest(request: Request) {
        super.openRequest(request)
        navController.navigate(R.id.action_open_bittensor_validator_filters, BittensorValidatorFiltersFragment.getBundle(request))
    }
}
