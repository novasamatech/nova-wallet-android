package io.novafoundation.nova.feature_dapp_impl.domain.search

import io.novafoundation.nova.common.list.GroupedList
import io.novafoundation.nova.feature_dapp_api.data.model.DappMetadata
import io.novafoundation.nova.feature_dapp_api.data.repository.DAppMetadataRepository
import io.novafoundation.nova.feature_dapp_impl.util.Urls
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SearchDappInteractor(
    private val dAppMetadataRepository: DAppMetadataRepository,
) {

    @OptIn(ExperimentalStdlibApi::class)
    suspend fun searchDapps(query: String): GroupedList<DappSearchGroup, DappSearchResult> = withContext(Dispatchers.Default) {
        val allDapps = dAppMetadataRepository.getDAppMetadatas()

        val dappsGroupContent = allDapps.filter { query.isEmpty() || query.lowercase() in it.name.lowercase() }
            .sortedBy(DappMetadata::name)
            .map(DappSearchResult::Dapp)

        val searchGroupContent = when {
            query.isEmpty() -> null
            Urls.isValidWebUrl(query) -> DappSearchResult.Url(Urls.ensureHttpsProtocol(query))
            else -> DappSearchResult.Search(query, searchUrlFor(query))
        }

        buildMap {
            searchGroupContent?.let {
                put(DappSearchGroup.SEARCH, listOf(searchGroupContent))
            }

            if (dappsGroupContent.isNotEmpty()) {
                put(DappSearchGroup.DAPPS, dappsGroupContent)
            }
        }
    }

    private fun searchUrlFor(query: String): String = "https://duckduckgo.com/?q=$query"
}
