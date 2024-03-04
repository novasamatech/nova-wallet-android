package io.novafoundation.nova.feature_governance_api.data.network.blockhain.model

import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.runtime.multiNetwork.runtime.types.custom.vote.Conviction
import io.novasama.substrate_sdk_android.runtime.AccountId

data class Delegation(
    val vote: Vote,
    val delegator: AccountId,
    val delegate: AccountId,
) {

    data class Vote(
        val amount: Balance,
        val conviction: Conviction
    )
}
