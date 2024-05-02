package io.novafoundation.nova.common.mixin.condition

import androidx.annotation.StringRes
import io.novafoundation.nova.common.presentation.DescriptiveButtonState
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.shareInBackground
import io.novafoundation.nova.common.utils.updateValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class RealConditionMixinFactory(
    private val resourceManager: ResourceManager,
) : ConditionMixinFactory {

    override fun createConditionMixin(
        coroutineScope: CoroutineScope,
        conditionsCount: Int,
        enabledButtonText: Int,
        disabledButtonText: Int,
    ): ConditionMixin {
        return RealConditionMixin(
            coroutineScope,
            resourceManager,
            conditionsCount,
            enabledButtonText,
            disabledButtonText,
        )
    }
}

class RealConditionMixin(
    private val coroutineScope: CoroutineScope,
    private val resourceManager: ResourceManager,
    private val conditionsCount: Int,
    @StringRes enabledButtonText: Int,
    @StringRes disabledButtonText: Int,
) : ConditionMixin, CoroutineScope by coroutineScope {

    private val conditionsState = MutableStateFlow(mapOf<Int, Boolean>())

    override val buttonState: Flow<DescriptiveButtonState> = conditionsState.map { conditions ->
        val allConditionsSelected = conditions.values.size == conditionsCount && conditions.values.all { it }
        when {
            allConditionsSelected -> DescriptiveButtonState.Enabled(resourceManager.getString(enabledButtonText))
            else -> DescriptiveButtonState.Disabled(resourceManager.getString(disabledButtonText))
        }
    }.shareInBackground()

    override fun checkCondition(index: Int, isChecked: Boolean) {
        conditionsState.updateValue { it + (index to isChecked) }
    }
}
