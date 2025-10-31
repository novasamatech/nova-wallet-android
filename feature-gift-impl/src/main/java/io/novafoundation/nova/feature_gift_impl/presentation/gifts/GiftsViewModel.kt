package io.novafoundation.nova.feature_gift_impl.presentation.gifts

import androidx.lifecycle.MutableLiveData
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.data.network.AppLinksProvider
import io.novafoundation.nova.common.mixin.api.Browserable
import io.novafoundation.nova.common.utils.Event
import io.novafoundation.nova.common.utils.mapList
import io.novafoundation.nova.feature_gift_impl.domain.GiftsInteractor
import io.novafoundation.nova.feature_gift_impl.presentation.GiftRouter

class GiftsViewModel(
    private val router: GiftRouter,
    private val appLinksProvider: AppLinksProvider,
    private val giftsInteractor: GiftsInteractor,
    private val giftsPayload: GiftsPayload
) : BaseViewModel(), Browserable {

    override val openBrowserEvent = MutableLiveData<Event<String>>()

    val gifts = giftsInteractor.observeGifts()
        .mapList { GiftRVItem(it.id) }

    fun back() {
        router.back()
    }

    fun learnMoreClicked() {
        openBrowserEvent.value = Event(appLinksProvider.wikiBase)
    }

    fun createGiftClicked() {
        when (giftsPayload) {
            GiftsPayload.AllAssets -> router.openGiftsFlow()
            is GiftsPayload.ByAsset -> router.openSelectGiftAmount(giftsPayload.assetPayload)
        }
    }
}
