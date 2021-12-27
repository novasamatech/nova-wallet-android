package io.novafoundation.nova.app.root.navigation.dApp

import io.novafoundation.nova.app.R
import io.novafoundation.nova.app.root.navigation.BaseInterScreenCommunicator
import io.novafoundation.nova.app.root.navigation.NavigationHolder
import io.novafoundation.nova.feature_dapp_impl.presentation.browser.signExtrinsic.DAppSignExtrinsicCommunicator
import io.novafoundation.nova.feature_dapp_impl.presentation.browser.signExtrinsic.DAppSignExtrinsicCommunicator.Response
import io.novafoundation.nova.feature_dapp_impl.presentation.browser.signExtrinsic.DAppSignExtrinsicFragment
import io.novafoundation.nova.feature_dapp_impl.presentation.browser.signExtrinsic.DAppSignExtrinsicPayload

class DAppSignExtrinsicCommunicatorImpl(navigationHolder: NavigationHolder) :
    BaseInterScreenCommunicator<DAppSignExtrinsicPayload, Response>(navigationHolder),
    DAppSignExtrinsicCommunicator {

    override fun openRequest(request: DAppSignExtrinsicPayload) {
        navController.navigate(R.id.action_DAppBrowserFragment_to_ConfirmSignExtrinsicFragment, DAppSignExtrinsicFragment.getBundle(request))
    }
}
