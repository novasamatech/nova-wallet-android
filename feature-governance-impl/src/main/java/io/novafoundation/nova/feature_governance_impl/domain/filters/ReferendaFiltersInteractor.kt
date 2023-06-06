package io.novafoundation.nova.feature_governance_impl.domain.filters

import io.novafoundation.nova.feature_governance_api.domain.referendum.filters.ReferendumTypeFilter
import io.novafoundation.nova.feature_governance_impl.data.repository.filters.ReferendaFiltersRepository
import kotlinx.coroutines.flow.Flow

interface ReferendaFiltersInteractor {

    fun getReferendumTypeFilter(): ReferendumTypeFilter

    fun observeReferendumTypeFilter(): Flow<ReferendumTypeFilter>

    fun updateReferendumTypeFilter(filter: ReferendumTypeFilter)
}

class RealReferendaFiltersInteractor(
    private val referendaFiltersRepository: ReferendaFiltersRepository
) : ReferendaFiltersInteractor {

    override fun getReferendumTypeFilter(): ReferendumTypeFilter {
        return referendaFiltersRepository.getReferendumTypeFilter()
    }

    override fun observeReferendumTypeFilter(): Flow<ReferendumTypeFilter> {
        return referendaFiltersRepository.observeReferendumTypeFilter()
    }

    override fun updateReferendumTypeFilter(filter: ReferendumTypeFilter) {
        referendaFiltersRepository.updateReferendumTypeFilter(filter)
    }
}
