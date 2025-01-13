package io.novafoundation.nova.app.root.navigation.navigators.builder

import io.novafoundation.nova.app.root.navigation.navigators.NavigationHoldersRegistry

class NavigationBuilderRegistry(private val registry: NavigationHoldersRegistry) {

    fun action(actionId: Int) = ActionNavigationBuilder(registry, actionId)

    fun cases() = CasesNavigationBuilder(registry)

    fun graph(graphId: Int) = GraphNavigationBuilder(registry, graphId)
}
