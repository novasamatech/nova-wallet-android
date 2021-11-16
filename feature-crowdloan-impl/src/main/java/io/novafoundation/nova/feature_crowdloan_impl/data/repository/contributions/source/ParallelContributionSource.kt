package io.novafoundation.nova.feature_crowdloan_impl.data.repository.contributions.source

import android.util.Log
import io.novafoundation.nova.common.utils.LOG_TAG
import io.novafoundation.nova.feature_crowdloan_impl.data.network.api.parallel.ParallelApi
import io.novafoundation.nova.feature_crowdloan_impl.data.network.api.parallel.getContributions
import io.novafoundation.nova.feature_crowdloan_impl.data.source.contribution.ExternalContributionSource
import io.novafoundation.nova.runtime.ext.Geneses
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import jp.co.soramitsu.fearless_utils.runtime.AccountId

class ParallelContributionSource(
    private val parallelApi: ParallelApi,
) : ExternalContributionSource {

    override val supportedChains = setOf(Chain.Geneses.POLKADOT)

    override suspend fun getContributions(
        chain: Chain,
        accountId: AccountId,
    ): List<ExternalContributionSource.Contribution> = runCatching {
        parallelApi.getContributions(
            chain = chain,
            accountId = accountId
        ).map {
            ExternalContributionSource.Contribution(
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
