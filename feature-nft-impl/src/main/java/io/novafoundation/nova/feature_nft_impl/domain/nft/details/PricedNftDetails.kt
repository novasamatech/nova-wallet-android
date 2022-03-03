package io.novafoundation.nova.feature_nft_impl.domain.nft.details

import io.novafoundation.nova.feature_nft_api.data.model.NftDetails
import io.novafoundation.nova.feature_wallet_api.domain.model.Price

class PricedNftDetails(
    val nftDetails: NftDetails,
    val price: Price?
)
