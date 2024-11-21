package io.novafoundation.nova.app.root.navigation.navigators.versions

import io.novafoundation.nova.app.R
import io.novafoundation.nova.app.root.navigation.holders.MainNavigationHolder
import io.novafoundation.nova.app.root.navigation.holders.NavigationHolder
import io.novafoundation.nova.common.utils.showBrowser
import io.novafoundation.nova.feature_versions_api.presentation.VersionsRouter

class VersionsNavigator(
    private val navigationHolder: NavigationHolder,
    private val updateSourceLink: String
) : VersionsRouter {

    override fun openAppUpdater() {
        navigationHolder.contextManager.getActivity()?.showBrowser(updateSourceLink, R.string.common_cannot_find_app)
    }

    override fun back() {
        navigationHolder.navController?.popBackStack()
    }
}
