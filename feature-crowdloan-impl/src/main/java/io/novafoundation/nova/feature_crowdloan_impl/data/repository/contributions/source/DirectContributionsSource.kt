package io.novafoundation.nova.feature_crowdloan_impl.data.repository.contributions.source

import io.novafoundation.nova.feature_crowdloan_api.data.network.blockhain.binding.FundInfo
import io.novafoundation.nova.feature_crowdloan_api.data.network.blockhain.binding.ParaId
import io.novafoundation.nova.feature_crowdloan_api.data.repository.CrowdloanRepository
import io.novafoundation.nova.feature_crowdloan_api.data.repository.getContributions
import io.novafoundation.nova.feature_crowdloan_impl.data.source.contribution.ContributionSource
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import jp.co.soramitsu.fearless_utils.runtime.AccountId

class DirectContributionsSource(
    private val crowdloanRepository: CrowdloanRepository,
) : ContributionSource {

    override suspend fun getContributions(
        chain: Chain,
        accountId: AccountId,
        funds: Map<ParaId, FundInfo>,
    ): List<ContributionSource.Contribution> {
        return crowdloanRepository.getContributions(
            chainId = chain.id,
            accountId = accountId,
            keys = funds.mapValues { (_, fundInfo) -> fundInfo.trieIndex }
        ).mapNotNull { (paraId, contribution) ->
            contribution?.let {
                ContributionSource.Contribution(
                    sourceName = null, // no source name for direct contributions
                    amount = contribution.amount,
                    paraId = paraId
                )
            }
        }
    }
}
