package io.novafoundation.nova.feature_governance_api.domain.referendum.common

import io.novafoundation.nova.common.data.network.runtime.binding.Perbill
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.VotingThreshold
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.VotingThreshold.Threshold
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.merge
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import java.math.BigInteger

data class ReferendumVoting(
    val support: Support,
    val approval: Approval,
    val abstainVotes: BigInteger?
) {

    data class Support(
        val threshold: Threshold<Balance>,
        val turnout: Balance,
        val electorate: Balance,
    )

    data class Approval(
        val ayeVotes: Votes,
        val nayVotes: Votes,
        val threshold: Threshold<Perbill>,
    ) {

        // post-conviction
        data class Votes(
            val amount: Balance,
            val fraction: Perbill
        )
    }
}

fun ReferendumVoting.Support.currentlyPassing(): Boolean {
    return threshold.currentlyPassing
}

fun ReferendumVoting.Approval.currentlyPassing(): Boolean {
    return threshold.currentlyPassing
}

fun ReferendumVoting.currentlyPassing(): Boolean {
    return support.currentlyPassing() && approval.currentlyPassing()
}

fun ReferendumVoting.projectedPassing(): VotingThreshold.ProjectedPassing {
    return support.threshold.projectedPassing.merge(approval.threshold.projectedPassing)
}

fun ReferendumVoting.Approval.totalVotes(): Balance {
    return ayeVotes.amount + nayVotes.amount
}

fun ReferendumVoting.Approval.ayeVotesIfNotEmpty(): ReferendumVoting.Approval.Votes? {
    return ayeVotes.takeIf { totalVotes() != Balance.ZERO }
}
