package io.novafoundation.nova.feature_push_notifications.domain.interactor

import io.novafoundation.nova.common.data.storage.Preferences

interface WelcomePushNotificationsInteractor {
    fun needToShowWelcomeScreen(): Boolean

    fun setWelcomeScreenShown()
}

class RealWelcomePushNotificationsInteractor(
    private val preferences: Preferences
) : WelcomePushNotificationsInteractor {

    override fun needToShowWelcomeScreen(): Boolean {
        return preferences.getBoolean(PREFS_WELCOME_SCREEN_SHOWN, true)
    }

    override fun setWelcomeScreenShown() {
        return preferences.putBoolean(PREFS_WELCOME_SCREEN_SHOWN, false)
    }

    companion object {
        private const val PREFS_WELCOME_SCREEN_SHOWN = "welcome_screen_shown"
    }
}
