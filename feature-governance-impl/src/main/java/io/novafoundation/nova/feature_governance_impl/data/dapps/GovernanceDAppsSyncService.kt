package io.novafoundation.nova.feature_governance_impl.data.dapps

import io.novafoundation.nova.common.utils.retryUntilDone
import io.novafoundation.nova.core_db.dao.GovernanceDAppsDao
import io.novafoundation.nova.core_db.model.GovernanceDAppLocal
import io.novafoundation.nova.feature_governance_impl.data.dapps.remote.GovernanceDappsFetcher
import io.novafoundation.nova.feature_governance_impl.data.dapps.remote.model.GovernanceChainDappsRemote
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GovernanceDAppsSyncService(
    private val dao: GovernanceDAppsDao,
    private val dappsFetcher: GovernanceDappsFetcher
) {

    suspend fun syncDapps() = withContext(Dispatchers.Default) {
        val newDapps = retryUntilDone {
            mapRemoteDappsToLocal(dappsFetcher.getDapps())
        }
        dao.update(newDapps)
    }

    private fun mapRemoteDappsToLocal(
        chainDapps: List<GovernanceChainDappsRemote>
    ): List<GovernanceDAppLocal> {
        return chainDapps.flatMap { chainWithDapp ->
            chainWithDapp.dapps.map {
                GovernanceDAppLocal(
                    chainWithDapp.chainId,
                    it.title,
                    it.urlV1,
                    it.urlV2,
                    it.icon,
                    it.details
                )
            }
        }
    }
}
