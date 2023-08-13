package io.novafoundation.nova.feature_nft_impl.presentation.nft.common

import io.novafoundation.nova.common.presentation.LoadingState
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

fun ResourceManager.mapNftToListItem(pricedNft: PricedNft): NftListItem {
    val content = when (val details = pricedNft.nft.details) {
        Nft.Details.Loadable -> LoadingState.Loading()

        is Nft.Details.Loaded -> {
            val amountModel = if (details.price != null && pricedNft.nftPriceToken != null) {
                mapAmountToAmountModel(details.price!!, pricedNft.nftPriceToken)
            } else {
                null
            }

            LoadingState.Loaded(
                NftListItem.Content(
                    title = mapNftNameForUi(details.name, pricedNft.nft.instanceId),
                    price = amountModel,
                    media = details.media,
                    collectionName = mapNftCollectionForUi(details.collectionName, pricedNft.nft.collectionId)
                )
            )
        }
    }

    return NftListItem(
        identifier = pricedNft.nft.identifier,
        content = content
    )
}
