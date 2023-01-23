package io.novafoundation.nova.feature_governance_api.domain.delegation.delegate.delegators.model

import io.novafoundation.nova.feature_account_api.data.model.OnChainIdentity
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.Delegation
import jp.co.soramitsu.fearless_utils.runtime.AccountId

data class Delegator(
    val accountId: AccountId,
    val identity: OnChainIdentity?,
    val delegatedVote: Delegation.Vote
)
