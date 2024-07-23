package io.novafoundation.nova.feature_governance_api.domain.referendum.common

import io.novafoundation.nova.common.data.network.runtime.binding.Perbill
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.VotingThreshold.Threshold
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance

data class ReferendumVoting(
    val support: Support,
    val approval: Approval
) {

    data class Support(
        val threshold: Threshold<Balance>,
        val turnout: Balance,
        val electorate: Balance
    )

    data class Approval(
        val ayeVotes: Votes,
        val nayVotes: Votes,
        val threshold: Threshold<Perbill>
    ) {

        // post-conviction
        data class Votes(
            val amount: Balance,
            val fraction: Perbill
        )
    }
}

fun ReferendumVoting.Support.passing(): Boolean {
    return threshold.passing
}

fun ReferendumVoting.Approval.passing(): Boolean {
    return threshold.passing
}

fun ReferendumVoting.passing(): Boolean {
    return support.passing() && approval.passing()
}

fun ReferendumVoting.Approval.totalVotes(): Balance {
    return ayeVotes.amount + nayVotes.amount
}

fun ReferendumVoting.Approval.ayeVotesIfNotEmpty(): ReferendumVoting.Approval.Votes? {
    return ayeVotes.takeIf { totalVotes() != Balance.ZERO }
}
