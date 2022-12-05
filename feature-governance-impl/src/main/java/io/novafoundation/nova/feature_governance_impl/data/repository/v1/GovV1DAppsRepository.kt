package io.novafoundation.nova.feature_governance_impl.data.repository.v1

import io.novafoundation.nova.common.utils.formatNamed
import io.novafoundation.nova.core_db.dao.GovernanceDAppsDao
import io.novafoundation.nova.core_db.model.GovernanceDAppLocal
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.ReferendumId
import io.novafoundation.nova.feature_governance_api.data.repository.GovernanceDAppsRepository
import io.novafoundation.nova.feature_governance_api.domain.referendum.details.ReferendumDApp
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class GovV1DAppsRepository(
    private val governanceDAppsDao: GovernanceDAppsDao
) : GovernanceDAppsRepository {

    override fun observeReferendumDApps(chainId: ChainId, referendumId: ReferendumId): Flow<List<ReferendumDApp>> {
        return governanceDAppsDao.observeChainDapps(chainId)
            .map { dapps ->
                val v1Dapps = dapps.filter { it.referendumUrlV1 != null }
                mapV1DappsLocalToDomain(referendumId, v1Dapps)
            }
    }
}

private fun mapV1DappsLocalToDomain(referendumId: ReferendumId, dapps: List<GovernanceDAppLocal>): List<ReferendumDApp> {
    return dapps.map {
        ReferendumDApp(
            it.chainId,
            it.name,
            it.referendumUrlV1?.formatNamed("referendumId" to referendumId.value.toString())!!,
            it.iconUrl,
            it.details
        )
    }
}
