package io.novafoundation.nova.feature_governance_impl.data.dapps.remote

import io.novafoundation.nova.feature_governance_impl.BuildConfig
import io.novafoundation.nova.feature_governance_impl.data.dapps.remote.model.GovernanceChainDappsRemote
import retrofit2.http.GET

interface GovernanceDappsFetcher {

    @GET(BuildConfig.GOVERNANCE_DAPPS_URL)
    suspend fun getDapps(): List<GovernanceChainDappsRemote>
}
