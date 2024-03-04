package io.novafoundation.nova.feature_governance_api.data.network.blockhain.model

import io.novasama.substrate_sdk_android.runtime.AccountId

class ReferendumVoter(
    val accountId: AccountId,
    val vote: AccountVote,
    val delegators: List<Delegation>
)

fun ReferendumVoter.getAllAccountIds(): List<AccountId> {
    return buildList {
        add(accountId)
        addAll(delegators.map { it.delegator })
    }
}
