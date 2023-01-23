package io.novafoundation.nova.feature_versions_api.domain

interface UpdateNotificationsInteractor {

    suspend fun hasImportantUpdates(): Boolean

    suspend fun getUpdateNotifications(): List<UpdateNotification>

    suspend fun skipNewUpdates()
}
