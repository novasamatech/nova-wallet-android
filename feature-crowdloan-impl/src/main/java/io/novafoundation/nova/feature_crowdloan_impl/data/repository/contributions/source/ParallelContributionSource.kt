package io.novafoundation.nova.feature_crowdloan_impl.data.repository.contributions.source

import android.util.Log
import io.novafoundation.nova.common.utils.LOG_TAG
import io.novafoundation.nova.feature_crowdloan_api.data.network.blockhain.binding.FundInfo
import io.novafoundation.nova.feature_crowdloan_api.data.network.blockhain.binding.ParaId
import io.novafoundation.nova.feature_crowdloan_impl.data.network.api.parallel.ParallelApi
import io.novafoundation.nova.feature_crowdloan_impl.data.network.api.parallel.getContributions
import io.novafoundation.nova.feature_crowdloan_impl.data.source.contribution.ContributionSource
import io.novafoundation.nova.runtime.ext.Geneses
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import jp.co.soramitsu.fearless_utils.runtime.AccountId

class ParallelContributionSource(
    private val parallelApi: ParallelApi,
) : ContributionSource {

    override val supportedChains = setOf(Chain.Geneses.POLKADOT)

    override suspend fun getContributions(
        chain: Chain,
        accountId: AccountId,
        funds: Map<ParaId, FundInfo>,
    ): List<ContributionSource.Contribution> = runCatching {
        parallelApi.getContributions(
            chain = chain,
            accountId = accountId
        ).map {
            ContributionSource.Contribution(
                sourceName = "Parallel",
                amount = it.amount,
                paraId = it.paraId
            )
        }
    }.getOrElse {
        Log.e(LOG_TAG, "Failed to fetch parallel contributions: ${it.message}")

        emptyList()
    }
}
