package io.novafoundation.nova.feature_dapp_impl.presentation.addToFavourites

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.utils.Event
import io.novafoundation.nova.common.utils.sendEvent
import io.novafoundation.nova.common.utils.singleReplaySharedFlow
import io.novafoundation.nova.feature_dapp_impl.presentation.DAppRouter
import io.novafoundation.nova.feature_dapp_api.presentation.addToFavorites.AddToFavouritesPayload
import io.novafoundation.nova.feature_dapp_impl.data.model.FavouriteDApp
import io.novafoundation.nova.feature_dapp_impl.domain.browser.addToFavourites.AddToFavouritesInteractor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class AddToFavouritesViewModel(
    private val interactor: AddToFavouritesInteractor,
    private val payload: AddToFavouritesPayload,
    private val router: DAppRouter,
) : BaseViewModel() {

    val urlFlow = MutableStateFlow(payload.url)
    val labelFlow = singleReplaySharedFlow<String>()

    val iconLink = singleReplaySharedFlow<String?>()

    private val _focusOnUrlFieldEvent = MutableLiveData<Event<Unit>>()
    val focusOnAddressFieldEvent: LiveData<Event<Unit>> = _focusOnUrlFieldEvent

    init {
        setInitialValues()
    }

    fun saveClicked() = launch {
        interactor.addToFavourites(urlFlow.value, labelFlow.first(), iconLink.first())

        router.back()
    }

    private fun setInitialValues() = launch {
        val resolvedDAppDisplay = interactor.resolveFavouriteDAppDisplay(url = payload.url, suppliedLabel = payload.label)

        labelFlow.emit(resolvedDAppDisplay.label)
        iconLink.emit(resolvedDAppDisplay.icon)

        _focusOnUrlFieldEvent.sendEvent()
    }

    fun backClicked() {
        router.back()
    }
}
