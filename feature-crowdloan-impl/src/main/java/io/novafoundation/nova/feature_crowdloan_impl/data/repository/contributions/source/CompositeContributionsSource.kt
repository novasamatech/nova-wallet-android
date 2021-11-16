package io.novafoundation.nova.feature_crowdloan_impl.data.repository.contributions.source

import io.novafoundation.nova.feature_crowdloan_api.data.network.blockhain.binding.FundInfo
import io.novafoundation.nova.feature_crowdloan_api.data.network.blockhain.binding.ParaId
import io.novafoundation.nova.feature_crowdloan_impl.data.source.contribution.ContributionSource
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import jp.co.soramitsu.fearless_utils.runtime.AccountId

class CompositeContributionsSource(
    private val children: Iterable<ContributionSource>,
) : ContributionSource {

    override suspend fun getContributions(
        chain: Chain,
        accountId: AccountId,
        funds: Map<ParaId, FundInfo>,
    ): List<ContributionSource.Contribution> {
        return children.fold(mutableListOf()) { acc, source ->
            val elements = source.getContributions(chain, accountId, funds)
            acc.addAll(elements)

            acc
        }
    }
}
