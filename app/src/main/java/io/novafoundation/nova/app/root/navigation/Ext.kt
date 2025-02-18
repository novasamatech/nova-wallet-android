package io.novafoundation.nova.app.root.navigation

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.annotation.IdRes
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.NavGraph
import io.novafoundation.nova.app.R
import io.novafoundation.nova.app.root.navigation.delayedNavigation.NavComponentDelayedNavigation
import io.novafoundation.nova.app.root.navigation.navigators.BaseNavigator
import io.novafoundation.nova.app.root.presentation.splitScreen.SplitScreenFragment
import io.novafoundation.nova.app.root.presentation.splitScreen.SplitScreenPayload

@SuppressLint("RestrictedApi")
fun NavController.getBackStackEntryBefore(@IdRes id: Int): NavBackStackEntry {
    val initial = getBackStackEntry(id)
    val backStack = backStack.toList()

    val initialIndex = backStack.indexOf(initial)

    var previousIndex = initialIndex - 1

    // ignore nav graphs
    while (previousIndex > 0 && backStack[previousIndex].destination is NavGraph) {
        previousIndex--
    }

    return backStack[previousIndex]
}

fun BaseNavigator.openSplitScreenWithInstantAction(actionId: Int, nestedActionExtras: Bundle? = null) {
    val delayedNavigation = NavComponentDelayedNavigation(actionId, nestedActionExtras)

    val splitScreenPayload = SplitScreenPayload.InstantNavigationOnAttach(delayedNavigation)
    navigationBuilder().action(R.id.action_open_split_screen)
        .setArgs(SplitScreenFragment.createPayload(splitScreenPayload))
        .navigateInRoot()
}
