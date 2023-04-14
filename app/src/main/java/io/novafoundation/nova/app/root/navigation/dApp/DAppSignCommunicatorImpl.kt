package io.novafoundation.nova.app.root.navigation.dApp

import io.novafoundation.nova.app.R
import io.novafoundation.nova.app.root.navigation.BaseInterScreenCommunicator
import io.novafoundation.nova.app.root.navigation.NavigationHolder
import io.novafoundation.nova.feature_dapp_impl.presentation.browser.signExtrinsic.DAppSignCommunicator
import io.novafoundation.nova.feature_dapp_impl.presentation.browser.signExtrinsic.DAppSignCommunicator.Response
import io.novafoundation.nova.feature_dapp_impl.presentation.browser.signExtrinsic.DAppSignExtrinsicFragment
import io.novafoundation.nova.feature_dapp_impl.presentation.browser.signExtrinsic.DAppSignPayload

class DAppSignCommunicatorImpl(navigationHolder: NavigationHolder) :
    BaseInterScreenCommunicator<DAppSignPayload, Response>(navigationHolder),
    DAppSignCommunicator {

    override fun openRequest(request: DAppSignPayload) {
        navController.navigate(R.id.action_open_externalSignGraph, DAppSignExtrinsicFragment.getBundle(request))
    }
}
