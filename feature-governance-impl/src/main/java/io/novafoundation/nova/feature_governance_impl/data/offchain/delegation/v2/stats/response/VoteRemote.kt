package io.novafoundation.nova.feature_governance_impl.data.offchain.delegation.v2.stats.response

import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.AccountVote
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.runtime.multiNetwork.runtime.types.custom.vote.Vote
import io.novafoundation.nova.runtime.multiNetwork.runtime.types.custom.vote.mapConvictionFromString
import java.math.BigInteger

class StandardVoteRemote(val aye: Boolean, val vote: VoteRemote)

class SplitVoteRemote(val ayeAmount: Balance, val nayAmount: Balance)

class SplitAbstainVoteRemote(val ayeAmount: Balance, val nayAmount: Balance, val abstainAmount: Balance)

interface MultiVoteRemote {

    val standardVote: StandardVoteRemote?

    val splitVote: SplitVoteRemote?

    val splitAbstainVote: SplitAbstainVoteRemote?
}

fun mapMultiVoteRemoteToAccountVote(vote: MultiVoteRemote): AccountVote {
    val standard = vote.standardVote
    val split = vote.splitVote
    val splitAbstain = vote.splitAbstainVote

    return when {
        standard != null -> AccountVote.Standard(
            balance = standard.vote.amount,
            vote = Vote(
                aye = standard.aye,
                conviction = mapConvictionFromString(standard.vote.conviction)
            )
        )
        split != null -> AccountVote.Split(
            aye = split.ayeAmount,
            nay = split.nayAmount
        )
        splitAbstain != null -> AccountVote.SplitAbstain(
            aye = splitAbstain.ayeAmount,
            nay = splitAbstain.nayAmount,
            abstain = splitAbstain.abstainAmount
        )
        else -> AccountVote.Unsupported
    }
}

class VoteRemote(val amount: BigInteger, val conviction: String)
