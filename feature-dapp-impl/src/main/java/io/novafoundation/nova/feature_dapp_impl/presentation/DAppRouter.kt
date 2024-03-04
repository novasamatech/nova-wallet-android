package io.novafoundation.nova.feature_dapp_impl.presentation

import io.novafoundation.nova.common.navigation.ReturnableRouter
import io.novafoundation.nova.feature_dapp_impl.presentation.addToFavourites.AddToFavouritesPayload

interface DAppRouter : ReturnableRouter {

    fun openChangeAccount()

    fun openDAppBrowser(initialUrl: String)

    fun openDappSearch()

    fun openAddToFavourites(payload: AddToFavouritesPayload)

    fun openAuthorizedDApps()
}
