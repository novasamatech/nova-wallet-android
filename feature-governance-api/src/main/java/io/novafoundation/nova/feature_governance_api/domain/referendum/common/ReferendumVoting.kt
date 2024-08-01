package io.novafoundation.nova.feature_governance_api.domain.referendum.common

import io.novafoundation.nova.common.data.network.runtime.binding.Perbill
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import java.math.BigInteger

data class ReferendumVoting(
    val support: Support,
    val approval: Approval,
    val abstainVotes: BigInteger?
) {

    data class Support(
        val turnout: Balance,
        val electorate: Balance,
    )

    data class Approval(
        val ayeVotes: Votes,
        val nayVotes: Votes,
    ) {

        // post-conviction
        data class Votes(
            val amount: Balance,
            val fraction: Perbill
        )
    }
}

fun ReferendumVoting.Approval.totalVotes(): Balance {
    return ayeVotes.amount + nayVotes.amount
}

fun ReferendumVoting.Approval.ayeVotesIfNotEmpty(): ReferendumVoting.Approval.Votes? {
    return ayeVotes.takeIf { totalVotes() != Balance.ZERO }
}
