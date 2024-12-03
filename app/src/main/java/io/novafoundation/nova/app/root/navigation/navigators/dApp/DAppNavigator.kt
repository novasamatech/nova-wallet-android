package io.novafoundation.nova.app.root.navigation.navigators.dApp

import androidx.navigation.fragment.FragmentNavigator
import io.novafoundation.nova.app.R
import io.novafoundation.nova.app.root.navigation.holders.RootNavigationHolder
import io.novafoundation.nova.app.root.navigation.navigators.BaseNavigator
import io.novafoundation.nova.feature_dapp_impl.presentation.DAppRouter
import io.novafoundation.nova.feature_dapp_impl.presentation.addToFavourites.AddToFavouritesFragment
import io.novafoundation.nova.feature_dapp_api.presentation.addToFavorites.AddToFavouritesPayload
import io.novafoundation.nova.feature_dapp_api.presentation.browser.main.DAppBrowserPayload
import io.novafoundation.nova.feature_dapp_impl.presentation.browser.main.DAppBrowserFragment
import io.novafoundation.nova.feature_dapp_impl.presentation.search.DappSearchFragment
import io.novafoundation.nova.feature_dapp_impl.presentation.search.SearchPayload

class DAppNavigator(
    private val rootNavigationHolder: RootNavigationHolder,
) : BaseNavigator(rootNavigationHolder), DAppRouter {

    override fun openChangeAccount() = performNavigation(R.id.action_open_switch_wallet)

    override fun openDAppBrowser(payload: DAppBrowserPayload, extras: FragmentNavigator.Extras?) {
        // Close deapp browser if it is already opened
        // TODO it's better to provide new url to existing browser
        val currentDestination = rootNavigationHolder.navController?.currentDestination

        val destinationId = when (currentDestination?.id) {
            R.id.dappBrowserFragment -> R.id.action_DAppBrowserFragment_to_DAppBrowserFragment
            R.id.dappSearchFragment -> R.id.action_dappSearchFragment_to_dapp_browser_graph
            R.id.dappTabsFragment -> R.id.action_dappTabsFragment_to_dapp_browser_graph
            else -> R.id.action_open_dappBrowser
        }

        performNavigation(destinationId, DAppBrowserFragment.getBundle(payload), extras)
    }

    override fun openDappSearch() {
        openDappSearchWithCategory(categoryId = null)
    }

    override fun openDappSearchWithCategory(categoryId: String?) {
        performNavigation(
            actionId = R.id.action_open_dappSearch,
            args = DappSearchFragment.getBundle(SearchPayload(initialUrl = null, SearchPayload.Request.OPEN_NEW_URL, preselectedCategoryId = categoryId))
        )
    }

    override fun finishDappSearch() {
        performNavigation(R.id.action_finish_dapp_search)
    }

    override fun openAddToFavourites(payload: AddToFavouritesPayload) = performNavigation(
        actionId = R.id.action_DAppBrowserFragment_to_addToFavouritesFragment,
        args = AddToFavouritesFragment.getBundle(payload)
    )

    override fun openAuthorizedDApps() = performNavigation(
        actionId = R.id.action_mainFragment_to_authorizedDAppsFragment
    )

    override fun openTabs() {
        val currentDestination = rootNavigationHolder.navController?.currentDestination

        val destinationId = when (currentDestination?.id) {
            R.id.dappBrowserFragment -> R.id.action_DAppBrowserFragment_to_browserTabsFragment
            else -> R.id.action_open_dappTabs
        }

        performNavigation(destinationId)
    }

    override fun closeTabsScreen() = performNavigation(
        actionId = R.id.action_finish_tabs_fragment
    )
}
