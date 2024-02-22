package io.novafoundation.nova.feature_governance_api.domain.referendum.voters

import io.novafoundation.nova.common.utils.sumByBigDecimal
import io.novafoundation.nova.feature_account_api.domain.account.identity.Identity
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.AccountVote
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.VoteType
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.amountFor
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.conviction
import io.novafoundation.nova.feature_governance_api.domain.delegation.delegate.label.DelegateLabel
import io.novafoundation.nova.feature_wallet_api.domain.model.amountFromPlanks
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novasama.substrate_sdk_android.runtime.AccountId
import java.math.BigDecimal

class ReferendumVoter(
    override val vote: Vote,
    override val identity: Identity?,
    override val accountId: AccountId,
    val metadata: DelegateLabel.Metadata?,
) : GenericVoter<ReferendumVoter.Vote> {

    sealed class Vote : GenericVoter.Vote {

        class OnlySelf(val selfVote: GenericVoter.ConvictionVote) : Vote(), GenericVoter.Vote by selfVote

        class WithDelegators(override val totalVotes: BigDecimal, val delegators: List<ReferendumVoterDelegator>) : Vote()
    }
}

class ReferendumVoterDelegator(
    override val accountId: AccountId,
    override val vote: GenericVoter.ConvictionVote,
    val metadata: DelegateLabel.Metadata?,
    override val identity: Identity?,
) : GenericVoter<GenericVoter.ConvictionVote>

fun ReferendumVoter(
    accountVote: AccountVote,
    voteType: VoteType,
    identity: Identity?,
    accountId: AccountId,
    chainAsset: Chain.Asset,
    metadata: DelegateLabel.Metadata?,
    delegators: List<ReferendumVoterDelegator>
): ReferendumVoter {
    val selfVote = ConvictionVote(accountVote, chainAsset, voteType)

    val referendumVote = if (delegators.isNotEmpty()) {
        val totalVotes = delegators.sumByBigDecimal { it.vote.totalVotes } + selfVote.totalVotes

        val selfAsDelegator = ReferendumVoterDelegator(accountId, selfVote, metadata, identity)
        val sortedDelegators = delegators.sortedByDescending { it.vote.totalVotes }
        val delegatorsPlusSelf = sortedDelegators + selfAsDelegator

        ReferendumVoter.Vote.WithDelegators(totalVotes, delegatorsPlusSelf)
    } else {
        ReferendumVoter.Vote.OnlySelf(selfVote)
    }

    return ReferendumVoter(
        vote = referendumVote,
        identity = identity,
        accountId = accountId,
        metadata = metadata,
    )
}

private fun ConvictionVote(accountVote: AccountVote, chainAsset: Chain.Asset, voteType: VoteType): GenericVoter.ConvictionVote {
    val amount = accountVote.amountFor(voteType)
    val conviction = accountVote.conviction()

    return if (amount != null && conviction != null) {
        GenericVoter.ConvictionVote(chainAsset.amountFromPlanks(amount), conviction)
    } else {
        error("Expected $accountVote to contain vote of type $voteType")
    }
}
