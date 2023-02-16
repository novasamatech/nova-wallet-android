package io.novafoundation.nova.feature_versions_impl.domain

import io.novafoundation.nova.feature_versions_api.domain.UpdateNotification
import io.novafoundation.nova.feature_versions_api.domain.UpdateNotificationsInteractor
import io.novafoundation.nova.feature_versions_impl.data.VersionRepository
import kotlinx.coroutines.flow.first

class RealUpdateNotificationsInteractor(
    private val versionRepository: VersionRepository
) : UpdateNotificationsInteractor {

    override suspend fun loadVersions() {
        versionRepository.loadVersions()
    }

    override suspend fun waitPermissionToUpdate() {
        versionRepository.inAppUpdatesCheckAllowedFlow().first { allowed -> allowed }
    }

    override fun allowInAppUpdateCheck() {
        versionRepository.allowUpdate()
    }

    override suspend fun hasImportantUpdates(): Boolean {
        return versionRepository.hasImportantUpdates()
    }

    override suspend fun getUpdateNotifications(): List<UpdateNotification> {
        return versionRepository.getNewUpdateNotifications()
            .sortedByDescending { it.version }
    }

    override suspend fun skipNewUpdates() {
        versionRepository.skipCurrentUpdates()
    }
}
