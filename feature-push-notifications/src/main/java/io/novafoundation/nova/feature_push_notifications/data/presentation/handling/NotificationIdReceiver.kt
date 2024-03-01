package io.novafoundation.nova.feature_push_notifications.data.presentation.handling

import io.novafoundation.nova.common.data.storage.Preferences

interface NotificationIdReceiver {
    fun getId(): Int
}

class RealNotificationIdReceiver(
    private val preferences: Preferences
) : NotificationIdReceiver {

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
