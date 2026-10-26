package io.novafoundation.nova.app.root.navigation.navigators.staking.relaychain

import io.novafoundation.nova.app.R
import io.novafoundation.nova.app.root.navigation.NavStackInterScreenCommunicator
import io.novafoundation.nova.app.root.navigation.navigators.NavigationHoldersRegistry
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.details.BittensorPortfolioReopenCommunicator
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.details.BittensorPortfolioReopenCommunicator.EmptyRequest
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.details.BittensorPortfolioReopenCommunicator.Response

class BittensorPortfolioReopenCommunicatorImpl(registry: NavigationHoldersRegistry) :
    BittensorPortfolioReopenCommunicator,
    NavStackInterScreenCommunicator<EmptyRequest, Response>(registry) {

    override fun respond(response: Response) {
        val responseEntry = navController.getBackStackEntry(R.id.bittensorPortfolioFragment)

        saveResultTo(responseEntry, response)
    }
}
