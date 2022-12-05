package io.novafoundation.nova.feature_governance_impl.domain.dapp

import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.common.utils.flowOfAll
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.ReferendumId
import io.novafoundation.nova.feature_governance_api.data.source.GovernanceSourceRegistry
import io.novafoundation.nova.feature_governance_api.data.source.SupportedGovernanceOption
import io.novafoundation.nova.feature_governance_impl.data.dapps.GovernanceDAppsSyncService
import kotlinx.coroutines.flow.Flow

class GovernanceDAppsInteractor(
    private val governanceDAppsSyncService: GovernanceDAppsSyncService,
    private val governanceSourceRegistry: GovernanceSourceRegistry,
) {

    fun syncGovernanceDapps(): Flow<*> = flowOf {
        governanceDAppsSyncService.syncDapps()
    }

    fun observeReferendumDapps(referendumId: ReferendumId, option: SupportedGovernanceOption) = flowOfAll {
        val govSource = governanceSourceRegistry.sourceFor(option)
        govSource.dappsRepository.observeReferendumDApps(option.assetWithChain.chain.id, referendumId)
    }
}
