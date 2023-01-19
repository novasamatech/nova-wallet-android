package io.novafoundation.nova.feature_versions_impl.domain

import io.novafoundation.nova.feature_versions_api.domain.UpdateNotification
import io.novafoundation.nova.feature_versions_api.domain.UpdateNotificationsInteractor
import io.novafoundation.nova.feature_versions_impl.data.VersionService

class RealUpdateNotificationsInteractor(
    private val versionService: VersionService
) : UpdateNotificationsInteractor {

    override suspend fun hasUpdateNotifications(): Boolean {
        return versionService.hasNewVersions()
    }

    override suspend fun getUpdateNotifications(): List<UpdateNotification> {
        return versionService.getNewVersions()
    }

    override fun hideNotificationsForCurrentVersion() {
        versionService.saveVersionCheckpoint()
    }
}
