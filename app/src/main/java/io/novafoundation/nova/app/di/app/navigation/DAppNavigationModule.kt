package io.novafoundation.nova.app.di.app.navigation

import dagger.Module
import dagger.Provides
import io.novafoundation.nova.app.root.navigation.holders.RootNavigationHolder
import io.novafoundation.nova.app.root.navigation.navigators.dApp.DAppNavigator
import io.novafoundation.nova.app.root.navigation.navigators.dApp.DAppSearchCommunicatorImpl
import io.novafoundation.nova.common.di.scope.ApplicationScope
import io.novafoundation.nova.feature_dapp_api.DAppRouter
import io.novafoundation.nova.feature_dapp_impl.presentation.search.DAppSearchCommunicator

@Module
class DAppNavigationModule {

    @ApplicationScope
    @Provides
    fun provideRouter(navigationHolder: RootNavigationHolder): DAppRouter = DAppNavigator(navigationHolder)

    @ApplicationScope
    @Provides
    fun provideSearchDappCommunicator(navigationHolder: RootNavigationHolder): DAppSearchCommunicator {
        return DAppSearchCommunicatorImpl(navigationHolder)
    }
}
