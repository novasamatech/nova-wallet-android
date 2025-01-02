package io.novafoundation.nova.app.root.navigation.navigators

import android.os.Bundle
import androidx.navigation.NavDestination
import androidx.navigation.NavOptions
import androidx.navigation.fragment.FragmentNavigator
import io.novafoundation.nova.app.root.navigation.holders.NavigationHolder

/**
 * Builder for navigation
 * @param navigationHoldersRegistry
 * @param actionId - action to perform or fallback when cases not found
 */
class NavigationBuilder(
    private val navigationHoldersRegistry: NavigationHoldersRegistry,
    private val actionId: Int?
) {

    class Case(val destination: Int, val actionId: Int)

    private val cases = mutableListOf<Case>()
    private var fallbackCaseActionId: Int? = null
    private var navOptions: NavOptions? = null
    private var args: Bundle? = null
    private var extras: FragmentNavigator.Extras? = null

    fun addCase(currentDestination: Int, actionId: Int): NavigationBuilder {
        cases.add(Case(currentDestination, actionId))
        return this
    }

    fun setFallbackCase(actionId: Int): NavigationBuilder {
        fallbackCaseActionId = actionId
        return this
    }

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

    fun perform() {
        performInternal(navigationHoldersRegistry.firstAttachedHolder)
    }

    fun performInRoot() {
        performInternal(navigationHoldersRegistry.rootNavigationHolder)
    }

    private fun performInternal(navigationHolder: NavigationHolder) {
        if (actionId == null) {
            performCases(navigationHolder)
        } else {
            performAction(navigationHolder, actionId)
        }
    }

    private fun performCases(navigationHolder: NavigationHolder) {
        val navController = navigationHolder.navController ?: return
        val currentDestination = navController.currentDestination ?: return

        val caseActionId = cases.find { case -> case.destination == currentDestination.id }
            ?.actionId
            ?: fallbackCaseActionId
            ?: throw IllegalArgumentException("Unknown case for ${currentDestination.label}")

        performAction(navigationHolder, caseActionId)
    }

    private fun performAction(navigationHolder: NavigationHolder, actionId: Int) {
        val navController = navigationHolder.navController ?: return
        val currentDestination = navController.currentDestination ?: return

        if (currentDestination.hasAction(actionId)) {
            navigationHolder.navController?.navigate(actionId, args, navOptions, extras)
        }
    }

    private fun NavDestination.hasAction(actionId: Int): Boolean {
        return this.getAction(actionId) != null
    }
}
