package io.novafoundation.nova.app.di.app.navigation

import dagger.Module
import dagger.Provides
import io.novafoundation.nova.app.root.navigation.holders.SplitScreenNavigationHolder
import io.novafoundation.nova.app.root.navigation.navigators.wallet.CurrencyNavigator
import io.novafoundation.nova.app.root.presentation.RootRouter
import io.novafoundation.nova.common.di.scope.ApplicationScope
import io.novafoundation.nova.feature_currency_api.presentation.CurrencyRouter

@Module
class CurrencyNavigationModule {

    @ApplicationScope
    @Provides
    fun provideRouter(
        rootRouter: RootRouter,
        navigationHolder: SplitScreenNavigationHolder
    ): CurrencyRouter = CurrencyNavigator(rootRouter, navigationHolder)
}
