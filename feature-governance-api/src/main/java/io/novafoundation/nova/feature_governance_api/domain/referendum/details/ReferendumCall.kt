package io.novafoundation.nova.feature_governance_api.domain.referendum.details

import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance

sealed class ReferendumCall {

    data class TreasuryRequest(val amount: Balance) : ReferendumCall()
}
