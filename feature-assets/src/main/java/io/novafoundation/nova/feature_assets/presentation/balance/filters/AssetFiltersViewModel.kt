package io.novafoundation.nova.feature_assets.presentation.balance.filters

import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.utils.checkEnabled
import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.common.utils.inBackground
import io.novafoundation.nova.feature_assets.domain.assets.filters.AssetFilter
import io.novafoundation.nova.feature_assets.domain.assets.filters.AssetFiltersInteractor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.launch

class AssetFiltersViewModel(
    private val interactor: AssetFiltersInteractor,
) : BaseViewModel() {

    private val initialFilters = flowOf { interactor.currentFilters() }
        .inBackground()
        .share()

    val filtersEnabledMap = createFilterEnabledMap()

    init {
        applyInitialState()
        filtersEnabledMap.applyOnChange()
    }

    private fun applyInitialState() = launch {
        val initialFilters = initialFilters.first()

        filtersEnabledMap.forEach { (filter, checked) ->
            checked.value = filter in initialFilters
        }
    }

    private fun createFilterEnabledMap() = interactor.allFilters.associateWith { MutableStateFlow(false) }

    private fun Map<AssetFilter, MutableStateFlow<Boolean>>.applyOnChange() {
        var isFirstEmmition = true
        combine(this.values) {
            if (isFirstEmmition) {
                isFirstEmmition = false
            } else {
                applyChanges()
            }
        }
            .launchIn(this@AssetFiltersViewModel)
    }

    private fun applyChanges() {
        val filters = interactor.allFilters.filter(filtersEnabledMap::checkEnabled)
        interactor.updateFilters(filters)
    }
}
