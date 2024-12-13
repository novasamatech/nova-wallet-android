package io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.alerts

class AlertModel(
    val title: CharSequence,
    val extraMessage: CharSequence,
    val type: Type
) {
    sealed class Type {
        object Info : Type()

        class CallToAction(val action: () -> Unit) : Type()
    }
}
