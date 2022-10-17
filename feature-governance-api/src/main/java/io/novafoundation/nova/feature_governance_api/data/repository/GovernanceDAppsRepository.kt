package io.novafoundation.nova.feature_governance_api.data.repository

import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.ReferendumId
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain

interface ReferendumUrlConstructor {

    val baseUrl: String

    val metadataSearchUrl: String

    fun urlFor(referendumId: ReferendumId): String
}

interface GovernanceDAppsRepository {

    suspend fun getDAppUrlConstructorsFor(chain: Chain): List<ReferendumUrlConstructor>
}
