package io.novafoundation.nova.feature_crowdloan_api.data.source.contribution

import io.novafoundation.nova.common.data.network.runtime.binding.ParaId
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import java.math.BigInteger
import jp.co.soramitsu.fearless_utils.runtime.AccountId

interface ExternalContributionSource {

    class ExternalContribution(
        val sourceId: String,
        val amount: BigInteger,
        val paraId: ParaId,
    )

    /**
     * null in case every chain is supported
     */
    val supportedChains: Set<ChainId>?

    val sourceId: String

    suspend fun getContributions(
        chain: Chain,
        accountId: AccountId,
    ): List<ExternalContribution>
}

fun ExternalContributionSource.supports(chain: Chain) = supportedChains == null || chain.id in supportedChains!!
