package io.novafoundation.nova.app.di.app.navigation

import dagger.Module
import dagger.Provides
import io.novafoundation.nova.app.root.navigation.NavigationHolder
import io.novafoundation.nova.app.root.navigation.dApp.DAppNavigator
import io.novafoundation.nova.common.di.scope.ApplicationScope
import io.novafoundation.nova.feature_dapp_impl.DAppRouter

@Module
class DAppNavigationModule {

    @ApplicationScope
    @Provides
    fun provideRouter(navigationHolder: NavigationHolder): DAppRouter = DAppNavigator(navigationHolder)
}
