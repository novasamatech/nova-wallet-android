package io.novafoundation.nova.feature_assets.data.repository.assetFilters

import io.novafoundation.nova.common.data.storage.Preferences
import io.novafoundation.nova.feature_assets.domain.assets.filters.AssetFilter
import io.novafoundation.nova.feature_assets.domain.assets.filters.NonZeroBalanceFilter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

interface AssetFiltersRepository {

    val allFilters: List<AssetFilter>

    fun assetFiltersFlow(): Flow<List<AssetFilter>>

    fun updateAssetFilters(filters: List<AssetFilter>)
}

private const val PREF_ASSET_FILTERS = "ASSET_FILTERS"

class PreferencesAssetFiltersRepository(
    private val preferences: Preferences
) : AssetFiltersRepository {

    override val allFilters: List<AssetFilter> = listOf(
        NonZeroBalanceFilter
    )

    private val filterFactory = allFilters.associateBy(AssetFilter::name)

    override fun assetFiltersFlow(): Flow<List<AssetFilter>> {
        return preferences.stringFlow(PREF_ASSET_FILTERS).map { encoded ->
            encoded?.let {
                encoded.split(",").mapNotNull(filterFactory::get)
            } ?: emptyList()
        }
    }

    override fun updateAssetFilters(filters: List<AssetFilter>) {
        val encoded = filters.joinToString(separator = ",") { it.name }

        preferences.putString(PREF_ASSET_FILTERS, encoded)
    }
}
