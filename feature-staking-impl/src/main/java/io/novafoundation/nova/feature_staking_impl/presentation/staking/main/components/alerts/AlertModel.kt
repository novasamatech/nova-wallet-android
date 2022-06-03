package io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.alerts

class AlertModel(
    val title: String,
    val extraMessage: String,
    val type: Type
) {
    sealed class Type {
        object Info : Type()

        class CallToAction(val action: () -> Unit) : Type()
    }
}
