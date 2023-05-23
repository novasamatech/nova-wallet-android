package io.novafoundation.nova.feature_governance_impl.domain.filters

import io.novafoundation.nova.common.utils.MatchAllFilter
import io.novafoundation.nova.feature_governance_api.domain.referendum.list.ReferendumPreview
import io.novafoundation.nova.feature_governance_impl.data.repository.filters.ReferendaFiltersRepository
import kotlinx.coroutines.flow.Flow

interface ReferendaFiltersInteractor {

    fun getReferendumTypeFilter(): ReferendumTypeFilter

    fun getReferendumTypeFiltersFlow(): Flow<MatchAllFilter<ReferendumPreview>>

    fun observeReferendumTypeFilter(): Flow<ReferendumTypeFilter>

    fun updateReferendumTypeFilter(filter: ReferendumTypeFilter)
}

class RealReferendaFiltersInteractor(
    private val referendaFiltersRepository: ReferendaFiltersRepository
) : ReferendaFiltersInteractor {

    override fun getReferendumTypeFilter(): ReferendumTypeFilter {
        return referendaFiltersRepository.getReferendumTypeFilter()
    }

    override fun getReferendumTypeFiltersFlow(): Flow<MatchAllFilter<ReferendumPreview>> {
        return referendaFiltersRepository.getReferendumTypeFiltersFlow()
    }

    override fun observeReferendumTypeFilter(): Flow<ReferendumTypeFilter> {
        return referendaFiltersRepository.observeReferenumTypeFilter()
    }

    override fun updateReferendumTypeFilter(filter: ReferendumTypeFilter) {
        referendaFiltersRepository.updateReferendumTypeFilter(filter)
    }
}
