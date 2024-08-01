package io.novafoundation.nova.feature_governance_impl.data.offchain.delegation.v2.stats.response

import io.novafoundation.nova.common.data.network.subquery.SubQueryNodes
import java.math.BigInteger

class ReferendumVotesResponse(
    val referendum: Referendum
) {

    class Referendum(val trackId: BigInteger, val castingVotings: SubQueryNodes<Vote>)

    class Vote(
        override val standardVote: StandardVoteRemote?,
        override val splitVote: SplitVoteRemote?,
        override val splitAbstainVote: SplitAbstainVoteRemote?,
        val delegatorVotes: SubQueryNodes<DelegatorVote>
    ) : MultiVoteRemote

    class DelegatorVote(
        val vote: VoteRemote
    )
}
