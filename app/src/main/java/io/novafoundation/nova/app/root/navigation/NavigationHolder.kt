package io.novafoundation.nova.app.root.navigation

import androidx.navigation.NavController
import io.novafoundation.nova.common.resources.ContextManager

class NavigationHolder(val contextManager: ContextManager) {

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
}

fun NavigationHolder.executeBack() {
    val popped = navController!!.popBackStack()

    if (!popped) {
        contextManager.getActivity()!!.finish()
    }
}
