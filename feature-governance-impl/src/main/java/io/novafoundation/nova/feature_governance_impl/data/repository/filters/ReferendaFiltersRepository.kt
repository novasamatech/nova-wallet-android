package io.novafoundation.nova.feature_governance_impl.data.repository.filters

import io.novafoundation.nova.feature_governance_api.domain.referendum.filters.ReferendumType
import io.novafoundation.nova.feature_governance_api.domain.referendum.filters.ReferendumTypeFilter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

private const val PREF_REFERENDUM_TYPE_FILTER = "PREF_REFERENDUM_TYPE_FILTER"

interface ReferendaFiltersRepository {

    fun getReferendumTypeFilter(): ReferendumTypeFilter

    fun observeReferendumTypeFilter(): Flow<ReferendumTypeFilter>

    fun updateReferendumTypeFilter(filter: ReferendumTypeFilter)
}

class PreferencesReferendaFiltersRepository : ReferendaFiltersRepository {

    private var referendumTypeFilter = MutableStateFlow(getDefaultReferendaTypeFilter())

    override fun getReferendumTypeFilter(): ReferendumTypeFilter {
        return referendumTypeFilter.value
    }

    override fun observeReferendumTypeFilter(): Flow<ReferendumTypeFilter> {
        return referendumTypeFilter
    }

    override fun updateReferendumTypeFilter(filter: ReferendumTypeFilter) {
        referendumTypeFilter.value = filter
    }

    private fun getDefaultReferendaTypeFilter(): ReferendumTypeFilter {
        return ReferendumTypeFilter(ReferendumType.ALL)
    }
}
