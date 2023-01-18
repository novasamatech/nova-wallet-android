package io.novafoundation.nova.feature_update_notification_impl.domain

interface UpdateNotificationsInteractor {

    fun hasUpdateNotifications(): Boolean

    fun getUpdateNotifications(): List<UpdateNotification>

    fun hideNotificationsForCurrentVersion()
}

class RealUpdateNotificationsInteractor() : UpdateNotificationsInteractor {

    override fun hasUpdateNotifications(): Boolean {

    }

    override fun getUpdateNotifications(): List<UpdateNotification> {

    }

    override fun hideNotificationsForCurrentVersion() {

    }
}
