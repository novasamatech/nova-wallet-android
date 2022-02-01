package io.novafoundation.nova.feature_wallet_impl.domain.assets.filters

import io.novafoundation.nova.feature_wallet_impl.data.repository.assetFilters.AssetFiltersRepository
import kotlinx.coroutines.flow.first

class AssetFiltersInteractor(
    private val assetFiltersRepository: AssetFiltersRepository
) {

    val allFilters = assetFiltersRepository.allFilters

    fun updateFilters(filters: List<AssetFilter>) {
        assetFiltersRepository.updateAssetFilters(filters)
    }

    suspend fun currentFilters(): Set<AssetFilter> = assetFiltersRepository.assetFiltersFlow().first().toSet()
}
