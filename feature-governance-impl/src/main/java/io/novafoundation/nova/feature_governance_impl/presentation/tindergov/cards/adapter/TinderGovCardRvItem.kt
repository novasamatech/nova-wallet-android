package io.novafoundation.nova.feature_governance_impl.presentation.tindergov.cards.adapter

import androidx.annotation.DrawableRes
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.ReferendumId
import io.novafoundation.nova.feature_wallet_api.presentation.model.AmountModel

data class TinderGovCardRvItem(
    val id: ReferendumId,
    val summary: CharSequence?,
    val requestedAmount: AmountModel?,
    @DrawableRes val backgroundRes: Int
)
