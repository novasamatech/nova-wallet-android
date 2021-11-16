package io.novafoundation.nova.feature_crowdloan_impl.data.repository.contributions.source

import android.util.Log
import io.novafoundation.nova.common.utils.LOG_TAG
import io.novafoundation.nova.feature_crowdloan_api.data.network.blockhain.binding.FundInfo
import io.novafoundation.nova.feature_crowdloan_api.data.network.blockhain.binding.ParaId
import io.novafoundation.nova.feature_crowdloan_impl.data.network.api.acala.AcalaApi
import io.novafoundation.nova.feature_crowdloan_impl.data.network.api.acala.getContributions
import io.novafoundation.nova.feature_crowdloan_impl.data.source.contribution.ContributionSource
import io.novafoundation.nova.runtime.ext.Geneses
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import jp.co.soramitsu.fearless_utils.runtime.AccountId

class LiquidAcalaContributionSource(
    private val acalaApi: AcalaApi,
) : ContributionSource {

    override val supportedChains = setOf(Chain.Geneses.POLKADOT)

    override suspend fun getContributions(
        chain: Chain,
        accountId: AccountId,
        funds: Map<ParaId, FundInfo>,
    ): List<ContributionSource.Contribution> = runCatching {
        val amount = acalaApi.getContributions(
            chain = chain,
            accountId = accountId
        ).proxyAmount

        listOf(
            ContributionSource.Contribution(
                sourceName = "Liquid",
                amount = amount!!,
                paraId = 2000.toBigInteger() // TODO consider how to identify it without hardcode
            )
        )
    }.getOrElse {
        Log.e(LOG_TAG, "Failed to fetch acala contributions: ${it.message}")

        emptyList()
    }
}
