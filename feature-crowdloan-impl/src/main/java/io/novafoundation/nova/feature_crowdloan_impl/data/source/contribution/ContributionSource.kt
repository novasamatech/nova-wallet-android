package io.novafoundation.nova.feature_crowdloan_impl.data.source.contribution

import io.novafoundation.nova.feature_crowdloan_api.data.network.blockhain.binding.FundInfo
import io.novafoundation.nova.feature_crowdloan_api.data.network.blockhain.binding.ParaId
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import jp.co.soramitsu.fearless_utils.runtime.AccountId
import java.math.BigInteger

interface ContributionSource {

    class Contribution(
        val sourceName: String?,
        val amount: BigInteger,
        val paraId: ParaId,
    )

    suspend fun getContributions(
        chain: Chain,
        accountId: AccountId,
        funds: Map<ParaId, FundInfo>,
    ): List<Contribution>
}
