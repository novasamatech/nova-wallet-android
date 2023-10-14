package io.novafoundation.nova.feature_nft_impl.presentation.nft.list

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.presentation.LoadingState
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.Event
import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.common.utils.formatting.format
import io.novafoundation.nova.common.utils.inBackground
import io.novafoundation.nova.common.utils.mapList
import io.novafoundation.nova.feature_nft_impl.NftRouter
import io.novafoundation.nova.feature_nft_impl.domain.nft.list.NftListInteractor
import io.novafoundation.nova.feature_nft_impl.presentation.nft.common.groupNftCards
import io.novafoundation.nova.feature_nft_impl.presentation.nft.common.mapNftToListItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class NftListViewModel(
    private val router: NftRouter,
    private val resourceManager: ResourceManager,
    private val interactor: NftListInteractor
) : BaseViewModel() {

    private val nftsFlow = interactor.userNftsFlow()
        .inBackground()

    val nftCountFlow = nftsFlow.map { it.size.format() }
        .share()
        .state(initialValue = "0")

    private val hiddenCollections = mutableSetOf<String>()
    private val hiddenCollectionChanged = MutableStateFlow("")

    val nftListItemsFlow = nftsFlow.combine(hiddenCollectionChanged) { nfts, _ -> nfts }
        .mapList { resourceManager.mapNftToListItem(it) }
        .map { groupNftCards(it, hiddenCollections) }
        .inBackground()
        .state(initialValue = emptyList())

    private val _hideRefreshEvent = MutableLiveData<Event<Unit>>()
    val hideRefreshEvent: LiveData<Event<Unit>> = _hideRefreshEvent


    init {
        subscribeUpdateNftListWhenOwnerChanged()
    }

    private fun subscribeUpdateNftListWhenOwnerChanged() {
        interactor.subscribeNftOwnerChanged()
            .inBackground()
            .launchIn(this)
    }

    fun syncNfts() {
        viewModelScope.launch {
            interactor.syncNftsList()

            _hideRefreshEvent.value = Event(Unit)
        }
    }

    fun nftClicked(nftListCard: NftListItem.NftListCard) = launch {
        if (nftListCard.content is LoadingState.Loaded) {
            router.openNftDetails(nftListCard.identifier)
        }
    }

    fun loadableNftShown(nftListCard: NftListItem.NftListCard) = launch(Dispatchers.Default) {
        val pricedNft = nftsFlow.first().firstOrNull { it.nft.identifier == nftListCard.identifier }
            ?: return@launch

        interactor.fullSyncNft(pricedNft.nft)
    }

    fun backClicked() {
        router.back()
    }

    fun onNftReceiveClick() {
        router.openNftReceiveFlowFragment()
    }

    fun onNftSendClick() {
        router.openNftSendFlowFragment()
    }

    fun toggleCollection(collection: String) {
        viewModelScope.launch {
            if (hiddenCollections.contains(collection)) {
                hiddenCollections.remove(collection)
                hiddenCollectionChanged.emit("expand:$collection")
            } else {
                hiddenCollections.add(collection)
                hiddenCollectionChanged.emit("collapse:$collection")
            }
        }
    }
}
