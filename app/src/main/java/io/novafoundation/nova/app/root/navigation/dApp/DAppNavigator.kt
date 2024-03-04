package io.novafoundation.nova.app.root.navigation.dApp

import io.novafoundation.nova.app.R
import io.novafoundation.nova.app.root.navigation.BaseNavigator
import io.novafoundation.nova.app.root.navigation.NavigationHolder
import io.novafoundation.nova.feature_dapp_impl.presentation.DAppRouter
import io.novafoundation.nova.feature_dapp_impl.presentation.addToFavourites.AddToFavouritesFragment
import io.novafoundation.nova.feature_dapp_api.presentation.addToFavorites.AddToFavouritesPayload
import io.novafoundation.nova.feature_dapp_impl.presentation.browser.main.DAppBrowserFragment
import io.novafoundation.nova.feature_dapp_impl.presentation.search.DappSearchFragment
import io.novafoundation.nova.feature_dapp_impl.presentation.search.SearchPayload

class DAppNavigator(
    private val navigationHolder: NavigationHolder,
) : BaseNavigator(navigationHolder), DAppRouter {

    override fun openChangeAccount() = performNavigation(R.id.action_open_switch_wallet)

    override fun openDAppBrowser(initialUrl: String) {
        // Close deapp browser if it is already opened
        // TODO it's better to provide new url to existing browser
        val currentDestination = navigationHolder.navController?.currentDestination

        val destinationId = when (currentDestination?.id) {
            R.id.DAppBrowserFragment -> R.id.action_DAppBrowserFragment_to_DAppBrowserFragment
            R.id.dappSearchFragment -> R.id.action_dappSearchFragment_to_dapp_browser_graph
            else -> R.id.action_dappBrowserGraph
        }
        performNavigation(destinationId, DAppBrowserFragment.getBundle(initialUrl))
    }

    override fun openDappSearch() = performNavigation(
        actionId = R.id.action_mainFragment_to_dappSearchGraph,
        args = DappSearchFragment.getBundle(SearchPayload(initialUrl = null))
    )

    override fun openAddToFavourites(payload: AddToFavouritesPayload) = performNavigation(
        actionId = R.id.action_DAppBrowserFragment_to_addToFavouritesFragment,
        args = AddToFavouritesFragment.getBundle(payload)
    )

    override fun openAuthorizedDApps() = performNavigation(
        actionId = R.id.action_mainFragment_to_authorizedDAppsFragment
    )
}
