package io.novafoundation.nova.splash

import io.novafoundation.nova.common.navigation.SecureRouter

interface SplashRouter : SecureRouter {

    fun openWelcomeScreen()

    fun openCreatePincode()

    fun openInitialCheckPincode()
}
