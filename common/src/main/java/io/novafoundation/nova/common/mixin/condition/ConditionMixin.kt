package io.novafoundation.nova.common.mixin.condition

import io.novafoundation.nova.common.presentation.DescriptiveButtonState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

interface ConditionMixinFactory {

    fun createConditionMixin(
        coroutineScope: CoroutineScope,
        conditionsCount: Int
    ): ConditionMixin
}

interface ConditionMixin {

    val allConditionsSatisfied: Flow<Boolean>

    fun checkCondition(index: Int, isChecked: Boolean)
}

fun ConditionMixin.buttonState(enabledState: String, disabledState: String): Flow<DescriptiveButtonState> {
    return allConditionsSatisfied.map { satisfied ->
        when (satisfied) {
            true -> DescriptiveButtonState.Enabled(enabledState)
            false -> DescriptiveButtonState.Disabled(disabledState)
        }
    }
}
