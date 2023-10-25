package io.novafoundation.nova.common.view

interface ValidatableInputField {

    fun showError(error: String)

    fun hideError()
}
