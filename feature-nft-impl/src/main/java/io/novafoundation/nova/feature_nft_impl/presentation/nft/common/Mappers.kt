package io.novafoundation.nova.feature_nft_impl.presentation.nft.common

import io.novafoundation.nova.common.presentation.LoadingState
import io.novafoundation.nova.common.presentation.dataOrNull
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.formatting.format
import io.novafoundation.nova.feature_nft_api.data.model.Nft
import io.novafoundation.nova.feature_nft_impl.R
import io.novafoundation.nova.feature_nft_impl.domain.common.mapNftCollectionForUi
import io.novafoundation.nova.feature_nft_impl.domain.common.mapNftNameForUi
import io.novafoundation.nova.feature_nft_impl.domain.nft.list.PricedNft
import io.novafoundation.nova.feature_nft_impl.presentation.nft.list.NftListItem
import io.novafoundation.nova.feature_wallet_api.presentation.model.mapAmountToAmountModel

fun ResourceManager.formatIssuance(issuance: Nft.Issuance): String {
    return when (issuance) {
        is Nft.Issuance.Unlimited -> getString(R.string.nft_issuance_unlimited)

        is Nft.Issuance.Limited -> {
            getString(
                R.string.nft_issuance_limited_format,
                issuance.edition.format(),
                issuance.max.format()
            )
        }
    }
}

fun ResourceManager.mapNftToListItem(pricedNft: PricedNft): NftListItem.NftListCard {
    val content = when (val details = pricedNft.nft.details) {
        Nft.Details.Loadable -> LoadingState.Loading()

        is Nft.Details.Loaded -> {
            val amountModel = if (details.price != null && pricedNft.nftPriceToken != null) {
                mapAmountToAmountModel(details.price!!, pricedNft.nftPriceToken)
            } else {
                null
            }

            LoadingState.Loaded(
                NftListItem.NftListCard.Content(
                    title = mapNftNameForUi(details.name, pricedNft.nft.instanceId),
                    price = amountModel,
                    media = details.media,
                    collectionName = mapNftCollectionForUi(details.collectionName, pricedNft.nft.collectionId),
                    wholeDetailsLoaded = pricedNft.nft.wholeDetailsLoaded
                )
            )
        }
    }

    return NftListItem.NftListCard(
        identifier = pricedNft.nft.identifier,
        content = content
    )
}

fun groupNftCards(nftCards: List<NftListItem.NftListCard>): List<NftListItem> {
    val groupedNfts = nftCards.groupBy { it.content.dataOrNull?.collectionName }
    val collections = groupedNfts.keys.sortedBy { it }
    val nftListItems = mutableListOf<NftListItem>()
    val singleNftsInCollection = mutableListOf<NftListItem>()
    collections.forEach {
        val nftsPerCollection = groupedNfts[it].orEmpty()
        if (nftsPerCollection.size > 1 && it != null) {
            nftListItems.add(NftListItem.NftCollection(it))
            nftListItems.addAll(
                nftsPerCollection.map { nftCard ->
                    val content = nftCard.content.dataOrNull
                    if (content is NftListItem.NftListCard.Content) {
                        nftCard.copy(
                            content = LoadingState.Loaded(
                                content.copy(collectionName = null)
                            )
                        )
                    } else {
                        nftCard
                    }
                }
            )
        } else if (nftsPerCollection.size == 1 || it == null) {
            singleNftsInCollection.addAll(nftsPerCollection)
        }
    }
    return nftListItems + if (singleNftsInCollection.isEmpty()) {
        emptyList()
    } else if (nftListItems.isNotEmpty()) {
        listOf(NftListItem.Divider) + singleNftsInCollection
    } else {
        singleNftsInCollection
    }
}
