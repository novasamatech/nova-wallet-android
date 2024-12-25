package io.novafoundation.nova.feature_versions_api.presentation

interface VersionsRouter {

    fun openAppUpdater()

    fun closeUpdateNotifications()
}
