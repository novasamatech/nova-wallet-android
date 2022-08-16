package io.novafoundation.nova.app.di.app.navigation

import dagger.Module
import dagger.Provides
import io.novafoundation.nova.app.root.navigation.NavigationHolder
import io.novafoundation.nova.app.root.navigation.Navigator
import io.novafoundation.nova.app.root.navigation.account.AdvancedEncryptionCommunicatorImpl
import io.novafoundation.nova.app.root.navigation.account.ParitySignerSignCommunicatorImpl
import io.novafoundation.nova.app.root.navigation.account.SelectWalletCommunicatorImpl
import io.novafoundation.nova.common.di.scope.ApplicationScope
import io.novafoundation.nova.feature_account_impl.presentation.AccountRouter
import io.novafoundation.nova.feature_account_impl.presentation.AdvancedEncryptionCommunicator
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.SelectWalletCommunicator
import io.novafoundation.nova.feature_account_impl.presentation.paritySigner.ParitySignerSignInterScreenCommunicator
import io.novafoundation.nova.feature_assets.presentation.WalletRouter

@Module
class AccountNavigationModule {

    @Provides
    @ApplicationScope
    fun provideAdvancedEncryptionCommunicator(
        navigationHolder: NavigationHolder
    ): AdvancedEncryptionCommunicator = AdvancedEncryptionCommunicatorImpl(navigationHolder)

    @Provides
    @ApplicationScope
    fun provideParitySignerCommunicator(
        navigationHolder: NavigationHolder
    ): ParitySignerSignInterScreenCommunicator = ParitySignerSignCommunicatorImpl(navigationHolder)

    @Provides
    @ApplicationScope
    fun provideSelectWalletCommunicator(
        router: WalletRouter,
        navigationHolder: NavigationHolder
    ): SelectWalletCommunicator = SelectWalletCommunicatorImpl(router, navigationHolder)

    @ApplicationScope
    @Provides
    fun provideAccountRouter(navigator: Navigator): AccountRouter = navigator
}
