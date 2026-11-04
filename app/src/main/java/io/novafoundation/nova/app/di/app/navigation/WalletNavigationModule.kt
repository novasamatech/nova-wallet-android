package io.novafoundation.nova.app.di.app.navigation

import dagger.Module
import dagger.Provides
import io.novafoundation.nova.app.root.navigation.navigators.NavigationHoldersRegistry
import io.novafoundation.nova.app.root.navigation.navigators.Navigator
import io.novafoundation.nova.app.root.navigation.navigators.wallet.WalletNavigator
import io.novafoundation.nova.common.di.scope.ApplicationScope
import io.novafoundation.nova.feature_dapp_impl.presentation.DAppRouter
import io.novafoundation.nova.feature_wallet_impl.presentation.WalletRouter

@Module
class WalletNavigationModule {

    @ApplicationScope
    @Provides
    fun provideRouter(
        navigationHoldersRegistry: NavigationHoldersRegistry,
        commonDelegate: Navigator,
        dAppRouter: DAppRouter
    ): WalletRouter = WalletNavigator(commonDelegate, dAppRouter, navigationHoldersRegistry)
}
