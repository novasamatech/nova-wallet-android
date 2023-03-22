package io.novafoundation.nova.feature_governance_impl.data.offchain.delegation.v2.stats.response

import com.google.gson.annotations.SerializedName
import io.novafoundation.nova.common.data.network.subquery.SubQueryNodes
import io.novafoundation.nova.common.data.network.subquery.SubQueryTotalCount
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance

class DelegateStatsResponse(
    val delegates: SubQueryNodes<Delegate>
) {

    class Delegate(
        @SerializedName("accountId") val address: String,
        val delegators: Int,
        val delegatorVotes: Balance,
        val delegateVotes: SubQueryTotalCount
    )
}
