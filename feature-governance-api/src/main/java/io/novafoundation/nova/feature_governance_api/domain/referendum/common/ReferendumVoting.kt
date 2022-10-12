package io.novafoundation.nova.feature_governance_api.domain.referendum.common

import io.novafoundation.nova.common.data.network.runtime.binding.Perbill
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance

data class ReferendumVoting(
    val support: Support,
    val approval: Approval
) {

    data class Support(
        val threshold: Balance,
        val turnout: Balance
    )

    data class Approval(
        val ayeVotes: Votes,
        val nayVotes: Votes,
        val threshold: Perbill
    ) {

        data class Votes(
            val amount: Balance,
            val fraction: Perbill
        )
    }
}

fun ReferendumVoting.Support.passes(): Boolean {
    return turnout > threshold
}
