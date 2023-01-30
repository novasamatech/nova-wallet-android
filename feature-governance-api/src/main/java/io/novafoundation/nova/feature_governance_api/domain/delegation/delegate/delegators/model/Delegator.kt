package io.novafoundation.nova.feature_governance_api.domain.delegation.delegate.delegators.model

import io.novafoundation.nova.common.utils.sumByBigDecimal
import io.novafoundation.nova.feature_account_api.domain.account.identity.Identity
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.Delegation
import io.novafoundation.nova.feature_governance_api.domain.referendum.voters.GenericVoter
import io.novafoundation.nova.feature_governance_api.domain.referendum.voters.GenericVoter.ConvictionVote
import io.novafoundation.nova.feature_wallet_api.domain.model.amountFromPlanks
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import jp.co.soramitsu.fearless_utils.runtime.AccountId
import java.math.BigDecimal

class Delegator(
    override val accountId: AccountId,
    override val identity: Identity?,
    override val vote: Vote
) : GenericVoter<Delegator.Vote> {

    sealed class Vote(override val totalVotes: BigDecimal): GenericVoter.Vote {

        class SingleTrack(val delegation: ConvictionVote): Vote(delegation.totalVotes)

        class MultiTrack(val trackCount: Int, totalVotes: BigDecimal): Vote(totalVotes)
    }
}

fun Delegator(
    accountId: AccountId,
    identity: Identity?,
    delegatorTrackDelegations: List<Delegation.Vote>,
    chainAsset: Chain.Asset,
): Delegator {
    val simpleVotes = delegatorTrackDelegations.map {
        ConvictionVote(chainAsset.amountFromPlanks(it.amount), it.conviction)
    }

    val vote = if (simpleVotes.size == 1) {
        Delegator.Vote.SingleTrack(simpleVotes.single())
    } else {
        val totalVotes = simpleVotes.sumByBigDecimal(ConvictionVote::totalVotes)
        Delegator.Vote.MultiTrack(trackCount = simpleVotes.size, totalVotes = totalVotes)
    }

    return Delegator(accountId, identity, vote)
}
