package io.novafoundation.nova.feature_governance_api.domain.referendum.voters

import io.novafoundation.nova.feature_account_api.domain.account.identity.Identity
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.AccountVote
import io.novafoundation.nova.feature_governance_api.data.network.offchain.model.delegation.DelegateMetadata
import io.novafoundation.nova.feature_wallet_api.domain.model.amountFromPlanks
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import jp.co.soramitsu.fearless_utils.runtime.AccountId

class ReferendumVoter(
    override val vote: GenericVoter.ConvictionVote?,
    override val identity: Identity?,
    override val accountId: AccountId,
    val delegators: List<ReferendumVoterDelegator>,
    val metadata: DelegateMetadata?
) : GenericVoter<GenericVoter.ConvictionVote?>

class ReferendumVoterDelegator(
    val accountId: AccountId,
    val vote: GenericVoter.ConvictionVote,
    val identity: Identity?,
    val metadata: DelegateMetadata?
)

fun ReferendumVoter(
    accountVote: AccountVote,
    identity: Identity?,
    accountId: AccountId,
    chainAsset: Chain.Asset,
    delegators: List<ReferendumVoterDelegator>,
): ReferendumVoter {
    val vote = ConvictionVote(accountVote, chainAsset)

    return ReferendumVoter(vote, identity, accountId, delegators, null)
}

// TODO support split and splitAbstain votes
fun ConvictionVote(accountVote: AccountVote, chainAsset: Chain.Asset): GenericVoter.ConvictionVote? {
    return when (accountVote) {
        is AccountVote.Standard -> {
            val amount = chainAsset.amountFromPlanks(accountVote.balance)
            GenericVoter.ConvictionVote(amount, accountVote.vote.conviction)
        }
        AccountVote.Unsupported -> null

        is AccountVote.Split -> null
        is AccountVote.SplitAbstain -> null
    }
}
