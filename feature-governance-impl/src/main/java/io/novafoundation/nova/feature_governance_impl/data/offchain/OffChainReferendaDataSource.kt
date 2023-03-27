package io.novafoundation.nova.feature_governance_impl.data.offchain

import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.ReferendumId
import io.novafoundation.nova.feature_governance_api.data.network.offchain.model.referendum.OffChainReferendumDetails
import io.novafoundation.nova.feature_governance_api.data.network.offchain.model.referendum.OffChainReferendumPreview

interface OffChainReferendaDataSource<O> {

    suspend fun referendumPreviews(baseUrl: String, options: O): List<OffChainReferendumPreview>

    suspend fun referendumDetails(referendumId: ReferendumId, baseUrl: String, options: O): OffChainReferendumDetails?
}
