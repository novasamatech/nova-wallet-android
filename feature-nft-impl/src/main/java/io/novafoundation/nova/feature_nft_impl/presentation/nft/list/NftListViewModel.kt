package io.novafoundation.nova.feature_nft_impl.presentation.nft.list

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.presentation.LoadingState
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.Event
import io.novafoundation.nova.common.utils.formatting.format
import io.novafoundation.nova.common.utils.inBackground
import io.novafoundation.nova.common.utils.mapList
import io.novafoundation.nova.feature_nft_api.data.model.Nft
import io.novafoundation.nova.feature_nft_impl.NftRouter
import io.novafoundation.nova.feature_nft_impl.domain.nft.list.NftListInteractor
import io.novafoundation.nova.feature_nft_impl.domain.nft.list.PricedNft
import io.novafoundation.nova.feature_nft_impl.presentation.nft.common.formatIssuance
import io.novafoundation.nova.feature_nft_impl.presentation.nft.common.formatNftPrice
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class NftListViewModel(
    private val router: NftRouter,
    private val resourceManager: ResourceManager,
    private val interactor: NftListInteractor,
) : BaseViewModel() {

    private val nftsFlow = interactor.userNftsFlow()
        .inBackground()
        .share()

    val nftCountFlow = nftsFlow.map { it.size.format() }
        .share()

    val nftListItemsFlow = nftsFlow.mapList(::mapNftToListItem)
        .inBackground()
        .share()

    private val _hideRefreshEvent = MutableLiveData<Event<Unit>>()
    val hideRefreshEvent: LiveData<Event<Unit>> = _hideRefreshEvent

    fun syncNfts() {
        viewModelScope.launch {
            interactor.syncNftsList()

            _hideRefreshEvent.value = Event(Unit)
        }
    }

    fun nftClicked(nftListItem: NftListItem) = launch {
        if (nftListItem.content is LoadingState.Loaded) {
            router.openNftDetails(nftListItem.identifier)
        }
    }

    fun loadableNftShown(nftListItem: NftListItem) = launch(Dispatchers.Default) {
        val pricedNft = nftsFlow.first().firstOrNull { it.nft.identifier == nftListItem.identifier }
            ?: return@launch

        interactor.fullSyncNft(pricedNft.nft)
    }

    private fun mapNftToListItem(pricedNft: PricedNft): NftListItem {
        val content = when (val details = pricedNft.nft.details) {
            Nft.Details.Loadable -> LoadingState.Loading()

            is Nft.Details.Loaded -> {
                val issuanceFormatted = resourceManager.formatIssuance(details.issuance)

                val price = resourceManager.formatNftPrice(details.price, pricedNft.nftPriceToken)

                LoadingState.Loaded(
                    NftListItem.Content(
                        issuance = issuanceFormatted,
                        title = details.name ?: pricedNft.nft.instanceId ?: pricedNft.nft.collectionId,
                        price = price,
                        media = details.media
                    )
                )
            }
        }

        return NftListItem(
            identifier = pricedNft.nft.identifier,
            content = content
        )
    }

    fun backClicked() {
        router.back()
    }
}
