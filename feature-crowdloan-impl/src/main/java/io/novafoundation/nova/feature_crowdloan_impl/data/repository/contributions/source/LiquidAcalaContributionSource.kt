package io.novafoundation.nova.feature_crowdloan_impl.data.repository.contributions.source

import android.util.Log
import io.novafoundation.nova.common.utils.LOG_TAG
import io.novafoundation.nova.feature_crowdloan_impl.data.network.api.acala.AcalaApi
import io.novafoundation.nova.feature_crowdloan_impl.data.network.api.acala.getContributions
import io.novafoundation.nova.feature_crowdloan_impl.data.source.contribution.ExternalContributionSource
import io.novafoundation.nova.runtime.ext.Geneses
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.repository.ParachainInfoRepository
import jp.co.soramitsu.fearless_utils.runtime.AccountId
import java.math.BigInteger

class LiquidAcalaContributionSource(
    private val acalaApi: AcalaApi,
    private val parachainInfoRepository: ParachainInfoRepository,
) : ExternalContributionSource {

    override val supportedChains = setOf(Chain.Geneses.POLKADOT)

    override suspend fun getContributions(
        chain: Chain,
        accountId: AccountId,
    ): List<ExternalContributionSource.Contribution> = runCatching {
        val amount = acalaApi.getContributions(
            chain = chain,
            accountId = accountId
        ).proxyAmount

        listOfNotNull(
            ExternalContributionSource.Contribution(
                sourceName = "Liquid",
                amount = amount!!,
                paraId = parachainInfoRepository.paraId(Chain.Geneses.ACALA)!!
            ).takeIf { amount > BigInteger.ZERO }
        )
    }.getOrElse {
        Log.e(LOG_TAG, "Failed to fetch acala contributions: ${it.message}")

        emptyList()
    }
}
