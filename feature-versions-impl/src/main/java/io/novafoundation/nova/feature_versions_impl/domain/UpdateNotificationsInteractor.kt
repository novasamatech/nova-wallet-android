package io.novafoundation.nova.feature_versions_impl.domain

import io.novafoundation.nova.feature_versions_api.domain.UpdateNotification
import io.novafoundation.nova.feature_versions_api.domain.UpdateNotificationsInteractor
import io.novafoundation.nova.feature_versions_impl.data.VersionService

class RealUpdateNotificationsInteractor(
    private val versionService: VersionService
) : UpdateNotificationsInteractor {

    override suspend fun hasImportantUpdates(): Boolean {
        return versionService.hasImportantUpdates()
    }

    override suspend fun getUpdateNotifications(): List<UpdateNotification> {
        return try {
            versionService.getNewUpdateNotifications()
                .sortedWith { o1, o2 -> o2.version.compareTo(o1.version) }
        } catch (e: Exception) {
            listOf()
        }
    }

    override suspend fun skipNewUpdates() {
        versionService.skipCurrentUpdates()
    }
}
