package io.novafoundation.nova.common.presentation

sealed class DescriptiveButtonState {

    data class Enabled(val action: String) : DescriptiveButtonState()

    data class Disabled(val reason: String) : DescriptiveButtonState()

    object Loading : DescriptiveButtonState()

    object Gone : DescriptiveButtonState()
}
