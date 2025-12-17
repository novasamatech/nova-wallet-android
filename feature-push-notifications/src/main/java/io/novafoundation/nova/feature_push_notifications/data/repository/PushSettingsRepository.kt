package io.novafoundation.nova.feature_push_notifications.data.repository

import io.novafoundation.nova.common.data.storage.Preferences

interface PushSettingsRepository {
    fun isMultisigsWasEnabledFirstTime(): Boolean

    fun setMultisigsWasEnabledFirstTime()
}

private const val IS_MULTISIG_WAS_ENABLED_FIRST_TIME = "IS_MULTISIG_WAS_ENABLED_FIRST_TIME"

class RealPushSettingsRepository(
    private val preferences: Preferences
) : PushSettingsRepository {
    override fun isMultisigsWasEnabledFirstTime(): Boolean {
        return preferences.getBoolean(IS_MULTISIG_WAS_ENABLED_FIRST_TIME, false)
    }

    override fun setMultisigsWasEnabledFirstTime() {
        preferences.putBoolean(IS_MULTISIG_WAS_ENABLED_FIRST_TIME, true)
    }
}
