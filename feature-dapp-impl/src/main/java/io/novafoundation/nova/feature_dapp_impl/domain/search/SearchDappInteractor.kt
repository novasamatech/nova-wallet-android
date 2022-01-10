package io.novafoundation.nova.feature_dapp_impl.domain.search

import io.novafoundation.nova.common.list.GroupedList
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
            .map(DappSearchResult::Dapp)

        val searchGroupContent = listOf(
            if (Urls.isValidWebUrl(query)) DappSearchResult.Url(query) else DappSearchResult.Search(query)
        )

        buildMap {
            if (dappsGroupContent.isNotEmpty()) {
                put(DappSearchGroup.DAPPS, dappsGroupContent)
            }

            put(DappSearchGroup.SEARCH, searchGroupContent)
        }
    }
}
