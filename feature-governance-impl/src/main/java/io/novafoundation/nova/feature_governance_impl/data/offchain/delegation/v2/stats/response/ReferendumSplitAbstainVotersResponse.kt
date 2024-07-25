package io.novafoundation.nova.feature_governance_impl.data.offchain.delegation.v2.stats.response

import com.google.gson.annotations.SerializedName
import io.novafoundation.nova.common.data.network.subquery.SubQueryNodes

class ReferendumSplitAbstainVotersResponse(
    @SerializedName("castingVotings") val voters: SubQueryNodes<Voter>
) {
    class Voter(
        val splitAbstainVote: SplitAbstainVoteRemote?
    )
}
