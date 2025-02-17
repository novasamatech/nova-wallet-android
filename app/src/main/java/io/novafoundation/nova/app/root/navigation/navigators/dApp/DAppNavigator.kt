package io.novafoundation.nova.app.root.navigation.navigators.dApp

import androidx.navigation.NavOptions
import androidx.navigation.fragment.FragmentNavigator
import io.novafoundation.nova.app.R
import io.novafoundation.nova.app.root.navigation.navigators.BaseNavigator
import io.novafoundation.nova.app.root.navigation.navigators.NavigationHoldersRegistry
import io.novafoundation.nova.app.root.navigation.navigators.builder.NavigationBuilder
import io.novafoundation.nova.feature_dapp_impl.presentation.DAppRouter
import io.novafoundation.nova.feature_dapp_impl.presentation.addToFavourites.AddToFavouritesFragment
import io.novafoundation.nova.feature_dapp_api.presentation.addToFavorites.AddToFavouritesPayload
import io.novafoundation.nova.feature_dapp_api.presentation.browser.main.DAppBrowserPayload
import io.novafoundation.nova.feature_dapp_impl.presentation.browser.main.DAppBrowserFragment
import io.novafoundation.nova.feature_dapp_impl.presentation.search.DappSearchFragment
import io.novafoundation.nova.feature_dapp_impl.presentation.search.SearchPayload
import java.math.BigInteger

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
        navigationBuilder().graph(R.id.dapp_browser_graph)
            .setDappAnimations()
            .setExtras(extras)
            .setArgs(DAppBrowserFragment.getBundle(payload))
            .navigateInRoot()
    }

    override fun openDappSearch() {
        openDappSearchWithCategory(categoryId = null)
    }

    override fun openDappSearchWithCategory(categoryId: String?) {
        navigationBuilder().graph(R.id.dapp_search_graph)
            .setDappAnimations()
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
            .setDappAnimations()
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

    private fun NavigationBuilder.setDappAnimations(): NavigationBuilder {
        val currentDestinationId = currentDestination?.id

        // For this currentDestinations we will use default animation. And for other - slide_in, slide_out
        val dappDestinations = listOf(
            R.id.dappSearchFragment,
            R.id.dappBrowserFragment,
            R.id.dappTabsFragment
        )

        val navOptionsBuilder = if (currentDestinationId in dappDestinations) {
            // Only slide out animation
            NavOptions.Builder()
                .setEnterAnim(R.anim.fragment_open_enter)
                .setExitAnim(R.anim.fragment_open_exit)
                .setPopEnterAnim(R.anim.fragment_close_enter)
                .setPopExitAnim(R.anim.fragment_slide_out)
                .setPopUpTo(R.id.splitScreenFragment, false)
        } else {
            // Slide in/out animations
            NavOptions.Builder()
                .setEnterAnim(R.anim.fragment_slide_in)
                .setExitAnim(R.anim.fragment_open_exit)
                .setPopEnterAnim(R.anim.fragment_close_enter)
                .setPopExitAnim(R.anim.fragment_slide_out)
                .setPopUpTo(R.id.splitScreenFragment, false)
        }

        return setNavOptions(navOptionsBuilder.build())
    }
}
