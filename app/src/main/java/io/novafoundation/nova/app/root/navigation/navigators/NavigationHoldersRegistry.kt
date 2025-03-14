package io.novafoundation.nova.app.root.navigation.navigators

import androidx.navigation.NavController
import io.novafoundation.nova.app.root.navigation.holders.NavigationHolder
import io.novafoundation.nova.app.root.navigation.holders.RootNavigationHolder
import io.novafoundation.nova.app.root.navigation.holders.SplitScreenNavigationHolder
import io.novafoundation.nova.app.root.navigation.navigators.builder.NavigationBuilderRegistry

class NavigationHoldersRegistry(
    val splitScreenNavigationHolder: SplitScreenNavigationHolder,
    val rootNavigationHolder: RootNavigationHolder
) {

    private val holders = listOf(splitScreenNavigationHolder, rootNavigationHolder)

    val firstAttachedHolder: NavigationHolder
        get() = holders.first { it.isControllerAttached() }

    val firstAttachedNavController: NavController?
        get() = firstAttachedHolder.navController
}

fun NavigationHoldersRegistry.navigationBuilder(): NavigationBuilderRegistry {
    return NavigationBuilderRegistry(this)
}
