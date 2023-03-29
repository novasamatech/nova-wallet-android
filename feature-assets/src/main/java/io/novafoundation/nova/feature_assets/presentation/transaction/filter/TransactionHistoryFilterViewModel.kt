package io.novafoundation.nova.feature_assets.presentation.transaction.filter

import androidx.lifecycle.viewModelScope
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.utils.checkEnabled
import io.novafoundation.nova.common.utils.filterToSet
import io.novafoundation.nova.common.utils.inBackground
import io.novafoundation.nova.common.utils.invoke
import io.novafoundation.nova.common.utils.lazyAsync
import io.novafoundation.nova.feature_assets.presentation.AssetsRouter
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.TransactionFilter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch

class TransactionHistoryFilterViewModel(
    private val router: AssetsRouter,
    private val historyFiltersProviderFactory: HistoryFiltersProviderFactory,
    private val payload: TransactionHistoryFilterPayload,
) : BaseViewModel() {

    private val historyFiltersProvider by lazyAsync {
        historyFiltersProviderFactory.get(
            scope = viewModelScope,
            chainId = payload.assetPayload.chainId,
            chainAssetId = payload.assetPayload.chainAssetId
        )
    }

    private val initialFiltersFlow = flow { emit(historyFiltersProvider().currentFilters()) }
        .share()

    val filtersEnabledMap by lazyAsync {
        createFilterEnabledMap()
    }

    private val modifiedFilters = flow {
        val inner = combine(filtersEnabledMap().values) {
            historyFiltersProvider().allAvailableFilters.filterToSet {
                filtersEnabledMap().checkEnabled(it)
            }
        }

        emitAll(inner)
    }
        .inBackground()
        .share()

    val isApplyButtonEnabled = combine(initialFiltersFlow, modifiedFilters) { initial, modified ->
        initial != modified && modified.isNotEmpty()
    }.share()

    init {
        viewModelScope.launch {
            initFromState(initialFiltersFlow.first())
        }
    }

    private fun initFromState(currentState: Set<TransactionFilter>) = launch {
        filtersEnabledMap().forEach { (filter, checked) ->
            checked.value = filter in currentState
        }
    }

    fun resetFilter() {
        viewModelScope.launch {
            val defaultFilters = historyFiltersProvider().defaultFilters

            initFromState(defaultFilters)
        }
    }

    fun backClicked() {
        router.back()
    }

    private suspend fun createFilterEnabledMap(): Map<TransactionFilter, MutableStateFlow<Boolean>> {
        return historyFiltersProvider()
            .allAvailableFilters
            .associateWith { MutableStateFlow(true) }
    }

    fun applyClicked() {
        viewModelScope.launch {
            historyFiltersProvider().setCustomFilters(modifiedFilters.first())

            router.back()
        }
    }
}
