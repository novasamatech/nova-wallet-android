package io.novafoundation.nova.feature_governance_impl.data.repository.v2

import io.novafoundation.nova.common.utils.formatNamed
import io.novafoundation.nova.core_db.dao.GovernanceDAppsDao
import io.novafoundation.nova.core_db.model.GovernanceDAppLocal
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.ReferendumId
import io.novafoundation.nova.feature_governance_api.data.repository.GovernanceDAppsRepository
import io.novafoundation.nova.feature_governance_api.domain.referendum.details.ReferendumDApp
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId

class RealGovernanceDAppsRepository(
    val governanceDAppsDao: GovernanceDAppsDao
) : GovernanceDAppsRepository {

    override fun getReferendumDApps(chainId: ChainId, referendumId: ReferendumId): List<ReferendumDApp> {
        val dapps = governanceDAppsDao.getChainDapps(chainId)
        return mapDappsLocalToDomain(referendumId, dapps)
    }
}

private fun mapDappsLocalToDomain(referendumId: ReferendumId, dapps: List<GovernanceDAppLocal>): List<ReferendumDApp> {
    return dapps.map {
        ReferendumDApp(
            it.chainId,
            it.name,
            it.referendumUrl.formatNamed("referendumId" to referendumId.toString()),
            it.iconUrl,
            it.details
        )
    }
}
