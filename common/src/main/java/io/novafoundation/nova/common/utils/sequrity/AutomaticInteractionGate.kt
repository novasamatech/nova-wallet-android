package io.novafoundation.nova.common.utils.sequrity

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn

interface AutomaticInteractionGate {

    val isInteractionAllowedFlow: Flow<Boolean>

    fun isInteractionAllowed(): Boolean

    fun initialPinPassed()

    fun wentToBackground()

    fun foregroundCheckPassed()
}

@Suppress("SimplifyBooleanWithConstants")
suspend fun AutomaticInteractionGate.awaitInteractionAllowed() {
    isInteractionAllowedFlow.first { allowed -> allowed == true }
}

internal class RealAutomaticInteractionGate : AutomaticInteractionGate, CoroutineScope by CoroutineScope(Dispatchers.Default) {

    private val initialPinPassed = MutableStateFlow(false)
    private val backgroundCheckPassed = MutableStateFlow(false)

    override val isInteractionAllowedFlow = combine(initialPinPassed, backgroundCheckPassed) { initialCheck, backgroundCheck ->
        isInteractionAllowed(initialPinPassed = initialCheck, backgroundCheckPassed = backgroundCheck)
    }
        .stateIn(this, SharingStarted.Eagerly, initialValue = false)

    override fun initialPinPassed() {
        initialPinPassed.value = true
    }

    override fun wentToBackground() {
        backgroundCheckPassed.value = false
    }

    override fun foregroundCheckPassed() {
        backgroundCheckPassed.value = true
    }

    override fun isInteractionAllowed(): Boolean {
        return isInteractionAllowed(initialPinPassed = initialPinPassed.value, backgroundCheckPassed = backgroundCheckPassed.value)
    }

    private fun isInteractionAllowed(initialPinPassed: Boolean, backgroundCheckPassed: Boolean): Boolean {
        return initialPinPassed && backgroundCheckPassed
    }
}
