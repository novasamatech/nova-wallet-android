package io.novafoundation.nova.app.root.navigation.navigators.versions

import io.novafoundation.nova.app.R
import io.novafoundation.nova.app.root.navigation.holders.RootNavigationHolder
import io.novafoundation.nova.app.root.navigation.holders.SplitScreenNavigationHolder
import io.novafoundation.nova.app.root.navigation.navigators.BaseNavigator
import io.novafoundation.nova.app.root.navigation.navigators.NavigationHoldersRegistry
import io.novafoundation.nova.common.resources.ContextManager
import io.novafoundation.nova.common.utils.showBrowser
import io.novafoundation.nova.feature_versions_api.presentation.VersionsRouter

class VersionsNavigator(
    navigationHoldersRegistry: NavigationHoldersRegistry,
    private val contextManager: ContextManager,
    private val updateSourceLink: String
) : BaseNavigator(navigationHoldersRegistry),VersionsRouter {

    override fun openAppUpdater() {
        contextManager.getActivity()?.showBrowser(updateSourceLink, R.string.common_cannot_find_app)
    }
}
