package io.novafoundation.nova.app.root.navigation.holders

import androidx.navigation.NavController
import io.novafoundation.nova.common.resources.ContextManager

abstract class NavigationHolder(val contextManager: ContextManager) {

    var navController: NavController? = null
        private set

    fun isControllerAttached(): Boolean {
        return navController != null
    }

    fun attach(navController: NavController) {
        this.navController = navController
    }

    /**
     * Detaches the current navController only if it matches the one provided.
     * This check ensures that if a new screen with a navController is attached,
     * it doesn't lose its navController when the previous screen calls detach.
     * By verifying equality, we prevent unintended detachment.
     */
    fun detachNavController(navController: NavController) {
        if (this.navController == navController) {
            this.navController = null
        }
    }

    fun detach() {
        navController = null
    }

    fun finishApp() {
        contextManager.getActivity()?.finish()
    }

    fun executeBack() {
        val popped = navController!!.popBackStack()

        if (!popped) {
            contextManager.getActivity()!!.finish()
        }
    }
}
