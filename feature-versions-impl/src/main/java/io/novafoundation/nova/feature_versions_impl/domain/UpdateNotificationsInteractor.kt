package io.novafoundation.nova.feature_versions_impl.domain

import io.novafoundation.nova.feature_versions_api.domain.UpdateNotification
import io.novafoundation.nova.feature_versions_api.domain.UpdateNotificationsInteractor
import io.novafoundation.nova.feature_versions_api.domain.Version
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
        val comparator = Version.getComparator()
        return versionService.getNewUpdateNotifications()
            .sortedWith { o1, o2 -> comparator.compare(o1.version, o2.version) }
    }

    override suspend fun skipNewUpdates() {
        versionService.skipCurrentUpdates()
    }
}
