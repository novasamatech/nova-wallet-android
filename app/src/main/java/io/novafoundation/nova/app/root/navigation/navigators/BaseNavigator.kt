package io.novafoundation.nova.app.root.navigation.navigators

import io.novafoundation.nova.common.navigation.ReturnableRouter

abstract class BaseNavigator(
    private val navigationHoldersRegistry: NavigationHoldersRegistry
) : ReturnableRouter {

    val currentBackStackEntry
        get() = navigationHoldersRegistry.firstAttachedNavController
            ?.currentBackStackEntry

    val previousBackStackEntry
        get() = navigationHoldersRegistry.firstAttachedNavController
            ?.previousBackStackEntry

    val currentDestination
        get() = navigationHoldersRegistry.firstAttachedNavController
            ?.currentDestination

    override fun back() {
        navigationHoldersRegistry.firstAttachedHolder.executeBack()
    }

    fun finishApp() {
        navigationHoldersRegistry.firstAttachedHolder.finishApp()
    }

    protected fun navigationBuilder(destination: Int? = null): NavigationBuilder {
        return NavigationBuilder(navigationHoldersRegistry, destination)
    }
}
