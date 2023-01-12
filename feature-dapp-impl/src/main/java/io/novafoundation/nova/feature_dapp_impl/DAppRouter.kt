package io.novafoundation.nova.feature_dapp_impl

import io.novafoundation.nova.feature_dapp_impl.presentation.addToFavourites.AddToFavouritesPayload
import io.novafoundation.nova.feature_dapp_impl.presentation.browser.options.DAppOptionsPayload

interface DAppRouter {

    fun openChangeAccount()

    fun openDAppBrowser(initialUrl: String)

    fun openDappSearch()

    fun openAddToFavourites(payload: AddToFavouritesPayload)

    fun openExtrinsicDetails(extrinsicContent: String)

    fun openAuthorizedDApps()

    fun back()
}
