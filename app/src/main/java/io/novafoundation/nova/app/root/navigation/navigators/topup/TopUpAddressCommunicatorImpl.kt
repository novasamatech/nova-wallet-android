package io.novafoundation.nova.app.root.navigation.navigators.topup

import io.novafoundation.nova.app.R
import io.novafoundation.nova.app.root.navigation.NavStackInterScreenCommunicator
import io.novafoundation.nova.app.root.navigation.navigators.NavigationHoldersRegistry
import io.novafoundation.nova.feature_assets.presentation.topup.TopUpAddressCommunicator
import io.novafoundation.nova.feature_assets.presentation.topup.TopUpAddressFragment
import io.novafoundation.nova.feature_assets.presentation.topup.TopUpAddressPayload
import io.novafoundation.nova.feature_assets.presentation.topup.TopUpAddressResponder

class TopUpAddressCommunicatorImpl(navigationHoldersRegistry: NavigationHoldersRegistry) :
    NavStackInterScreenCommunicator<TopUpAddressPayload, TopUpAddressResponder.Response>(navigationHoldersRegistry),
    TopUpAddressCommunicator {

    override fun openRequest(request: TopUpAddressPayload) {
        super.openRequest(request)

        navigationBuilder().action(R.id.action_open_topUpAddress)
            .setArgs(TopUpAddressFragment.createPayload(request))
            .navigateInFirstAttachedContext()
    }
}
