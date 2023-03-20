package io.novafoundation.nova.feature_governance_impl.data.offchain.delegation.v2.stats.response

import com.google.gson.annotations.SerializedName
import io.novafoundation.nova.common.data.network.subquery.SubQueryNodes

class ReferendumVotersResponse(
    @SerializedName("castingVotings") val voters: SubQueryNodes<ReferendumVoterRemote>
)

class ReferendumVoterRemote(
    @SerializedName("voter") val voterId: String,
    val delegateId: String,
    override val standardVote: StandardVoteRemote?,
    override val splitVote: SplitVoteRemote?,
    override val splitAbstainVote: SplitAbstainVoteRemote?,
    val delegatorVotes: SubQueryNodes<ReferendumDelegatorVoteRemote>
) : MultiVoteRemote

class ReferendumDelegatorVoteRemote(
    @SerializedName("delegator") val delegatorId: String,
    val vote: VoteRemote
)
