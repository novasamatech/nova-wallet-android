package io.novafoundation.nova.common.mixin.condition

import io.novafoundation.nova.common.utils.CheckableListener

fun ConditionMixin.setupConditions(vararg conditionInputs: CheckableListener) {
    conditionInputs.forEachIndexed { index, conditionInput ->
        conditionInput.setOnCheckedChangeListener { _, isChecked ->
            checkCondition(index, isChecked)
        }
    }
}
