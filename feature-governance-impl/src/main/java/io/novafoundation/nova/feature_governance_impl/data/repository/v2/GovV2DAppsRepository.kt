package io.novafoundation.nova.feature_governance_impl.data.repository.v2

import io.novafoundation.nova.common.utils.formatNamed
import io.novafoundation.nova.core_db.dao.GovernanceDAppsDao
import io.novafoundation.nova.core_db.model.GovernanceDAppLocal
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.ReferendumId
import io.novafoundation.nova.feature_governance_api.data.repository.GovernanceDAppsRepository
import io.novafoundation.nova.feature_governance_api.domain.referendum.details.ReferendumDApp
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class GovV2DAppsRepository(
    private val governanceDAppsDao: GovernanceDAppsDao
) : GovernanceDAppsRepository {

    override fun observeReferendumDApps(chainId: ChainId, referendumId: ReferendumId): Flow<List<ReferendumDApp>> {
        return governanceDAppsDao.observeChainDapps(chainId)
            .map { dapps ->
                val v1Dapps = dapps.filter { it.referendumUrlV2 != null }
                mapV2DappsLocalToDomain(referendumId, v1Dapps)
            }
    }
}

private fun mapV2DappsLocalToDomain(referendumId: ReferendumId, dapps: List<GovernanceDAppLocal>): List<ReferendumDApp> {
    return dapps.map {
        ReferendumDApp(
            it.chainId,
            it.name,
            it.referendumUrlV2?.formatNamed("referendumId" to referendumId.value.toString())!!,
            it.iconUrl,
            it.details
        )
    }
}
