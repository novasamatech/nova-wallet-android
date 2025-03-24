package io.novafoundation.nova.app.root.navigation.navigators.builder

import android.os.Bundle
import androidx.navigation.NavDestination
import androidx.navigation.NavOptions
import androidx.navigation.fragment.FragmentNavigator
import io.novafoundation.nova.app.root.navigation.holders.NavigationHolder
import io.novafoundation.nova.app.root.navigation.navigators.NavigationHoldersRegistry

/**
 * Class for building navigation.
 * Currently, it has 2 navigators: split_screen and root
 * - root-navigator - a navigator that opens fragments on top of others and is designed for fragments that do not require a split screen (For example, DAppBrowser needs to be opened in the root navigator)
 * - split_screen-navigator - the main navigator of the application. All fragments opened in it will be opened in a split screen mode
 *
 * When building navigation, it is usually sufficient to add a new node to split_screen_navigation_graph, but there are cases when this node needs to be added to both graphs at once
 * (For example, the transaction confirmation screen, we need to open both in the split screen and directly in the browser)
 * To handle such cases, we use the [navigateInFirstAttachedContext] method, which by default tries to open the fragment in split_screen, but if it fails, opens the fragment in root
 **/
abstract class NavigationBuilder (
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

    /**
     * Opens a fragment in the first attached navigation holder (split_screen or root).
     * If it is assumed that the fragment can be opened both in root and in split_screen, then it is necessary to add a navigation node both to split_screen_navigation_graph and to root_navigation_graph
     */
    fun navigateInFirstAttachedContext() {
        performInternal(navigationHoldersRegistry.firstAttachedHolder)
    }

    /**
     * Always open fragment in root navigation holder.
     * In this case, the node must be added to root_navigation_graph
     **/
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
