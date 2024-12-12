package io.novafoundation.nova.app.di.app.navigation

import dagger.Module
import dagger.Provides
import io.novafoundation.nova.app.root.navigation.holders.SplitScreenNavigationHolder
import io.novafoundation.nova.app.root.navigation.navigators.Navigator
import io.novafoundation.nova.app.root.navigation.navigators.settings.SettingsNavigator
import io.novafoundation.nova.app.root.presentation.RootRouter
import io.novafoundation.nova.common.di.scope.ApplicationScope
import io.novafoundation.nova.feature_settings_impl.SettingsRouter
import io.novafoundation.nova.feature_wallet_connect_impl.WalletConnectRouter

@Module
class SettingsNavigationModule {

    @ApplicationScope
    @Provides
    fun provideRouter(
        rootRouter: RootRouter,
        navigationHolder: SplitScreenNavigationHolder,
        walletConnectRouter: WalletConnectRouter,
        navigator: Navigator,
    ): SettingsRouter = SettingsNavigator(navigationHolder, rootRouter, walletConnectRouter, navigator)
}
