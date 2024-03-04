package io.novafoundation.nova.feature_staking_impl.domain.recommendations.settings

import io.novafoundation.nova.common.utils.RuntimeDependent
import io.novafoundation.nova.feature_staking_impl.domain.recommendations.settings.filters.HasIdentityFilter
import io.novafoundation.nova.feature_staking_impl.domain.recommendations.settings.filters.NotBlockedFilter
import io.novafoundation.nova.feature_staking_impl.domain.recommendations.settings.filters.NotOverSubscribedFilter
import io.novafoundation.nova.feature_staking_impl.domain.recommendations.settings.filters.NotSlashedFilter
import io.novafoundation.nova.feature_staking_impl.domain.recommendations.settings.postprocessors.RemoveClusteringPostprocessor
import io.novafoundation.nova.feature_staking_impl.domain.recommendations.settings.sortings.APYSorting
import io.novasama.substrate_sdk_android.runtime.RuntimeSnapshot
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class RecommendationSettingsProvider(
    private val runtimeSnapshot: RuntimeSnapshot,
) {

    private val alwaysEnabledFilters = runtimeSnapshot.availableDependents<RecommendationFilter>(
        NotBlockedFilter,
    )

    private val customizableFilters = runtimeSnapshot.availableDependents(
        NotSlashedFilter,
        HasIdentityFilter,
        NotOverSubscribedFilter
    )

    val allAvailableFilters = alwaysEnabledFilters + customizableFilters

    val allPostProcessors = runtimeSnapshot.availableDependents(
        RemoveClusteringPostprocessor
    )

    private val customSettingsFlow = MutableStateFlow(defaultSelectCustomSettings())

    fun createModifiedCustomValidatorsSettings(
        filterIncluder: (RecommendationFilter) -> Boolean,
        postProcessorIncluder: (RecommendationPostProcessor) -> Boolean,
        sorting: RecommendationSorting? = null
    ): RecommendationSettings {
        val current = customSettingsFlow.value

        return current.copy(
            alwaysEnabledFilters = alwaysEnabledFilters,
            customEnabledFilters = customizableFilters.filter(filterIncluder),
            postProcessors = allPostProcessors.filter(postProcessorIncluder),
            sorting = sorting ?: current.sorting
        )
    }

    fun setCustomValidatorsSettings(recommendationSettings: RecommendationSettings) {
        customSettingsFlow.value = recommendationSettings
    }

    fun observeRecommendationSettings(): Flow<RecommendationSettings> = customSettingsFlow

    fun currentSettings() = customSettingsFlow.value

    fun defaultSettings(maximumValidatorsPerNominator: Int): RecommendationSettings {
        return RecommendationSettings(
            alwaysEnabledFilters = alwaysEnabledFilters,
            customEnabledFilters = customizableFilters,
            sorting = APYSorting,
            postProcessors = allPostProcessors,
            limit = maximumValidatorsPerNominator
        )
    }

    fun defaultSelectCustomSettings() = RecommendationSettings(
        alwaysEnabledFilters = alwaysEnabledFilters,
        customEnabledFilters = customizableFilters,
        sorting = APYSorting,
        postProcessors = allPostProcessors,
        limit = null
    )

    private fun <T : RuntimeDependent> RuntimeSnapshot.availableDependents(vararg candidates: T): List<T> {
        return candidates.filter { it.availableIn(this) }
    }
}
