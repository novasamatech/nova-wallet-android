package io.novafoundation.nova.feature_governance_impl.data.repository.common

import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.amountMultiplier
import io.novafoundation.nova.feature_governance_api.data.network.offchain.model.referendum.OffChainReferendumVotingDetails.VotingInfo
import io.novafoundation.nova.feature_governance_api.data.network.offchain.model.referendum.empty
import io.novafoundation.nova.feature_governance_api.data.network.offchain.model.referendum.plus
import io.novafoundation.nova.feature_governance_impl.data.offchain.delegation.v2.stats.response.ReferendumVotesResponse
import io.novafoundation.nova.feature_governance_impl.data.offchain.delegation.v2.stats.response.SplitAbstainVoteRemote
import io.novafoundation.nova.feature_governance_impl.data.offchain.delegation.v2.stats.response.SplitVoteRemote
import io.novafoundation.nova.feature_governance_impl.data.offchain.delegation.v2.stats.response.StandardVoteRemote
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.runtime.multiNetwork.runtime.types.custom.vote.Conviction
import io.novafoundation.nova.runtime.multiNetwork.runtime.types.custom.vote.mapConvictionFromString
import java.math.BigDecimal

fun StandardVoteRemote?.toOffChainVotes(): VotingInfo.Full {
    if (this == null) return VotingInfo.Full.empty()

    val conviction = mapConvictionFromString(this.vote.conviction)
    val amount = this.vote.amount.toBigDecimal() * conviction.amountMultiplier()
    return VotingInfo.Full(
        aye = if (this.aye) amount else BigDecimal.ZERO,
        nay = if (!this.aye) amount else BigDecimal.ZERO,
        abstain = BigDecimal.ZERO,
        support = this.vote.amount
    )
}

fun SplitVoteRemote?.toOffChainVotes(): VotingInfo.Full {
    if (this == null) return VotingInfo.Full.empty()

    val amountMultiplier = Conviction.None.amountMultiplier()

    return VotingInfo.Full(
        aye = this.ayeAmount.toBigDecimal() * amountMultiplier,
        nay = this.nayAmount.toBigDecimal() * amountMultiplier,
        abstain = BigDecimal.ZERO,
        support = this.ayeAmount + this.nayAmount
    )
}

fun SplitAbstainVoteRemote?.toOffChainVotes(): VotingInfo.Full {
    if (this == null) return VotingInfo.Full.empty()

    val amountMultiplier = Conviction.None.amountMultiplier()

    return VotingInfo.Full(
        aye = this.ayeAmount.toBigDecimal() * amountMultiplier,
        nay = this.nayAmount.toBigDecimal() * amountMultiplier,
        abstain = this.abstainAmount.toBigDecimal() * amountMultiplier,
        support = this.ayeAmount + this.nayAmount + this.abstainAmount
    )
}

fun ReferendumVotesResponse.Vote.toOffChainVotes(): VotingInfo.Full {
    var delegatorsVoteSum = BigDecimal.ZERO
    var delegatorSupportSum = Balance.ZERO

    this.delegatorVotes.nodes.forEach { delegatorVote ->
        val conviction = mapConvictionFromString(delegatorVote.vote.conviction)
        delegatorsVoteSum += delegatorVote.vote.amount.toBigDecimal() * conviction.amountMultiplier()
        delegatorSupportSum += delegatorVote.vote.amount
    }

    return standardVote.toOffChainVotes() + splitVote.toOffChainVotes() + splitAbstainVote.toOffChainVotes() + getDelegationVotes()
}

private fun ReferendumVotesResponse.Vote.getDelegationVotes(): VotingInfo.Full {
    return if (standardVote != null) {
        var delegatorsVoteSum = BigDecimal.ZERO
        var delegatorSupportSum = Balance.ZERO

        this.delegatorVotes.nodes.forEach { delegatorVote ->
            val conviction = mapConvictionFromString(delegatorVote.vote.conviction)
            delegatorsVoteSum += delegatorVote.vote.amount.toBigDecimal() * conviction.amountMultiplier()
            delegatorSupportSum += delegatorVote.vote.amount
        }

        return VotingInfo.Full(
            aye = if (standardVote.aye) delegatorsVoteSum else BigDecimal.ZERO,
            nay = if (!standardVote.aye) delegatorsVoteSum else BigDecimal.ZERO,
            abstain = BigDecimal.ZERO,
            support = delegatorSupportSum
        )
    } else {
        VotingInfo.Full.empty()
    }
}
