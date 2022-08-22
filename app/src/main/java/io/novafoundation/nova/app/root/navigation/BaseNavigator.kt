package io.novafoundation.nova.app.root.navigation

import android.os.Bundle
import androidx.annotation.IdRes
import io.novafoundation.nova.common.navigation.ReturnableRouter

abstract class BaseNavigator(
    private val navigationHolder: NavigationHolder
): ReturnableRouter {

    override fun back() {
        navigationHolder.executeBack()
    }

    /**
     * Performs conditional navigation based on current destination
     * @param cases - array of pairs (currentDestination, navigationAction)
     */
    fun performNavigation(
        cases: Array<Pair<Int, Int>>,
        args: Bundle?
    ) {
        val navController = navigationHolder.navController

        navController?.currentDestination?.let { currentDestination ->
            val (_, case) = cases.find { (startDestination, _) -> startDestination == currentDestination.id }
                ?: throw IllegalArgumentException("Unknown case for ${currentDestination.label}")

            currentDestination.getAction(case)?.let {
                navController.navigate(case, args)
            }
        }
    }

    fun performNavigation(@IdRes actionId: Int, args: Bundle? = null) {
        val navController = navigationHolder.navController

        navController?.currentDestination?.getAction(actionId)?.let {
            navController.navigate(actionId, args)
        }
    }
}
