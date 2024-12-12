package io.novafoundation.nova.app.di.app.navigation

import dagger.Module
import dagger.Provides
import io.novafoundation.nova.app.root.navigation.holders.SplitScreenNavigationHolder
import io.novafoundation.nova.app.root.navigation.navigators.Navigator
import io.novafoundation.nova.app.root.navigation.navigators.swap.SwapNavigator
import io.novafoundation.nova.common.di.scope.ApplicationScope
import io.novafoundation.nova.feature_swap_impl.presentation.SwapRouter

@Module
class SwapNavigationModule {

    @ApplicationScope
    @Provides
    fun provideRouter(
        navigationHolder: SplitScreenNavigationHolder,
        commonDelegate: Navigator
    ): SwapRouter = SwapNavigator(navigationHolder, commonDelegate)
}
