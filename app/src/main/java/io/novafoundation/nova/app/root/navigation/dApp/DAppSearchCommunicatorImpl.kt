package io.novafoundation.nova.app.root.navigation.dApp

import io.novafoundation.nova.app.R
import io.novafoundation.nova.app.root.navigation.BaseInterScreenCommunicator
import io.novafoundation.nova.app.root.navigation.NavigationHolder
import io.novafoundation.nova.feature_dapp_impl.presentation.search.DAppSearchCommunicator
import io.novafoundation.nova.feature_dapp_impl.presentation.search.DAppSearchCommunicator.Response
import io.novafoundation.nova.feature_dapp_impl.presentation.search.DappSearchFragment
import io.novafoundation.nova.feature_dapp_impl.presentation.search.SearchPayload

class DAppSearchCommunicatorImpl(navigationHolder: NavigationHolder) :
    BaseInterScreenCommunicator<SearchPayload, Response>(navigationHolder),
    DAppSearchCommunicator {
    override fun openRequest(request: SearchPayload) {
        navController.navigate(R.id.action_DAppBrowserFragment_to_dappSearchFragment, DappSearchFragment.getBundle(request))
    }
}
