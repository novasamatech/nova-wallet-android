package io.novafoundation.nova.feature_governance_impl.data.repository.filters

import io.novafoundation.nova.common.data.storage.Preferences
import io.novafoundation.nova.common.utils.FilterGroup
import io.novafoundation.nova.feature_governance_api.domain.referendum.list.ReferendumPreview
import io.novafoundation.nova.feature_governance_impl.domain.filters.ReferendumType
import io.novafoundation.nova.feature_governance_impl.domain.filters.ReferendumTypeFilter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

private const val PREF_REFERENDUM_TYPE_FILTER = "PREF_REFERENDUM_TYPE_FILTER"

interface ReferendaFiltersRepository {

    fun getReferendumTypeFilter(): ReferendumTypeFilter

    fun getReferendumTypeFiltersFlow(): Flow<FilterGroup<ReferendumPreview>>

    fun updateReferendumTypeFilter(filter: ReferendumTypeFilter)
}

class PreferencesReferendaFiltersRepository(private val preferences: Preferences) : ReferendaFiltersRepository {

    private var referendumTypeFilter = MutableStateFlow(getReferendumTypeFilterFromPreferences())

    override fun getReferendumTypeFilter(): ReferendumTypeFilter {
        return referendumTypeFilter.value
    }

    override fun getReferendumTypeFiltersFlow(): Flow<FilterGroup<ReferendumPreview>> {
        return referendumTypeFilter.map { FilterGroup(listOf(it)) }
    }

    override fun updateReferendumTypeFilter(filter: ReferendumTypeFilter) {
        preferences.putString(PREF_REFERENDUM_TYPE_FILTER, filter.selected.name)
        referendumTypeFilter.value = filter
    }

    private fun getReferendumTypeFilterFromPreferences(): ReferendumTypeFilter {
        return try {
            val encoded = preferences.getString(PREF_REFERENDUM_TYPE_FILTER)
            encoded?.let { ReferendumTypeFilter(ReferendumType.valueOf(it)) }
                ?: ReferendumTypeFilter(ReferendumType.ALL)
        } catch (e: Exception) {
            preferences.putString(PREF_REFERENDUM_TYPE_FILTER, ReferendumType.ALL.name)
            ReferendumTypeFilter(ReferendumType.ALL)
        }
    }
}
