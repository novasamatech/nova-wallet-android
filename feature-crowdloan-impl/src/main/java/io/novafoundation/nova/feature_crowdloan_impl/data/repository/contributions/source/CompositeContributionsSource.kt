package io.novafoundation.nova.feature_crowdloan_impl.data.repository.contributions.source

import io.novafoundation.nova.feature_crowdloan_impl.data.source.contribution.ExternalContributionSource
import io.novafoundation.nova.feature_crowdloan_impl.data.source.contribution.supports
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import jp.co.soramitsu.fearless_utils.runtime.AccountId

class CompositeContributionsSource(
    private val children: Iterable<ExternalContributionSource>,
) : ExternalContributionSource {

    override val supportedChains: Set<ChainId>? = null

    override suspend fun getContributions(
        chain: Chain,
        accountId: AccountId,
    ): List<ExternalContributionSource.Contribution> {
        return children
            .filter { it.supports(chain) }
            .fold(mutableListOf()) { acc, source ->
                val elements = source.getContributions(chain, accountId)
                acc.addAll(elements)

                acc
            }
    }
}
