package io.novafoundation.nova.app.root.navigation.swap

import io.novafoundation.nova.app.root.navigation.BaseNavigator
import io.novafoundation.nova.app.root.navigation.NavigationHolder
import io.novafoundation.nova.feature_swap_impl.presentation.SwapRouter

class SwapNavigator(
    navigationHolder: NavigationHolder
) : BaseNavigator(navigationHolder), SwapRouter
