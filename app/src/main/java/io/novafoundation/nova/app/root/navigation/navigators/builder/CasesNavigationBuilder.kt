package io.novafoundation.nova.app.root.navigation.navigators.builder

import androidx.navigation.NavDestination
import io.novafoundation.nova.app.root.navigation.holders.NavigationHolder
import io.novafoundation.nova.app.root.navigation.navigators.NavigationHoldersRegistry

class CasesNavigationBuilder(
    navigationHoldersRegistry: NavigationHoldersRegistry
) : NavigationBuilder(navigationHoldersRegistry) {

    private class Case(val destination: Int, val actionId: Int)

    private var cases = mutableListOf<Case>()

    private var fallbackCaseActionId: Int? = null

    fun addCase(currentDestination: Int, actionId: Int): CasesNavigationBuilder {
        cases.add(Case(currentDestination, actionId))
        return this
    }

    fun setFallbackCase(actionId: Int): CasesNavigationBuilder {
        fallbackCaseActionId = actionId
        return this
    }

    override fun performInternal(navigationHolder: NavigationHolder) {
        val navController = navigationHolder.navController ?: return
        val currentDestination = navController.currentDestination ?: return

        val caseActionId = cases.find { case -> case.destination == currentDestination.id }
            ?.actionId
            ?: fallbackCaseActionId
            ?: throw IllegalArgumentException("Unknown case for ${currentDestination.label}")

        performAction(navigationHolder, caseActionId)
    }

    private fun NavDestination.hasAction(actionId: Int): Boolean {
        return this.getAction(actionId) != null
    }
}
