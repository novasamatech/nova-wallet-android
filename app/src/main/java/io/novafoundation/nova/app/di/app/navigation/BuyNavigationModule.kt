package io.novafoundation.nova.app.di.app.navigation

import dagger.Module
import dagger.Provides
import io.novafoundation.nova.app.root.navigation.holders.SplitScreenNavigationHolder
import io.novafoundation.nova.app.root.navigation.navigators.buy.BuyNavigator
import io.novafoundation.nova.common.di.scope.ApplicationScope
import io.novafoundation.nova.feature_buy_impl.presentation.BuyRouter

@Module
class BuyNavigationModule {

    @ApplicationScope
    @Provides
    fun provideRouter(navigationHolder: SplitScreenNavigationHolder): BuyRouter = BuyNavigator(navigationHolder)
}
