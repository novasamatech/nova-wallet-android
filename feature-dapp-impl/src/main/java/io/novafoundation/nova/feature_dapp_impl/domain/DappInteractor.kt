package io.novafoundation.nova.feature_dapp_impl.domain

import io.novafoundation.nova.common.list.GroupedList
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_dapp_api.data.model.DappCategory
import io.novafoundation.nova.feature_dapp_api.data.model.DappMetadata
import io.novafoundation.nova.feature_dapp_api.data.repository.DAppMetadataRepository
import io.novafoundation.nova.feature_dapp_impl.R
import io.novafoundation.nova.feature_dapp_impl.domain.browser.DAppInfo
import io.novafoundation.nova.feature_dapp_impl.util.UrlNormalizer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DappInteractor(
    private val dAppMetadataRepository: DAppMetadataRepository,
    private val resourceManager: ResourceManager,
) {

    suspend fun syncDAppMetadatas() = withContext(Dispatchers.Default) {
        dAppMetadataRepository.syncDAppMetadatas()
    }

    @OptIn(ExperimentalStdlibApi::class)
    suspend fun getDAppMetadatasByCategory(): GroupedList<DappCategory, DappMetadata> {
        val allDApps = dAppMetadataRepository.getDAppMetadatas()
        val allCategories = allDApps.flatMap { it.categories }

        // Regrouping in O(Categories * Dapps)
        // Complexity should be fine for expected amount of dApps
        val derivedCategories = allCategories.associateWith { category ->
            allDApps.filter { category in it.categories }
        }

        val categoryAll = DappCategory(
            id = "all",
            name = resourceManager.getString(R.string.common_all)
        )

        return buildMap {
            put(categoryAll, allDApps)
            putAll(derivedCategories)
        }
    }

    suspend fun getDAppInfo(dAppUrl: String): DAppInfo {
        val baseUrl = UrlNormalizer.normalizeUrl(dAppUrl)

        return withContext(Dispatchers.Default) {
            DAppInfo(
                baseUrl = baseUrl,
                metadata = dAppMetadataRepository.getDAppMetadata(baseUrl)
            )
        }
    }
}
