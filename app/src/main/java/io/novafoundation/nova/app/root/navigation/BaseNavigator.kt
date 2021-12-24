package io.novafoundation.nova.app.root.navigation

import android.os.Bundle
import androidx.annotation.IdRes

abstract class BaseNavigator(
    private val navigationHolder: NavigationHolder
) {

    fun back() {
        navigationHolder.executeBack()
    }

    fun performNavigation(@IdRes actionId: Int, args: Bundle? = null) {
        val navController = navigationHolder.navController

        navController?.currentDestination?.getAction(actionId)?.let {
            navController.navigate(actionId, args)
        }
    }
}
