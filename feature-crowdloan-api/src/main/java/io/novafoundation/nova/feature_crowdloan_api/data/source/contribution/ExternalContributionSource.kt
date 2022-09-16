package io.novafoundation.nova.feature_crowdloan_api.data.source.contribution

import io.novafoundation.nova.common.data.network.runtime.binding.ParaId
import io.novafoundation.nova.feature_crowdloan_api.data.common.CrowdloanContribution
import io.novafoundation.nova.feature_crowdloan_api.domain.contributions.Contribution
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import jp.co.soramitsu.fearless_utils.runtime.AccountId
import java.math.BigInteger

interface ExternalContributionSource {

    class ExternalContribution(
        val sourceName: String?,
        override val amount: BigInteger,
        val paraId: ParaId,
    ) : CrowdloanContribution

    /**
     * null in case every chain is supported
     */
    val supportedChains: Set<ChainId>?

    val contributionsType: Contribution.Type

    suspend fun getContributions(
        chain: Chain,
        accountId: AccountId,
    ): List<ExternalContribution>
}

fun ExternalContributionSource.supports(chain: Chain) = supportedChains == null || chain.id in supportedChains!!
