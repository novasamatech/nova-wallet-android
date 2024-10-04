package io.novafoundation.nova.feature_governance_impl.presentation.tindergov.cards.model

import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.ReferendumId
import io.novafoundation.nova.feature_wallet_api.presentation.model.AmountModel

data class CardWithDetails(
    val id: ReferendumId,
    val summary: String,
    val amount: AmountModel?
)
