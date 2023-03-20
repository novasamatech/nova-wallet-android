package io.novafoundation.nova.feature_governance_impl.data.offchain.referendum.subsquare.v1

import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.ReferendumId
import io.novafoundation.nova.feature_governance_api.data.network.offchain.model.referendum.OffChainReferendumDetails
import io.novafoundation.nova.feature_governance_api.data.network.offchain.model.referendum.OffChainReferendumPreview
import io.novafoundation.nova.feature_governance_impl.data.offchain.OffChainReferendaDataSource
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain.ExternalApi.GovernanceReferenda.Source

class SubSquareV1ReferendaDataSource() : OffChainReferendaDataSource<Source.SubSquare> {

    override suspend fun referendumPreviews(baseUrl: String, options: Source.SubSquare): List<OffChainReferendumPreview> {
       return emptyList()
    }

    override suspend fun referendumDetails(referendumId: ReferendumId, baseUrl: String, options: Source.SubSquare): OffChainReferendumDetails? {
       return null
    }
}
