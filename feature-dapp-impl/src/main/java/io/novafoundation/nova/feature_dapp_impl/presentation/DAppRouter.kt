package io.novafoundation.nova.feature_dapp_impl.presentation

import androidx.navigation.fragment.FragmentNavigator
import io.novafoundation.nova.common.navigation.ReturnableRouter
import io.novafoundation.nova.feature_dapp_api.presentation.addToFavorites.AddToFavouritesPayload
import io.novafoundation.nova.feature_dapp_api.presentation.browser.main.DAppBrowserPayload

interface DAppRouter : ReturnableRouter {

    fun openChangeAccount()

    fun openDAppBrowser(payload: DAppBrowserPayload, extras: FragmentNavigator.Extras? = null)

    fun openDappSearch()

    fun openDappSearchWithCategory(categoryId: String?)

    fun finishDappSearch()

    fun openAddToFavourites(payload: AddToFavouritesPayload)

    fun openAuthorizedDApps()

    fun openTabs()

    fun closeTabsScreen()
}
