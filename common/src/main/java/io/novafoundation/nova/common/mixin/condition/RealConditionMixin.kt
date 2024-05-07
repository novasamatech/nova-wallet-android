package io.novafoundation.nova.common.mixin.condition

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
        conditionsCount: Int
    ): ConditionMixin {
        return RealConditionMixin(
            coroutineScope,
            resourceManager,
            conditionsCount
        )
    }
}

class RealConditionMixin(
    private val coroutineScope: CoroutineScope,
    private val resourceManager: ResourceManager,
    private val conditionsCount: Int
) : ConditionMixin, CoroutineScope by coroutineScope {

    private val conditionsState = MutableStateFlow(mapOf<Int, Boolean>())

    override val allConditionsSatisfied: Flow<Boolean> = conditionsState.map { conditions ->
        conditions.values.size == conditionsCount && conditions.values.all { it }
    }.shareInBackground()

    override fun checkCondition(index: Int, isChecked: Boolean) {
        conditionsState.updateValue { it + (index to isChecked) }
    }
}
