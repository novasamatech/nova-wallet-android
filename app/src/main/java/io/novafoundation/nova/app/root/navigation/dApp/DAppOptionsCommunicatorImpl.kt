package io.novafoundation.nova.app.root.navigation.dApp

import io.novafoundation.nova.app.R
import io.novafoundation.nova.app.root.navigation.BaseInterScreenCommunicator
import io.novafoundation.nova.app.root.navigation.NavigationHolder
import io.novafoundation.nova.feature_dapp_impl.presentation.browser.options.DAppOptionsBottomSheet
import io.novafoundation.nova.feature_dapp_impl.presentation.browser.options.DAppOptionsCommunicator
import io.novafoundation.nova.feature_dapp_impl.presentation.browser.options.DAppOptionsCommunicator.Response
import io.novafoundation.nova.feature_dapp_impl.presentation.browser.options.DAppOptionsPayload

class DAppOptionsCommunicatorImpl(navigationHolder: NavigationHolder) :
    BaseInterScreenCommunicator<DAppOptionsPayload, Response>(navigationHolder),
    DAppOptionsCommunicator {
    override fun openRequest(request: DAppOptionsPayload) {
        navController.navigate(R.id.action_DAppBrowserFragment_to_dappOptionsFragment, DAppOptionsBottomSheet.getBundle(request))
    }
}
