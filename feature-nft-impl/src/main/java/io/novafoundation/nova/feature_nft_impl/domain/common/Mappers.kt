package io.novafoundation.nova.feature_nft_impl.domain.common

import io.novafoundation.nova.feature_nft_api.data.model.NftDetails
import io.novafoundation.nova.feature_nft_impl.domain.nft.search.SendNftListItem

fun mapNftDetailsToListItem(nftDetails: NftDetails): SendNftListItem {
    return SendNftListItem(
        identifier = nftDetails.identifier,
        name = nftDetails.name,
        collectionName = mapNftCollectionForUi(nftDetails.collection?.name, nftDetails.collection?.id),
        media = nftDetails.media,
        chain = nftDetails.chain
    )
}

fun mapNftCollectionForUi(collectionName: String?, collectionId: String?): String {
    return collectionName ?: "NFT Collection ID: $collectionId"
}
