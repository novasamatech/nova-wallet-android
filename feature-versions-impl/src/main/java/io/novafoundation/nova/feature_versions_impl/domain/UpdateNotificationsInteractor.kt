package io.novafoundation.nova.feature_versions_impl.domain

import io.novafoundation.nova.feature_versions_api.domain.UpdateNotification
import io.novafoundation.nova.feature_versions_api.domain.UpdateNotificationsInteractor
import io.novafoundation.nova.feature_versions_impl.data.VersionService
import kotlinx.coroutines.flow.Flow

class RealUpdateNotificationsInteractor(
    private val versionService: VersionService
) : UpdateNotificationsInteractor {

    override fun inAppUpdatesCheckAllowedFlow(): Flow<Boolean> {
        return versionService.inAppUpdatesCheckAllowed
    }

    override suspend fun checkForUpdates() {
        versionService.checkForUpdates()
    }

    override suspend fun getUpdateNotifications(): List<UpdateNotification> {
        return versionService.getNewUpdateNotifications()
            .sortedByDescending { it.version }
    }

    override suspend fun skipNewUpdates() {
        versionService.skipCurrentUpdates()
    }
}
