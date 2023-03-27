package io.novafoundation.nova.feature_governance_impl.data.offchain.delegation.v2.stats.response

import io.novafoundation.nova.common.data.network.subquery.SubQueryNodes
import io.novafoundation.nova.common.data.network.subquery.SubQueryTotalCount
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance

class DelegateDetailedStatsResponse(
    val delegates: SubQueryNodes<Delegate>
) {

    class Delegate(
        val delegators: Int,
        val delegatorVotes: Balance,
        val recentVotes: SubQueryTotalCount,
        val allVotes: SubQueryTotalCount
    )
}
