package io.novafoundation.nova.feature_governance_impl.data.repository

import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.ReferendumId
import io.novafoundation.nova.feature_governance_api.data.network.offchain.model.referendum.OffChainReferendumDetails
import io.novafoundation.nova.feature_governance_api.data.network.offchain.model.referendum.OffChainReferendumPreview
import io.novafoundation.nova.feature_governance_api.data.repository.OffChainReferendaInfoRepository
import io.novafoundation.nova.feature_governance_impl.data.offchain.OffChainReferendaDataSource
import io.novafoundation.nova.runtime.ext.externalApi
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain.ExternalApi.GovernanceReferenda.Source

class MultiSourceOffChainReferendaInfoRepository(
    private val subSquareReferendaDataSource: OffChainReferendaDataSource<Source.SubSquare>,
    private val polkassemblyReferendaDataSource: OffChainReferendaDataSource<Source.Polkassembly>
) : OffChainReferendaInfoRepository {

    override suspend fun referendumPreviews(chain: Chain): List<OffChainReferendumPreview> {
        return runCatching {
            val dataSource = chain.carriedGovernanceDataSource() ?: return emptyList()

            dataSource.referendumPreviews()
        }.getOrDefault(emptyList())
    }

    override suspend fun referendumDetails(referendumId: ReferendumId, chain: Chain): OffChainReferendumDetails? {
        return runCatching {
            val dataSource = chain.carriedGovernanceDataSource() ?: return null

            dataSource.referendumDetails(referendumId)
        }.getOrNull()
    }

    private fun Chain.carriedGovernanceDataSource(): OffChainReferendaDataSourceCarried<*>? {
        val governanceApi = externalApi<Chain.ExternalApi.GovernanceReferenda>() ?: return null
        val baseUrl = governanceApi.url

        return when (val source = governanceApi.source) {
            is Source.Polkassembly -> OffChainReferendaDataSourceCarried(polkassemblyReferendaDataSource, baseUrl, source)
            is Source.SubSquare -> OffChainReferendaDataSourceCarried(subSquareReferendaDataSource, baseUrl, source)
        }
    }

    private class OffChainReferendaDataSourceCarried<O>(
        private val dataSource: OffChainReferendaDataSource<O>,
        private val baseUrl: String,
        private val options: O
    ) {
        suspend fun referendumPreviews(): List<OffChainReferendumPreview> = dataSource.referendumPreviews(baseUrl, options)

        suspend fun referendumDetails(referendumId: ReferendumId): OffChainReferendumDetails? = dataSource.referendumDetails(referendumId, baseUrl, options)
    }
}
