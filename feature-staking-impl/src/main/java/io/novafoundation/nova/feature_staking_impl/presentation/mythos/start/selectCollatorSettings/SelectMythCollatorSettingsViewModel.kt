package io.novafoundation.nova.feature_staking_impl.presentation.mythos.start.selectCollatorSettings

import androidx.lifecycle.viewModelScope
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.feature_staking_impl.domain.mythos.common.recommendations.MythosCollatorRecommendationConfig
import io.novafoundation.nova.feature_staking_impl.presentation.MythosStakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.ParachainStakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.mythos.start.selectCollatorSettings.model.MythCollatorRecommendationConfigParcel
import io.novafoundation.nova.feature_staking_impl.presentation.mythos.start.selectCollatorSettings.model.toDomain
import io.novafoundation.nova.feature_staking_impl.presentation.mythos.start.selectCollatorSettings.model.toParcel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class SelectMythCollatorSettingsViewModel(
    private val router: MythosStakingRouter,
    private val payload: MythCollatorRecommendationConfigParcel,
    private val selectCollatorSettingsInterScreenResponder: SelectMythCollatorSettingsInterScreenResponder,
) : BaseViewModel() {

    val selectedSortingFlow = MutableStateFlow(payload.sorting)

    private val initialConfig = payload.toDomain()

    private val modifiedConfig = selectedSortingFlow.map(::MythosCollatorRecommendationConfig)
        .share()

    val isApplyButtonEnabled = modifiedConfig.map { modified ->
        initialConfig != modified
    }.share()

    fun reset() {
        viewModelScope.launch {
            val defaultSettings = MythosCollatorRecommendationConfig.DEFAULT
            selectedSortingFlow.value = defaultSettings.sorting
        }
    }

    fun applyChanges() {
        viewModelScope.launch {
            val newConfig = modifiedConfig.first().toParcel()
            selectCollatorSettingsInterScreenResponder.respond(newConfig)

            router.back()
        }
    }

    fun backClicked() {
        router.back()
    }
}
