package io.novafoundation.nova.app.root.navigation.versions

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import io.novafoundation.nova.app.root.navigation.NavigationHolder
import io.novafoundation.nova.feature_versions_api.presentation.VersionsRouter

class VersionsNavigator(
    private val navigationHolder: NavigationHolder
) : VersionsRouter {

    override fun openInstallUpdates() {
        val activity = navigationHolder.contextManager.getActivity()
        require(activity != null)
        val packageName = activity.packageName

        try {
            activity.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName")))
        } catch (e: ActivityNotFoundException) {
            activity.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$packageName")))
        }
    }

    override fun back() {
        navigationHolder.navController?.popBackStack()
    }
}
