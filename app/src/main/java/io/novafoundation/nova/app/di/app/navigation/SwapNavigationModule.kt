package io.novafoundation.nova.app.di.app.navigation

import dagger.Module
import dagger.Provides
import io.novafoundation.nova.app.root.navigation.holders.MainNavigationHolder
import io.novafoundation.nova.app.root.navigation.navigators.Navigator
import io.novafoundation.nova.app.root.navigation.navigators.swap.SwapNavigator
import io.novafoundation.nova.common.di.scope.ApplicationScope
import io.novafoundation.nova.feature_swap_impl.presentation.SwapRouter

@Module
class SwapNavigationModule {

    @ApplicationScope
    @Provides
    fun provideRouter(
        navigationHolder: MainNavigationHolder,
        commonDelegate: Navigator
    ): SwapRouter = SwapNavigator(navigationHolder, commonDelegate)
}
