package io.novafoundation.nova.feature_governance_api.domain.referendum.vote

import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.AccountVote
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.VoteType
import io.novafoundation.nova.runtime.multiNetwork.runtime.types.custom.vote.Conviction
import io.novafoundation.nova.runtime.multiNetwork.runtime.types.custom.vote.Vote
import java.math.BigInteger

fun constructAccountVote(amount: BigInteger, conviction: Conviction, voteType: VoteType): AccountVote {
    return if (voteType == VoteType.ABSTAIN) {
        AccountVote.SplitAbstain(
            aye = BigInteger.ZERO,
            nay = BigInteger.ZERO,
            abstain = amount
        )
    } else {
        AccountVote.Standard(
            vote = Vote(
                aye = voteType == VoteType.AYE,
                conviction = conviction
            ),
            balance = amount
        )
    }
}
