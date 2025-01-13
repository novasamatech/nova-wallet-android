package io.novafoundation.nova.app.root.navigation.navigators.dApp

import androidx.navigation.fragment.FragmentNavigator
import io.novafoundation.nova.app.R
import io.novafoundation.nova.app.root.navigation.navigators.BaseNavigator
import io.novafoundation.nova.app.root.navigation.navigators.NavigationHoldersRegistry
import io.novafoundation.nova.feature_dapp_impl.presentation.DAppRouter
import io.novafoundation.nova.feature_dapp_impl.presentation.addToFavourites.AddToFavouritesFragment
import io.novafoundation.nova.feature_dapp_api.presentation.addToFavorites.AddToFavouritesPayload
import io.novafoundation.nova.feature_dapp_api.presentation.browser.main.DAppBrowserPayload
import io.novafoundation.nova.feature_dapp_impl.presentation.browser.main.DAppBrowserFragment
import io.novafoundation.nova.feature_dapp_impl.presentation.search.DappSearchFragment
import io.novafoundation.nova.feature_dapp_impl.presentation.search.SearchPayload

class DAppNavigator(
    navigationHoldersRegistry: NavigationHoldersRegistry,
) : BaseNavigator(navigationHoldersRegistry), DAppRouter {

    override fun openChangeAccount() {
        navigationBuilder().action(R.id.action_open_switch_wallet)
            .navigateInFirstAttachedContext()
    }

    override fun openDAppBrowser(payload: DAppBrowserPayload, extras: FragmentNavigator.Extras?) {
        // Close dapp browser if it is already opened
        // TODO it's better to provide new url to existing browser
        navigationBuilder().cases()
            .addCase(R.id.dappBrowserFragment, R.id.action_DAppBrowserFragment_to_DAppBrowserFragment)
            .addCase(R.id.dappSearchFragment, R.id.action_dappSearchFragment_to_dapp_browser_graph)
            .addCase(R.id.dappTabsFragment, R.id.action_dappTabsFragment_to_dapp_browser_graph)
            .setFallbackCase(R.id.action_open_dappBrowser)
            .setExtras(extras)
            .setArgs(DAppBrowserFragment.getBundle(payload))
            .navigateInRoot()
    }

    override fun openDappSearch() {
        openDappSearchWithCategory(categoryId = null)
    }

    override fun openDappSearchWithCategory(categoryId: String?) {
        navigationBuilder().cases()
            .addCase(R.id.dappTabsFragment, R.id.action_dappTabsFragment_to_dappSearch)
            .setFallbackCase(R.id.action_open_dappSearch)
            .setArgs(DappSearchFragment.getBundle(SearchPayload(initialUrl = null, SearchPayload.Request.OPEN_NEW_URL, preselectedCategoryId = categoryId)))
            .navigateInRoot()
    }

    override fun finishDappSearch() {
        navigationBuilder().action(R.id.action_finish_dapp_search)
            .navigateInRoot()
    }

    override fun openAddToFavourites(payload: AddToFavouritesPayload) {
        navigationBuilder().action(R.id.action_DAppBrowserFragment_to_addToFavouritesFragment)
            .setArgs(AddToFavouritesFragment.getBundle(payload))
            .navigateInFirstAttachedContext()
    }

    override fun openAuthorizedDApps() {
        navigationBuilder().action(R.id.action_mainFragment_to_authorizedDAppsFragment)
            .navigateInFirstAttachedContext()
    }

    override fun openTabs() {
        navigationBuilder().graph(R.id.dapp_tabs_graph)
            //.addCase(R.id.dappBrowserFragment, R.id.action_DAppBrowserFragment_to_browserTabsFragment)
            //.setFallbackCase(R.id.action_open_dappTabs)
            .navigateInRoot()
    }

    override fun closeTabsScreen() {
        navigationBuilder().action(R.id.action_finish_tabs_fragment)
            .navigateInRoot()
    }

    override fun openDAppFavorites() {
        navigationBuilder().action(R.id.action_open_dapp_favorites)
            .navigateInFirstAttachedContext()
    }
}
