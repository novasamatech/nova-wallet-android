package io.novafoundation.nova.common.utils.sequrity

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first

interface AutomaticInteractionGate {

    val isInteractionAllowedFlow: Flow<Boolean>

    fun setInteractionAllowed(allowed: Boolean)
}

@Suppress("SimplifyBooleanWithConstants")
suspend fun AutomaticInteractionGate.awaitInteractionAllowed() {
    isInteractionAllowedFlow.first { allowed -> allowed == true }
}

internal class RealAutomaticInteractionGate : AutomaticInteractionGate {

    override val isInteractionAllowedFlow = MutableStateFlow(false)

    override fun setInteractionAllowed(allowed: Boolean) {
        isInteractionAllowedFlow.value = allowed
    }
}
