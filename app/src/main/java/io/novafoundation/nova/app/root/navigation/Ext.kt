package io.novafoundation.nova.app.root.navigation

import android.annotation.SuppressLint
import androidx.annotation.IdRes
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.NavGraph

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
