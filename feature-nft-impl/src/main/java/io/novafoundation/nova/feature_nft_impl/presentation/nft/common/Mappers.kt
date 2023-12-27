package io.novafoundation.nova.feature_nft_impl.presentation.nft.common

import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.formatting.format
import io.novafoundation.nova.feature_nft_api.data.model.Nft
import io.novafoundation.nova.feature_nft_impl.R

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
