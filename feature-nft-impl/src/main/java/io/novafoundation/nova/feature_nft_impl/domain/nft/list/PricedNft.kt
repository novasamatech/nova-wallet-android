package io.novafoundation.nova.feature_nft_impl.domain.nft.list

import io.novafoundation.nova.feature_nft_api.data.model.Nft
import io.novafoundation.nova.feature_nft_api.data.model.NftDetails
import io.novafoundation.nova.feature_wallet_api.domain.model.Token

class PricedNft(
    val nft: Nft,
    val nftPriceToken: Token?
)
