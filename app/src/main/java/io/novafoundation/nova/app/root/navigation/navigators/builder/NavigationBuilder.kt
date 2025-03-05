package io.novafoundation.nova.app.root.navigation.navigators.builder

import android.os.Bundle
import androidx.navigation.NavDestination
import androidx.navigation.NavOptions
import androidx.navigation.fragment.FragmentNavigator
import io.novafoundation.nova.app.root.navigation.holders.NavigationHolder
import io.novafoundation.nova.app.root.navigation.navigators.NavigationHoldersRegistry

abstract class NavigationBuilder(
    private val navigationHoldersRegistry: NavigationHoldersRegistry
) {

    protected var navOptions: NavOptions? = null
    protected var args: Bundle? = null
    protected var extras: FragmentNavigator.Extras? = null

    fun setArgs(args: Bundle?): NavigationBuilder {
        this.args = args
        return this
    }

    fun setNavOptions(navOptions: NavOptions): NavigationBuilder {
        this.navOptions = navOptions
        return this
    }

    fun setExtras(extras: FragmentNavigator.Extras?): NavigationBuilder {
        this.extras = extras
        return this
    }

    fun navigateInFirstAttachedContext() {
        performInternal(navigationHoldersRegistry.firstAttachedHolder)
    }

    fun navigateInRoot() {
        performInternal(navigationHoldersRegistry.rootNavigationHolder)
    }

    protected fun NavigationBuilder.performAction(navigationHolder: NavigationHolder, actionId: Int) {
        val navController = navigationHolder.navController ?: return
        val currentDestination = navController.currentDestination ?: return

        if (currentDestination.hasAction(actionId)) {
            navigationHolder.navController?.navigate(actionId, args, navOptions, extras)
        }
    }

    protected abstract fun performInternal(navigationHolder: NavigationHolder)
}

private fun NavDestination.hasAction(actionId: Int): Boolean {
    return this.getAction(actionId) != null
}
