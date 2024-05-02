package io.novafoundation.nova.common.mixin.condition

import androidx.annotation.StringRes
import io.novafoundation.nova.common.presentation.DescriptiveButtonState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow

interface ConditionMixin {

    val buttonState: Flow<DescriptiveButtonState>

    fun checkCondition(index: Int, isChecked: Boolean)
}

interface ConditionMixinFactory {

    fun createConditionMixin(
        coroutineScope: CoroutineScope,
        conditionsCount: Int,
        @StringRes enabledButtonText: Int,
        @StringRes disabledButtonText: Int,
    ): ConditionMixin
}
