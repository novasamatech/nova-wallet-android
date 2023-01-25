package io.novafoundation.nova.feature_versions_impl.domain

import io.novafoundation.nova.feature_versions_api.domain.UpdateNotification
import io.novafoundation.nova.feature_versions_api.domain.UpdateNotificationsInteractor
import io.novafoundation.nova.feature_versions_impl.data.VersionService
import kotlinx.coroutines.flow.first

class RealUpdateNotificationsInteractor(
    private val versionService: VersionService
) : UpdateNotificationsInteractor {

    override suspend fun waitPermissionToUpdate() {
        versionService.inAppUpdatesCheckAllowed.first { allowed -> allowed }
    }

    override fun allowInAppUpdateCheck() {
        versionService.allowUpdate()
    }

    override suspend fun hasImportantUpdates(): Boolean {
        return versionService.hasImportantUpdates()
    }

    override suspend fun getUpdateNotifications(): List<UpdateNotification> {
        return versionService.getNewUpdateNotifications()
            .sortedByDescending { it.version }
    }

    override suspend fun skipNewUpdates() {
        versionService.skipCurrentUpdates()
    }
}
