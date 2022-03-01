package io.novafoundation.nova.feature_assets.domain.assets.list

import io.novafoundation.nova.feature_nft_api.data.model.Nft

class NftPreviews(
    val totalNftsCount: Int,
    val nftPreviews: List<Nft>
)
