package io.novafoundation.nova.feature_governance_impl.domain.dapp

import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.ReferendumId
import io.novafoundation.nova.feature_governance_api.data.repository.GovernanceDAppsRepository
import io.novafoundation.nova.feature_governance_api.domain.referendum.details.ReferendumDApp
import io.novafoundation.nova.feature_governance_impl.data.dapps.GovernanceDAppsSyncService
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import kotlinx.coroutines.flow.Flow

class GovernanceDAppsInteractor(
    private val governanceDAppsSyncService: GovernanceDAppsSyncService,
    private val governanceDAppsRepository: GovernanceDAppsRepository
) {

    fun syncGovernanceDapps(): Flow<*> = flowOf {
        governanceDAppsSyncService.syncDapps()
    }

    fun getReferendumDapps(chainId: ChainId, referendumId: ReferendumId): List<ReferendumDApp> {
        return governanceDAppsRepository.getReferendumDApps(chainId, referendumId)
    }
}
