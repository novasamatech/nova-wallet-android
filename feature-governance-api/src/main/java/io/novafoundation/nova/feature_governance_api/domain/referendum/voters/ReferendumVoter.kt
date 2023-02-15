package io.novafoundation.nova.feature_governance_api.domain.referendum.voters

import io.novafoundation.nova.feature_account_api.domain.account.identity.Identity
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.AccountVote
import io.novafoundation.nova.feature_governance_api.domain.delegation.delegate.label.DelegateLabel
import io.novafoundation.nova.feature_wallet_api.domain.model.amountFromPlanks
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import jp.co.soramitsu.fearless_utils.runtime.AccountId

class ReferendumVoter(
    override val vote: GenericVoter.ConvictionVote?,
    override val identity: Identity?,
    override val accountId: AccountId,
    val metadata: DelegateLabel.Metadata?,
    val delegators: List<ReferendumVoterDelegator>,
) : GenericVoter<GenericVoter.ConvictionVote?>

class ReferendumVoterDelegator(
    override val accountId: AccountId,
    override val vote: GenericVoter.ConvictionVote,
    val metadata: DelegateLabel.Metadata?,
    override val identity: Identity?,
) : GenericVoter<GenericVoter.ConvictionVote?>

fun ReferendumVoter(
    accountVote: AccountVote,
    identity: Identity?,
    accountId: AccountId,
    chainAsset: Chain.Asset,
    metadata: DelegateLabel.Metadata?,
    delegators: List<ReferendumVoterDelegator>
): ReferendumVoter {
    val vote = ConvictionVote(accountVote, chainAsset)

    return ReferendumVoter(
        vote = vote,
        identity = identity,
        accountId = accountId,
        metadata = metadata,
        delegators = delegators
    )
}

fun ConvictionVote(accountVote: AccountVote, chainAsset: Chain.Asset): GenericVoter.ConvictionVote? {
    return when (accountVote) {
        is AccountVote.Standard -> {
            val amount = chainAsset.amountFromPlanks(accountVote.balance)
            GenericVoter.ConvictionVote(amount, accountVote.vote.conviction)
        }
        AccountVote.Unsupported -> null
    }
}
