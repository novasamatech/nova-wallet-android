package io.novafoundation.nova.feature_governance_api.domain.delegation.delegate.delegators.model

import io.novafoundation.nova.common.utils.sumByBigDecimal
import io.novafoundation.nova.feature_account_api.domain.account.identity.Identity
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.Delegation
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.Voting
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.getConvictionVote
import io.novafoundation.nova.feature_governance_api.domain.referendum.voters.GenericVoter
import io.novafoundation.nova.feature_governance_api.domain.referendum.voters.GenericVoter.ConvictionVote
import io.novafoundation.nova.feature_wallet_api.domain.model.amountFromPlanks
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novasama.substrate_sdk_android.runtime.AccountId
import java.math.BigDecimal

class Delegator(
    override val accountId: AccountId,
    override val identity: Identity?,
    override val vote: Vote
) : GenericVoter<Delegator.Vote> {

    sealed class Vote(override val totalVotes: BigDecimal) : GenericVoter.Vote {

        class SingleTrack(val delegation: ConvictionVote) : Vote(delegation.totalVotes)

        class MultiTrack(val trackCount: Int, totalVotes: BigDecimal) : Vote(totalVotes)
    }
}

fun Delegator(
    accountId: AccountId,
    identity: Identity?,
    delegatorTrackDelegations: List<Delegation.Vote>,
    chainAsset: Chain.Asset,
): Delegator {
    val vote = requireNotNull(DelegatorVote(delegatorTrackDelegations, chainAsset))

    return Delegator(accountId, identity, vote)
}

fun DelegatorVote(delegatorTrackDelegations: List<Delegation.Vote>, chainAsset: Chain.Asset): Delegator.Vote? {
    val simpleVotes = delegatorTrackDelegations.map {
        ConvictionVote(chainAsset.amountFromPlanks(it.amount), it.conviction)
    }

    return DelegatorVote(simpleVotes)
}

@JvmName("DelegatorVoteFromDelegating")
fun DelegatorVote(delegations: Collection<Voting.Delegating>, chainAsset: Chain.Asset): Delegator.Vote? {
    val simpleVotes = delegations.map { it.getConvictionVote(chainAsset) }

    return DelegatorVote(simpleVotes)
}

@JvmName("DelegatorVoteFromConvictionVote")
fun DelegatorVote(votes: Collection<ConvictionVote>): Delegator.Vote? {
    return when (votes.size) {
        0 -> null
        1 -> Delegator.Vote.SingleTrack(votes.single())
        else -> {
            val totalVotes = votes.sumByBigDecimal(ConvictionVote::totalVotes)
            Delegator.Vote.MultiTrack(trackCount = votes.size, totalVotes = totalVotes)
        }
    }
}
