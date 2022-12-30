package io.novafoundation.nova.app.di.app.navigation

import dagger.Module
import dagger.Provides
import io.novafoundation.nova.app.root.navigation.NavigationHolder
import io.novafoundation.nova.app.root.navigation.dApp.DAppNavigator
import io.novafoundation.nova.app.root.navigation.dApp.DAppOptionsCommunicatorImpl
import io.novafoundation.nova.app.root.navigation.dApp.DAppSearchCommunicatorImpl
import io.novafoundation.nova.app.root.navigation.dApp.DAppSignCommunicatorImpl
import io.novafoundation.nova.common.di.scope.ApplicationScope
import io.novafoundation.nova.feature_dapp_impl.DAppRouter
import io.novafoundation.nova.feature_dapp_impl.presentation.browser.options.DAppOptionsCommunicator
import io.novafoundation.nova.feature_dapp_impl.presentation.browser.signExtrinsic.DAppSignCommunicator
import io.novafoundation.nova.feature_dapp_impl.presentation.search.DAppSearchCommunicator

@Module
class DAppNavigationModule {

    @ApplicationScope
    @Provides
    fun provideRouter(navigationHolder: NavigationHolder): DAppRouter = DAppNavigator(navigationHolder)

    @ApplicationScope
    @Provides
    fun provideSignExtrinsicCommunicator(navigationHolder: NavigationHolder): DAppSignCommunicator {
        return DAppSignCommunicatorImpl(navigationHolder)
    }

    @ApplicationScope
    @Provides
    fun provideSearchDappCommunicator(navigationHolder: NavigationHolder): DAppSearchCommunicator {
        return DAppSearchCommunicatorImpl(navigationHolder)
    }

    @ApplicationScope
    @Provides
    fun provideDAppOptionsCommunicator(navigationHolder: NavigationHolder): DAppOptionsCommunicator {
        return DAppOptionsCommunicatorImpl(navigationHolder)
    }
}
