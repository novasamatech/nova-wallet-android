package io.novafoundation.nova.feature_governance_api.data.repository

import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.ReferendumId
import io.novafoundation.nova.feature_governance_api.domain.referendum.details.ReferendumDApp
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId

interface GovernanceDAppsRepository {

    fun getReferendumDApps(chainId: ChainId, referendumId: ReferendumId): List<ReferendumDApp>
}
