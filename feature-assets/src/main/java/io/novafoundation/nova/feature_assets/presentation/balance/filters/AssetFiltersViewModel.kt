package io.novafoundation.nova.feature_assets.presentation.balance.filters

import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.utils.checkEnabled
import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.common.utils.inBackground
import io.novafoundation.nova.feature_assets.domain.assets.filters.AssetFiltersInteractor
import io.novafoundation.nova.feature_assets.presentation.WalletRouter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class AssetFiltersViewModel(
    private val interactor: AssetFiltersInteractor,
    private val router: WalletRouter,
) : BaseViewModel() {

    private val initialFilters = flowOf { interactor.currentFilters() }
        .inBackground()
        .share()

    val filtersEnabledMap = createFilterEnabledMap()

    init {
        applyInitialState()
    }

    fun backClicked() {
        router.back()
    }

    fun applyClicked() = launch {
        val filters = interactor.allFilters.filter(filtersEnabledMap::checkEnabled)

        interactor.updateFilters(filters)

        router.back()
    }

    private fun applyInitialState() = launch {
        val initialFilters = initialFilters.first()

        filtersEnabledMap.forEach { (filter, checked) ->
            checked.value = filter in initialFilters
        }
    }

    private fun createFilterEnabledMap() = interactor.allFilters.associateWith { MutableStateFlow(false) }
}
