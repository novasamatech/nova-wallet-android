package io.novafoundation.nova.feature_dapp_impl.presentation.favorites

import android.net.Uri
import io.novafoundation.nova.analytics.AnalyticsEvent
import io.novafoundation.nova.analytics.AnalyticsService
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.mixin.actionAwaitable.ActionAwaitableMixin
import io.novafoundation.nova.common.mixin.actionAwaitable.confirmingAction
import io.novafoundation.nova.feature_dapp_api.presentation.browser.main.DAppBrowserPayload
import io.novafoundation.nova.feature_dapp_impl.data.model.FavouriteDApp
import io.novafoundation.nova.feature_dapp_impl.domain.DappInteractor
import io.novafoundation.nova.feature_dapp_impl.presentation.DAppRouter
import io.novafoundation.nova.feature_dapp_impl.presentation.common.DappModel
import io.novafoundation.nova.feature_dapp_impl.presentation.common.favourites.RemoveFavouritesPayload
import io.novafoundation.nova.feature_dapp_impl.presentation.common.mapDAppModelToFavorite
import io.novafoundation.nova.feature_dapp_impl.presentation.common.mapFavoriteDappToDappModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class DAppFavoritesViewModel(
    private val router: DAppRouter,
    private val interactor: DappInteractor,
    private val actionAwaitableMixinFactory: ActionAwaitableMixin.Factory,
    private val analyticsService: AnalyticsService,
) : BaseViewModel() {

    val removeFavouriteConfirmationAwaitable = actionAwaitableMixinFactory.confirmingAction<RemoveFavouritesPayload>()

    private val favoriteDAppsFlow = MutableStateFlow<List<FavouriteDApp>>(emptyList())

    val favoriteDAppsUIFlow = favoriteDAppsFlow
        .map { dapps -> dapps.map { mapFavoriteDappToDappModel(it) } }
        .shareInBackground()

    init {
        launch {
            updateDApps()
        }
    }

    fun backClicked() {
        router.back()
    }

    fun openDApp(dapp: DappModel) {
        trackDappOpened(dapp.url)

        router.openDAppBrowser(DAppBrowserPayload.Address(dapp.url))
    }

    private fun trackDappOpened(url: String) = launch {
        val isKnownDapp = runCatching { interactor.getDAppInfo(url).metadata != null }.getOrDefault(false)

        analyticsService.track(
            AnalyticsEvent.DappOpened(
                dappHost = Uri.parse(url).host.orEmpty(),
                source = "favorites",
                isKnownDapp = isKnownDapp
            )
        )
    }

    fun onFavoriteClicked(dapp: DappModel) = launch {
        removeFavouriteConfirmationAwaitable.awaitAction(dapp.name)

        interactor.removeDAppFromFavourites(dapp.url)

        // Update list, since item was removed
        updateDApps()
    }

    fun changeDAppOrdering(newOrdering: List<DappModel>) = launch {
        val favoriteItems = newOrdering.mapIndexed { index, dappModel ->
            mapDAppModelToFavorite(dappModel, index)
        }

        interactor.updateFavoriteDapps(favoriteItems)
    }

    private suspend fun updateDApps() {
        val dapps = interactor.getFavoriteDApps()
        favoriteDAppsFlow.value = dapps
    }
}
