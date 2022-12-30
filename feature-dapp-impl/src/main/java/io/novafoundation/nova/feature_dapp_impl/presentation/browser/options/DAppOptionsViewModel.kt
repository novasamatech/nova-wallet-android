package io.novafoundation.nova.feature_dapp_impl.presentation.browser.options

import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.mixin.actionAwaitable.ActionAwaitableMixin
import io.novafoundation.nova.common.mixin.actionAwaitable.confirmingAction
import io.novafoundation.nova.feature_dapp_impl.DAppRouter
import io.novafoundation.nova.feature_dapp_impl.domain.DappInteractor
import io.novafoundation.nova.feature_dapp_impl.presentation.addToFavourites.AddToFavouritesPayload
import io.novafoundation.nova.feature_dapp_impl.presentation.common.favourites.RemoveFavouritesPayload
import kotlinx.coroutines.launch

class DAppOptionsViewModel(
    val payload: DAppOptionsPayload,
    private val responder: DAppOptionsResponder,
    private val router: DAppRouter,
    private val interactor: DappInteractor,
    private val actionAwaitableMixinFactory: ActionAwaitableMixin.Factory
) : BaseViewModel() {

    val removeFromFavouritesConfirmation = actionAwaitableMixinFactory.confirmingAction<RemoveFavouritesPayload>()

    fun favoriteClick() {
        performFavoriteClick()
    }

    fun desktopModeClick() {
        responder.respond(DAppOptionsCommunicator.Response.DesktopModeClick)
        router.back()
    }

    private fun performFavoriteClick() = launch {
        if (payload.isFavorite) {
            removeFromFavouritesConfirmation.awaitAction(payload.currentPageTitle)

            interactor.removeDAppFromFavourites(payload.url)
            router.back()
        } else {
            val payload = AddToFavouritesPayload(
                url = payload.url,
                label = payload.currentPageTitle,
                iconLink = null
            )

            router.openAddToFavourites(payload)
        }
    }
}
