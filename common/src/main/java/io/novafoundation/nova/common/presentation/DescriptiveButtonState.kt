package io.novafoundation.nova.common.presentation

sealed class DescriptiveButtonState {

    data class Enabled(val action: String) : DescriptiveButtonState()

    data class Disabled(val reason: String) : DescriptiveButtonState()

    object Loading : DescriptiveButtonState()

    object Gone : DescriptiveButtonState()

    object Invisible : DescriptiveButtonState()
}

fun DescriptiveButtonState.textOrNull(): String? {
    return when (this) {
        is DescriptiveButtonState.Enabled -> action
        is DescriptiveButtonState.Disabled -> reason
        DescriptiveButtonState.Gone,
        DescriptiveButtonState.Invisible,
        DescriptiveButtonState.Loading -> null
    }
}
