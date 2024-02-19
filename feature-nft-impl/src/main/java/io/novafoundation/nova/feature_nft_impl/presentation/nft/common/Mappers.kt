package io.novafoundation.nova.feature_nft_impl.presentation.nft.common

import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.formatting.format
import io.novafoundation.nova.feature_nft_api.data.model.Nft
import io.novafoundation.nova.feature_nft_impl.R
import io.novafoundation.nova.feature_nft_impl.presentation.nft.common.model.NftPriceModel
import io.novafoundation.nova.feature_wallet_api.domain.model.Token
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

        is Nft.Issuance.Fungible -> {
            getString(
                R.string.nft_issuance_fungible_format,
                issuance.myAmount.format(),
                issuance.totalSupply.format()
            )
        }
    }
}

fun ResourceManager.formatNftPrice(price: Nft.Price?, priceToken: Token?): NftPriceModel? {
    if (price == null || priceToken == null) return null

    return when (price) {
        is Nft.Price.Fungible -> {
            val units = price.units.format()
            val amountModel = mapAmountToAmountModel(price.totalPrice, priceToken)

            NftPriceModel(
                amountInfo = getString(R.string.nft_fungile_price, units, amountModel.token),
                fiat = amountModel.fiat
            )
        }
        is Nft.Price.NonFungible -> {
            val amountModel = mapAmountToAmountModel(price.nftPrice, priceToken)

            NftPriceModel(
                amountInfo = amountModel.token,
                fiat = amountModel.fiat
            )
        }
    }
}
