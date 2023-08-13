package io.novafoundation.nova.feature_nft_impl.presentation.nft.list

import io.novafoundation.nova.common.presentation.LoadingState
import io.novafoundation.nova.feature_wallet_api.presentation.model.AmountModel

sealed interface NftListItem {

    object Actions : NftListItem

    object Divider : NftListItem

    data class NftCollection(
        val name: String
    ) : NftListItem

    data class NftListCard(
        val content: LoadingState<Content>,
        val identifier: String,
    ) : NftListItem {

        data class Content(
            val collectionName: String?,
            val title: String,
            val price: AmountModel?,
            val media: String?,
            val wholeDetailsLoaded: Boolean
        )
    }
}
