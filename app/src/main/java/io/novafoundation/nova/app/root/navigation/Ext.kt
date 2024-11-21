package io.novafoundation.nova.app.root.navigation

import android.annotation.SuppressLint
import androidx.activity.OnBackPressedCallback
import androidx.annotation.IdRes
import androidx.appcompat.app.AppCompatActivity
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

fun AppCompatActivity.setupBackNavigation(mainNavController: NavController, dAppNavController: NavController) {
    onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            // Firstly handle dapp back navigation
            if (dAppNavController.popBackStack()) {
                // If back navigation is handled - return
                return
            }

            // Secondly handle main back navigation
            if (mainNavController.popBackStack()) {
                // If back navigation is handled - return
                return
            }

            isEnabled = false
            onBackPressedDispatcher.onBackPressed()
            isEnabled = true
        }
    })
}
