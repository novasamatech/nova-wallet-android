package io.novafoundation.nova.app.root.navigation.navigators.dApp

import io.novafoundation.nova.app.R
import io.novafoundation.nova.app.root.navigation.NavStackInterScreenCommunicator
import io.novafoundation.nova.app.root.navigation.holders.RootNavigationHolder
import io.novafoundation.nova.feature_dapp_impl.presentation.search.DAppSearchCommunicator
import io.novafoundation.nova.feature_dapp_impl.presentation.search.DAppSearchCommunicator.Response
import io.novafoundation.nova.feature_dapp_impl.presentation.search.DappSearchFragment
import io.novafoundation.nova.feature_dapp_impl.presentation.search.SearchPayload

class DAppSearchCommunicatorImpl(navigationHolder: RootNavigationHolder) :
    NavStackInterScreenCommunicator<SearchPayload, Response>(navigationHolder),
    DAppSearchCommunicator {

    override fun openRequest(request: SearchPayload) {
        super.openRequest(request)

        navController.navigate(R.id.action_open_dappSearch_from_browser, DappSearchFragment.getBundle(request))
    }
}
