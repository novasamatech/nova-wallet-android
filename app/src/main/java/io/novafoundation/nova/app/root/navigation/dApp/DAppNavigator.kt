package io.novafoundation.nova.app.root.navigation.dApp

import io.novafoundation.nova.app.R
import io.novafoundation.nova.app.root.navigation.BaseNavigator
import io.novafoundation.nova.app.root.navigation.NavigationHolder
import io.novafoundation.nova.feature_dapp_impl.DAppRouter
import io.novafoundation.nova.feature_dapp_impl.presentation.addToFavourites.AddToFavouritesFragment
import io.novafoundation.nova.feature_dapp_impl.presentation.addToFavourites.AddToFavouritesPayload
import io.novafoundation.nova.feature_dapp_impl.presentation.browser.extrinsicDetails.DappExtrinsicDetailsFragment
import io.novafoundation.nova.feature_dapp_impl.presentation.browser.main.DAppBrowserFragment
import io.novafoundation.nova.feature_dapp_impl.presentation.search.DappSearchFragment
import io.novafoundation.nova.feature_dapp_impl.presentation.search.SearchPayload

class DAppNavigator(
    navigationHolder: NavigationHolder,
) : BaseNavigator(navigationHolder), DAppRouter {

    override fun openChangeAccount() = performNavigation(R.id.action_open_switch_wallet)

    override fun openDAppBrowser(initialUrl: String) = performNavigation(
        cases = arrayOf(
            R.id.mainFragment to R.id.action_mainFragment_to_dappBrowserGraph,
            R.id.dappSearchFragment to R.id.action_dappSearchFragment_to_dapp_browser_graph,
        ),
        args = DAppBrowserFragment.getBundle(initialUrl)
    )

    override fun openDappSearch() = performNavigation(
        actionId = R.id.action_mainFragment_to_dappSearchGraph,
        args = DappSearchFragment.getBundle(SearchPayload(initialUrl = null))
    )

    override fun openAddToFavourites(payload: AddToFavouritesPayload) = performNavigation(
        actionId = R.id.action_DAppBrowserFragment_to_addToFavouritesFragment,
        args = AddToFavouritesFragment.getBundle(payload)
    )

    override fun openExtrinsicDetails(extrinsicContent: String) = performNavigation(
        actionId = R.id.action_ConfirmSignExtrinsicFragment_to_extrinsicDetailsFragment,
        args = DappExtrinsicDetailsFragment.getBundle(extrinsicContent)
    )

    override fun openAuthorizedDApps() = performNavigation(
        actionId = R.id.action_mainFragment_to_authorizedDAppsFragment
    )
}
