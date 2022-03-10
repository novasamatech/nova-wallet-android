package io.novafoundation.nova.feature_nft_api

import io.novafoundation.nova.feature_nft_api.data.repository.NftRepository

interface NftFeatureApi {

    val nftRepository: NftRepository
}
