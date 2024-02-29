package io.novafoundation.nova.feature_governance_impl.presentation.referenda.filters

import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.utils.inBackground
import io.novafoundation.nova.common.utils.reversed
import io.novafoundation.nova.feature_governance_impl.R
import io.novafoundation.nova.feature_governance_impl.domain.filters.ReferendaFiltersInteractor
import io.novafoundation.nova.feature_governance_api.domain.referendum.filters.ReferendumType
import io.novafoundation.nova.feature_governance_api.domain.referendum.filters.ReferendumTypeFilter
import io.novafoundation.nova.feature_governance_api.presentation.GovernanceRouter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

private val REFERENDUM_TYPE_FILTERS = mapOf(
    R.id.referendaFilterAll to ReferendumType.ALL,
    R.id.referendaFilterNotVoted to ReferendumType.NOT_VOTED,
    R.id.referendaFilterVoted to ReferendumType.VOTED
)

private val REFERENDUM_TYPE_FILTERS_REVERSE = REFERENDUM_TYPE_FILTERS.reversed()

class ReferendaFiltersViewModel(
    private val interactor: ReferendaFiltersInteractor,
    private val governanceRouter: GovernanceRouter
) : BaseViewModel() {

    private var selectedFilterFlow = MutableStateFlow(interactor.getReferendumTypeFilter().selectedType)

    private val initialTypeFilter = interactor.getReferendumTypeFilter()

    val isApplyButtonAvailableFlow = selectedFilterFlow.map { selectedFilter ->
        selectedFilter != interactor.getReferendumTypeFilter().selectedType
    }.inBackground()
        .share()

    fun getReferendumTypeSelectedOption(): Int {
        return REFERENDUM_TYPE_FILTERS_REVERSE.getValue(initialTypeFilter.selectedType)
    }

    fun onFilterTypeChanged(checkedId: Int) {
        selectedFilterFlow.value = REFERENDUM_TYPE_FILTERS.getValue(checkedId)
    }

    fun onApplyFilters() {
        interactor.updateReferendumTypeFilter(ReferendumTypeFilter(selectedFilterFlow.value))
        governanceRouter.back()
    }

    fun homeButtonClicked() {
        governanceRouter.back()
    }
}
