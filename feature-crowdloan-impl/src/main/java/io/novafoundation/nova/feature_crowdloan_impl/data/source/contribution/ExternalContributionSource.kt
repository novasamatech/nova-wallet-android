package io.novafoundation.nova.feature_crowdloan_impl.data.source.contribution

import io.novafoundation.nova.common.data.network.runtime.binding.ParaId
import io.novafoundation.nova.feature_crowdloan_api.data.common.SourceContribution
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import jp.co.soramitsu.fearless_utils.runtime.AccountId
import java.math.BigInteger

interface ExternalContributionSource {

    class Contribution(
        val sourceName: String?,
        override val amount: BigInteger,
        val paraId: ParaId,
    ) : SourceContribution

    /**
     * null in case every chain is supported
     */
    val supportedChains: Set<ChainId>?

    suspend fun getContributions(
        chain: Chain,
        accountId: AccountId,
    ): List<Contribution>
}

fun ExternalContributionSource.supports(chain: Chain) = supportedChains == null || chain.id in supportedChains!!
