package io.novafoundation.nova.feature_governance_api.data.network.blockhain.model

import jp.co.soramitsu.fearless_utils.runtime.AccountId

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
