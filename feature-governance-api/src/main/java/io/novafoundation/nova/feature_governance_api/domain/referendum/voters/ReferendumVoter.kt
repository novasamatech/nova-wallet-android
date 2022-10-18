package io.novafoundation.nova.feature_governance_api.domain.referendum.voters

import io.novafoundation.nova.feature_account_api.domain.account.identity.Identity
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.AccountVote
import jp.co.soramitsu.fearless_utils.runtime.AccountId

data class ReferendumVoter(
    val vote: AccountVote,
    val accountId: AccountId,
    val identity: Identity?,
)
