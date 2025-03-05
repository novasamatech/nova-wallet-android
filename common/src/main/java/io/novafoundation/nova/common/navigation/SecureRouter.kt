package io.novafoundation.nova.common.navigation

@Retention(AnnotationRetention.SOURCE)
annotation class PinRequired

interface SecureRouter {

    fun withPinCodeCheckRequired(
        delayedNavigation: DelayedNavigation,
        createMode: Boolean = false,
        pinCodeTitleRes: Int? = null
    )

    fun openAfterPinCode(delayedNavigation: DelayedNavigation)
}
