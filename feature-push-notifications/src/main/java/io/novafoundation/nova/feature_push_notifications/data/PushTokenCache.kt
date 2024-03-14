package io.novafoundation.nova.feature_push_notifications.data

import io.novafoundation.nova.common.data.storage.Preferences

private const val PUSH_TOKEN_KEY = "push_token"

interface PushTokenCache {

    fun getPushToken(): String?

    fun updatePushToken(pushToken: String?)
}

class RealPushTokenCache(
    private val preferences: Preferences
) : PushTokenCache {

    override fun getPushToken(): String? {
        return preferences.getString(PUSH_TOKEN_KEY)
    }

    override fun updatePushToken(pushToken: String?) {
        preferences.putString(PUSH_TOKEN_KEY, pushToken)
    }
}
