package io.novafoundation.nova.feature_versions_api.presentation

import io.novafoundation.nova.common.navigation.DelayedNavigation

interface VersionsRouter {

    fun skipUpdatesClicked(nextNavigation: DelayedNavigation)

    fun openInstallUpdates()
}
