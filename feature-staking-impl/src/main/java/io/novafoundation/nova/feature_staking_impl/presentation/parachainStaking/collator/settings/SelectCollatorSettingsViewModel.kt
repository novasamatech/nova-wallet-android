package io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.collator.settings

import androidx.lifecycle.viewModelScope
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.common.recommendations.CollatorRecommendationConfig
import io.novafoundation.nova.feature_staking_impl.presentation.ParachainStakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.collator.settings.SelectCollatorSettingsInterScreenCommunicator.Response
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.collator.settings.model.CollatorRecommendationConfigParcelModel
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.collator.settings.model.mapCollatorRecommendationConfigFromParcel
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.collator.settings.model.mapCollatorRecommendationConfigToParcel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class SelectCollatorSettingsViewModel(
    private val router: ParachainStakingRouter,
    private val payload: CollatorRecommendationConfigParcelModel,
    private val selectCollatorSettingsInterScreenResponder: SelectCollatorSettingsInterScreenResponder,
) : BaseViewModel() {

    val selectedSortingFlow = MutableStateFlow(CollatorRecommendationConfig.DEFAULT.sorting)

    private val initialConfig = mapCollatorRecommendationConfigFromParcel(payload)

    private val modifiedConfig = selectedSortingFlow.map(::CollatorRecommendationConfig)
        .share()

    val isApplyButtonEnabled = modifiedConfig.map { modified ->
        initialConfig != modified
    }.share()

    init {
        setFromSettings(initialConfig)
    }

    fun reset() {
        viewModelScope.launch {
            val defaultSettings = CollatorRecommendationConfig.DEFAULT

            setFromSettings(defaultSettings)
        }
    }

    fun applyChanges() {
        viewModelScope.launch {
            val newConfig = mapCollatorRecommendationConfigToParcel(modifiedConfig.first())
            val response = Response(newConfig)

            selectCollatorSettingsInterScreenResponder.respond(response)

            router.back()
        }
    }

    fun backClicked() {
        router.back()
    }

    private fun setFromSettings(currentSettings: CollatorRecommendationConfig) {
        selectedSortingFlow.value = currentSettings.sorting
    }
}
