package io.novafoundation.nova.feature_governance_impl.data.offchain.delegation.v2.stats.response

import io.novafoundation.nova.common.data.network.subquery.SubQueryNodes
import java.math.BigInteger

class ReferendumSplitAbstainVotersResponse(
    val referendum: Referendum
) {

    class Referendum(val trackId: BigInteger, val castingVotings: SubQueryNodes<Voter>)

    class Voter(
        val splitAbstainVote: SplitAbstainVoteRemote?
    )
}
