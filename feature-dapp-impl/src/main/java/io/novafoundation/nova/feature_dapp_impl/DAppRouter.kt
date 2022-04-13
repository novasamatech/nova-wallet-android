package io.novafoundation.nova.feature_dapp_impl

import io.novafoundation.nova.feature_dapp_impl.presentation.addToFavourites.AddToFavouritesPayload

interface DAppRouter {

    fun openChangeAccount()

    fun openDAppBrowser(initialUrl: String)

    fun openDappSearch()

    fun openAddToFavourites(payload: AddToFavouritesPayload)

    fun openExtrinsicDetails(extrinsicContent: String)

    fun back()
}
