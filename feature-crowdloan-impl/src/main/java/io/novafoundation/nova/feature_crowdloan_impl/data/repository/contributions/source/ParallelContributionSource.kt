package io.novafoundation.nova.feature_crowdloan_impl.data.repository.contributions.source

import io.novafoundation.nova.feature_crowdloan_api.data.source.contribution.ExternalContributionSource
import io.novafoundation.nova.feature_crowdloan_api.domain.contributions.Contribution
import io.novafoundation.nova.feature_crowdloan_impl.data.network.api.parallel.ParallelApi
import io.novafoundation.nova.runtime.ext.Geneses
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novasama.substrate_sdk_android.runtime.AccountId

class ParallelContributionSource(
    private val parallelApi: ParallelApi,
) : ExternalContributionSource {

    override val supportedChains = setOf(Chain.Geneses.POLKADOT)

    override val sourceId: String = Contribution.PARALLEL_SOURCE_ID

    override suspend fun getContributions(
        chain: Chain,
        accountId: AccountId,
    ): Result<List<ExternalContributionSource.ExternalContribution>> {
        return Result.success(emptyList())
    }
}
