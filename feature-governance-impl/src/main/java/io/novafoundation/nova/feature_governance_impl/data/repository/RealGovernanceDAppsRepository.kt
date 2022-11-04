package io.novafoundation.nova.feature_governance_impl.data.repository

import io.novafoundation.nova.common.utils.formatNamed
import io.novafoundation.nova.core_db.dao.GovernanceDAppsDao
import io.novafoundation.nova.core_db.model.GovernanceDAppLocal
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.ReferendumId
import io.novafoundation.nova.feature_governance_api.data.repository.GovernanceDAppsRepository
import io.novafoundation.nova.feature_governance_api.domain.referendum.details.ReferendumDApp
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RealGovernanceDAppsRepository(
    private val governanceDAppsDao: GovernanceDAppsDao
) : GovernanceDAppsRepository {

    override fun observeReferendumDApps(chainId: ChainId, referendumId: ReferendumId): Flow<List<ReferendumDApp>> {
        return governanceDAppsDao.observeChainDapps(chainId)
            .map { mapDappsLocalToDomain(referendumId, it) }
    }
}

private fun mapDappsLocalToDomain(referendumId: ReferendumId, dapps: List<GovernanceDAppLocal>): List<ReferendumDApp> {
    return dapps.map {
        ReferendumDApp(
            it.chainId,
            it.name,
            it.referendumUrl.formatNamed("referendumId" to referendumId.value.toString()),
            it.iconUrl,
            it.details
        )
    }
}
