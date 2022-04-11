package io.novafoundation.nova.feature_assets.presentation.transaction.filter

import androidx.lifecycle.Lifecycle
import io.novafoundation.nova.common.data.memory.ComputationalCache
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.TransactionFilter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

private const val FILTERS__PROVIDER_TAG = "HistoryFiltersProvider"

/**
 * Factory ensures that [HistoryFiltersProvider] scope will be limited to the current flow of screens
 */
class HistoryFiltersProviderFactory(
    private val computationalCache: ComputationalCache
) {

    suspend fun get(lifecycle: Lifecycle) = computationalCache.useCache(FILTERS__PROVIDER_TAG, lifecycle) {
        HistoryFiltersProvider()
    }
}

class HistoryFiltersProvider {
    val allFilters = TransactionFilter.values().toSet()

    val defaultFilters = allFilters

    private val customFiltersFlow = MutableStateFlow(defaultFilters)

    fun currentFilters() = customFiltersFlow.value

    fun filtersFlow(): Flow<Set<TransactionFilter>> = customFiltersFlow

    fun setCustomFilters(filters: Set<TransactionFilter>) {
        customFiltersFlow.value = filters
    }
}
