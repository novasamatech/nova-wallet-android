package io.novafoundation.nova.feature_governance_impl.presentation.tindergov.cards.model

import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.ReferendumId
import io.novafoundation.nova.feature_wallet_api.presentation.model.AmountModel

class CardWithDetails(
    val id: ReferendumId,
    val summary: String,
    val amount: AmountModel?
) {

    override fun equals(other: Any?): Boolean {
        return other is CardWithDetails &&
            id == other.id &&
            summary == other.summary &&
            amount == other.amount
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + summary.hashCode()
        result = 31 * result + amount.hashCode()
        return result
    }
}
