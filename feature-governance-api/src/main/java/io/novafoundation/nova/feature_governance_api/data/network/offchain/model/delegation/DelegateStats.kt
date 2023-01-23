package io.novafoundation.nova.feature_governance_api.data.network.offchain.model.delegation

import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import jp.co.soramitsu.fearless_utils.runtime.AccountId

class DelegateStats(
    val accountId: AccountId,
    val delegationsCount: Int,
    val delegatedVotes: Balance,
    val recentVotes: Int
)
