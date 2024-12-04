package io.novafoundation.nova.feature_dapp_impl.presentation.favorites

import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.mixin.actionAwaitable.ActionAwaitableMixin
import io.novafoundation.nova.common.mixin.actionAwaitable.confirmingAction
import io.novafoundation.nova.feature_dapp_api.presentation.browser.main.DAppBrowserPayload
import io.novafoundation.nova.feature_dapp_impl.data.model.mapFavoriteDappToDappModel
import io.novafoundation.nova.feature_dapp_impl.domain.DappInteractor
import io.novafoundation.nova.feature_dapp_impl.presentation.DAppRouter
import io.novafoundation.nova.feature_dapp_impl.presentation.common.DappModel
import io.novafoundation.nova.feature_dapp_impl.presentation.common.favourites.RemoveFavouritesPayload
import java.util.Collections
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class DAppFavoritesViewModel(
    private val router: DAppRouter,
    private val interactor: DappInteractor,
    private val actionAwaitableMixinFactory: ActionAwaitableMixin.Factory,
) : BaseViewModel() {

    val removeFavouriteConfirmationAwaitable = actionAwaitableMixinFactory.confirmingAction<RemoveFavouritesPayload>()

    private val favoriteDAppsFlow = interactor.observeFavoriteDApps()
        .shareInBackground()

    val favoriteDAppsUIFlow = favoriteDAppsFlow
        .map { dapps -> dapps.map { mapFavoriteDappToDappModel(it) } }
        .shareInBackground()

    fun openDApp(dapp: DappModel) {
        router.openDAppBrowser(DAppBrowserPayload.Address(dapp.url))
    }

    fun onFavoriteClicked(dapp: DappModel) = launch {
        removeFavouriteConfirmationAwaitable.awaitAction(dapp.name)

        interactor.removeDAppFromFavourites(dapp.url)
    }

    fun swapDAppOrdering(fromPosition: Int, toPosition: Int) = launch {
        val dapps = favoriteDAppsFlow.first().toMutableList()
        Collections.swap(dapps, fromPosition, toPosition)
        val changedOrderingDapps = dapps.mapIndexed { index, dApp ->
            dApp.copy(orderingIndex = index)
        }
        interactor.updateFavoriteDapps(changedOrderingDapps)
    }
}
