package io.novafoundation.nova.feature_governance_impl.data.offchain.v2.delegation.stats.response

import com.google.gson.annotations.SerializedName
import io.novafoundation.nova.common.data.network.subquery.SubQueryNodes
import java.math.BigInteger

class AllVotesResponse(
    @SerializedName("castingVotings") val direct: SubQueryNodes<DirectVoteRemote>,
    @SerializedName("delegatorVotings") val delegated: SubQueryNodes<DelegatedVoteRemote>
)

class DirectVotesResponse(
    @SerializedName("castingVotings") val direct: SubQueryNodes<DirectVoteRemote>,
)

class DirectVoteRemote(
    val referendumId: BigInteger,
    override val standardVote: StandardVoteRemote?,
    override val splitVote: SplitVoteRemote?,
    override val splitAbstainVote: SplitAbstainVoteRemote?
) : MultiVoteRemote

class DelegatedVoteRemote(
    val vote: VoteRemote,
    val parent: Parent
) {

    class Parent(
        val referendumId: BigInteger,
        val delegate: Delegate,
        val standardVote: StandardVoteRemote?
    )

    class Delegate(@SerializedName("accountId") val address: String)
}
