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
 * Let's look at the scenarios for building navigation:
 * - In the normal case, you need to add a navigation node only to the split_screen_nav_graph, be it a dialog or a fragment.
 *   This can be used when you are sure that the fragment or dialog should not be launched in the browser or on top of a split screen.
 *   Use [navigateInFirstAttachedContext] and the fragment will be automatically attached to the SplitScreenNavigationHolder.
 * - If you expect that the fragment can also be launched from the browser, you need to add it to both the root_navigation_graph and the split_screen_navigation_graph.
 *   Keep in mind that the actionId must be the same in both graphs.
 *   Use [navigateInFirstAttachedContext] and the fragment will be automatically attached to the desired holder.
 * - In case of adding dialogs, you can add it only to the root_navigation_graph if you think that the dialog can be launched both from the browser and from the remote part of the application.
 *   To attach the dialog to the RootNavigationHolder, call [navigateInRoot]
 * - In the latter case, we may need to add a screen that is strictly required to be opened on top of the split screen or only in the browser flow. (Such screens as entering a pin code).
 *   In this case, we need to add an action only to the root_navigation_graph
 *   To attach the fragment to the RootNavigationHolder, call [navigateInRoot]
 **/
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
