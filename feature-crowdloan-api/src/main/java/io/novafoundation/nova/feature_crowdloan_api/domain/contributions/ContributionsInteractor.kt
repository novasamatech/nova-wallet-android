package io.novafoundation.nova.feature_crowdloan_api.domain.contributions

import io.novafoundation.nova.core.updater.Updater
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_crowdloan_api.data.common.CrowdloanContribution
import io.novafoundation.nova.feature_crowdloan_api.data.source.contribution.ExternalContributionSource
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import java.math.BigInteger
import kotlinx.coroutines.flow.Flow

interface ContributionsInteractor {
    fun runUpdate(): Flow<Updater.SideEffect>

    fun getTotalAmountOfContributions(crowdloanContributions: List<CrowdloanContribution>): BigInteger

    fun externalContributionsFlow(chain: Chain, account: MetaAccount): Flow<List<ExternalContributionSource.ExternalContribution>>

    fun observeUserContributions(): Flow<ContributionsWithTotalAmount>
}
