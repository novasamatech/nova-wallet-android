package io.novafoundation.nova.app.root.navigation.navigators.dApp

import androidx.navigation.fragment.FragmentNavigator
import io.novafoundation.nova.app.R
import io.novafoundation.nova.app.root.navigation.holders.SplitScreenNavigationHolder
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
    splitScreenNavigationHolder: SplitScreenNavigationHolder,
    rootNavigationHolder: RootNavigationHolder,
) : BaseNavigator(splitScreenNavigationHolder, rootNavigationHolder), DAppRouter {

    override fun openChangeAccount() {
        navigationBuilder(R.id.action_open_switch_wallet)
            .perform()
    }

    override fun openDAppBrowser(payload: DAppBrowserPayload, extras: FragmentNavigator.Extras?) {
        navigationBuilder()
            .addCase(R.id.dappBrowserFragment, R.id.action_DAppBrowserFragment_to_DAppBrowserFragment)
            .addCase(R.id.dappSearchFragment, R.id.action_dappSearchFragment_to_dapp_browser_graph)
            .addCase(R.id.dappTabsFragment, R.id.action_dappTabsFragment_to_dapp_browser_graph)
            .setFallbackCase(R.id.action_open_dappBrowser)
            .setExtras(extras)
            .setArgs(DAppBrowserFragment.getBundle(payload))
            .perform()
    }

    override fun openDappSearch() {
        openDappSearchWithCategory(categoryId = null)
    }

    override fun openDappSearchWithCategory(categoryId: String?) {
        navigationBuilder(R.id.action_open_dappSearch)
            .setArgs(DappSearchFragment.getBundle(SearchPayload(initialUrl = null, SearchPayload.Request.OPEN_NEW_URL, preselectedCategoryId = categoryId)))
            .perform()
    }

    override fun finishDappSearch() {
        navigationBuilder(R.id.action_finish_dapp_search)
            .perform()
    }

    override fun openAddToFavourites(payload: AddToFavouritesPayload) {
        navigationBuilder(R.id.action_DAppBrowserFragment_to_addToFavouritesFragment)
            .setArgs(AddToFavouritesFragment.getBundle(payload))
            .perform()
    }

    override fun openAuthorizedDApps() {
        navigationBuilder(R.id.action_mainFragment_to_authorizedDAppsFragment)
            .perform()
    }

    override fun openTabs() {
        navigationBuilder()
            .addCase(R.id.dappBrowserFragment, R.id.action_DAppBrowserFragment_to_browserTabsFragment)
            .setFallbackCase(R.id.action_open_dappTabs)
            .perform()
    }

    override fun closeTabsScreen() {
        navigationBuilder(R.id.action_finish_tabs_fragment)
            .perform()
    }

    override fun openDAppFavorites() {
        navigationBuilder(R.id.action_open_dapp_favorites)
            .perform()
    }
}
