package io.novafoundation.nova.feature_versions_api.domain

import kotlinx.coroutines.flow.Flow

interface UpdateNotificationsInteractor {

    fun inAppUpdatesCheckAllowedFlow(): Flow<Boolean>

    suspend fun checkForUpdates()

    suspend fun getUpdateNotifications(): List<UpdateNotification>

    suspend fun skipNewUpdates()
}
