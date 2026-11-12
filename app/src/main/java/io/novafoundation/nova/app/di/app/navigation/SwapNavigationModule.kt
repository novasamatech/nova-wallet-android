package io.novafoundation.nova.app.di.app.navigation

import io.novafoundation.nova.feature_swap_api.presentation.navigation.SwapTokenSelectionCommunicator
import io.novafoundation.nova.app.root.navigation.navigators.swap.SwapTokenSelectionCommunicatorImpl
import dagger.Module
import dagger.Provides
import io.novafoundation.nova.app.root.navigation.navigators.NavigationHoldersRegistry
import io.novafoundation.nova.app.root.navigation.navigators.Navigator
import io.novafoundation.nova.app.root.navigation.navigators.swap.SlippageSettingsCommunicatorImpl
import io.novafoundation.nova.app.root.navigation.navigators.swap.SwapNavigator
import io.novafoundation.nova.app.root.navigation.navigators.swap.SwapTabNavigator
import io.novafoundation.nova.common.di.scope.ApplicationScope
import io.novafoundation.nova.feature_swap_api.presentation.navigation.SlippageSettingsCommunicator
import io.novafoundation.nova.feature_swap_impl.presentation.SwapRouter

@Module
class SwapNavigationModule {

    @ApplicationScope
    @Provides
    fun provideSwapTabNavigator(registry: NavigationHoldersRegistry): SwapTabNavigator = SwapTabNavigator(registry)

    @ApplicationScope
    @Provides
    fun provideSwapTokenSelectionCommunicator(registry: NavigationHoldersRegistry): SwapTokenSelectionCommunicator =
        SwapTokenSelectionCommunicatorImpl(registry)

    @ApplicationScope
    @Provides
    fun provideSlippageSettingsCommunicator(registry: NavigationHoldersRegistry): SlippageSettingsCommunicator =
        SlippageSettingsCommunicatorImpl(registry)

    @ApplicationScope
    @Provides
    fun provideRouter(
        navigationHoldersRegistry: NavigationHoldersRegistry,
        commonDelegate: Navigator
    ): SwapRouter = SwapNavigator(navigationHoldersRegistry, commonDelegate)
}
