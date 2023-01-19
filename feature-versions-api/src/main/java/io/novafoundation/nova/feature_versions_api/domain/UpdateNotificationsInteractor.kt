package io.novafoundation.nova.feature_versions_api.domain

interface UpdateNotificationsInteractor {

    suspend fun hasUpdateNotifications(): Boolean

    suspend fun getUpdateNotifications(): List<UpdateNotification>

    fun hideNotificationsForCurrentVersion()
}
