package io.novafoundation.nova.feature_governance_impl.data.repository.filters

import io.novafoundation.nova.common.data.storage.Preferences
import io.novafoundation.nova.feature_governance_api.domain.referendum.filters.ReferendumType
import io.novafoundation.nova.feature_governance_api.domain.referendum.filters.ReferendumTypeFilter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

private const val PREF_REFERENDUM_TYPE_FILTER = "PREF_REFERENDUM_TYPE_FILTER"

interface ReferendaFiltersRepository {

    fun getReferendumTypeFilter(): ReferendumTypeFilter

    fun observeReferendumTypeFilter(): Flow<ReferendumTypeFilter>

    fun updateReferendumTypeFilter(filter: ReferendumTypeFilter)
}

class PreferencesReferendaFiltersRepository(private val preferences: Preferences) : ReferendaFiltersRepository {

    private var referendumTypeFilter = MutableStateFlow(getReferendumTypeFilterFromPreferences())

    override fun getReferendumTypeFilter(): ReferendumTypeFilter {
        return referendumTypeFilter.value
    }

    override fun observeReferendumTypeFilter(): Flow<ReferendumTypeFilter> {
        return preferences.stringFlow(PREF_REFERENDUM_TYPE_FILTER).map {
            mapFilterStringToModel(it)
        }
    }

    override fun updateReferendumTypeFilter(filter: ReferendumTypeFilter) {
        preferences.putString(PREF_REFERENDUM_TYPE_FILTER, filter.selectedType.name)
        referendumTypeFilter.value = filter
    }

    private fun getReferendumTypeFilterFromPreferences(): ReferendumTypeFilter {
        val encoded = preferences.getString(PREF_REFERENDUM_TYPE_FILTER)
        return mapFilterStringToModel(encoded)
    }

    private fun mapFilterStringToModel(filter: String?): ReferendumTypeFilter {
        return try {
            filter?.let { ReferendumTypeFilter(ReferendumType.valueOf(it)) }
                ?: ReferendumTypeFilter(ReferendumType.ALL)
        } catch (e: Exception) {
            preferences.putString(PREF_REFERENDUM_TYPE_FILTER, ReferendumType.ALL.name)
            ReferendumTypeFilter(ReferendumType.ALL)
        }
    }
}
