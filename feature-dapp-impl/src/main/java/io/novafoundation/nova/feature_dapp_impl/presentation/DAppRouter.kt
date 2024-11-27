package io.novafoundation.nova.feature_dapp_impl.presentation

import io.novafoundation.nova.common.navigation.ReturnableRouter
import io.novafoundation.nova.feature_dapp_api.presentation.addToFavorites.AddToFavouritesPayload
import io.novafoundation.nova.feature_dapp_api.presentation.browser.main.DAppBrowserPayload

interface DAppRouter : ReturnableRouter {

    fun openChangeAccount()

    fun openDAppBrowser(payload: DAppBrowserPayload)

    fun openDappSearch()

    fun finishDappSearch()

    fun openAddToFavourites(payload: AddToFavouritesPayload)

    fun openAuthorizedDApps()

    fun openTabs()

    fun closeTabsScreen()
}
