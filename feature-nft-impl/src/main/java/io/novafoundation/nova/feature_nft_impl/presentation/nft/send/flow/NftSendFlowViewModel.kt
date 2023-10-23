package io.novafoundation.nova.feature_assets.presentation.send.flow

import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.list.toListWithHeaders
import io.novafoundation.nova.feature_account_api.data.mappers.mapChainToUi
import io.novafoundation.nova.feature_nft_impl.NftRouter
import io.novafoundation.nova.feature_nft_impl.domain.nft.search.NftSearchInteractor
import io.novafoundation.nova.feature_nft_impl.domain.nft.search.SendNftListItem
import io.novafoundation.nova.feature_nft_impl.presentation.NftPayload
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

class NftSendFlowViewModel(
    private val router: NftRouter,
    private val interactor: NftSearchInteractor
) : BaseViewModel() {

    val query = MutableStateFlow("")

    val searchResults = searchNftsFlow()
        .distinctUntilChanged()
        .shareInBackground()

    fun backClicked() {
        router.back()
    }

    fun nftClicked(item: SendNftListItem) {
        router.openInputAddressNftFromSendFlow(
            nftPayload = NftPayload(
                chainId = item.chain.id,
                identifier = item.identifier
            )
        )
    }

    private fun searchNftsFlow(): Flow<List<Any>> {
        return interactor.sendNftSearch(query)
            .map { it.mapKeys { mapChainToUi(it.key) } }
            .map { it.toListWithHeaders() }
    }
}
