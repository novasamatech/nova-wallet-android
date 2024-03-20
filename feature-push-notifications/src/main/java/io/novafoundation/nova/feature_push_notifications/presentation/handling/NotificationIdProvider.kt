package io.novafoundation.nova.feature_push_notifications.presentation.handling

import io.novafoundation.nova.common.data.storage.Preferences

interface NotificationIdProvider {
    fun getId(): Int
}

class RealNotificationIdProvider(
    private val preferences: Preferences
) : NotificationIdProvider {

    override fun getId(): Int {
        val id = preferences.getInt(KEY, START_ID)
        preferences.putInt(KEY, id + 1)
        return id
    }

    companion object {
        private const val KEY = "notification_id"
        private const val START_ID = 0
    }
}
