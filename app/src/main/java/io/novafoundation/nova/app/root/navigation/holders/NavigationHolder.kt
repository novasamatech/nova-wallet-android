package io.novafoundation.nova.app.root.navigation.holders

import androidx.navigation.NavController
import io.novafoundation.nova.common.resources.ContextManager

abstract class NavigationHolder(val contextManager: ContextManager) {

    var navController: NavController? = null
        private set

    fun attach(navController: NavController) {
        this.navController = navController
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
