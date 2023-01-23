package io.novafoundation.nova.app.root.navigation.versions

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.core.content.ContextCompat.startActivity
import androidx.navigation.NavOptions
import io.novafoundation.nova.app.R
import io.novafoundation.nova.app.root.navigation.NavComponentDelayedNavigation
import io.novafoundation.nova.app.root.navigation.NavigationHolder
import io.novafoundation.nova.common.navigation.DelayedNavigation
import io.novafoundation.nova.feature_versions_api.presentation.VersionsRouter

class VersionsNavigator(
    private val navigationHolder: NavigationHolder
) : VersionsRouter {

    override fun skipUpdatesClicked(nextNavigation: DelayedNavigation) {
        require(nextNavigation is NavComponentDelayedNavigation)

        val navOptions = NavOptions.Builder()
            .setPopUpTo(R.id.versions_nav_graph, true)
            .setEnterAnim(R.anim.fragment_open_enter)
            .setExitAnim(R.anim.fragment_open_exit)
            .setPopEnterAnim(R.anim.fragment_close_enter)
            .setPopExitAnim(R.anim.fragment_close_exit)
            .build()

        navigationHolder.navController?.navigate(nextNavigation.globalActionId, nextNavigation.extras, navOptions)
    }

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
}
