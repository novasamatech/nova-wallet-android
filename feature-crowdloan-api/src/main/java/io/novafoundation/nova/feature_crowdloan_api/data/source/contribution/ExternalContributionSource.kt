package io.novafoundation.nova.feature_crowdloan_api.data.source.contribution

import io.novafoundation.nova.common.data.network.runtime.binding.ParaId
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novasama.substrate_sdk_android.runtime.AccountId
import java.math.BigInteger

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
    ): Result<List<ExternalContribution>>
}

fun ExternalContributionSource.supports(chain: Chain) = supportedChains == null || chain.id in supportedChains!!
