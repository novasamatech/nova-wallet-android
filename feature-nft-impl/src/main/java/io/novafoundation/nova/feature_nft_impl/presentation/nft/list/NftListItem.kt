package io.novafoundation.nova.feature_nft_impl.presentation.nft.list

import io.novafoundation.nova.common.presentation.LoadingState
import io.novafoundation.nova.feature_wallet_api.presentation.model.AmountModel

data class NftListItem(
    val content: LoadingState<Content>,
    val identifier: String,
) {

    data class Content(
        val issuance: String,
        val title: String,
        val price: AmountModel?,
        val media: String?,
    )
}
