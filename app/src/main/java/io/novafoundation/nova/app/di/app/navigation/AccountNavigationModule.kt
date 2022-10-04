package io.novafoundation.nova.app.di.app.navigation

import dagger.Module
import dagger.Provides
import io.novafoundation.nova.app.root.navigation.NavigationHolder
import io.novafoundation.nova.app.root.navigation.Navigator
import io.novafoundation.nova.app.root.navigation.account.AdvancedEncryptionCommunicatorImpl
import io.novafoundation.nova.app.root.navigation.account.ParitySignerSignCommunicatorImpl
import io.novafoundation.nova.app.root.navigation.account.SelectAddressCommunicatorImpl
import io.novafoundation.nova.common.di.scope.ApplicationScope
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.list.SelectAddressCommunicator
import io.novafoundation.nova.feature_account_impl.data.signer.paritySigner.ParitySignerSignCommunicator
import io.novafoundation.nova.feature_account_impl.presentation.AccountRouter
import io.novafoundation.nova.feature_account_impl.presentation.AdvancedEncryptionCommunicator
import io.novafoundation.nova.feature_assets.presentation.AssetsRouter

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
    ): ParitySignerSignCommunicator = ParitySignerSignCommunicatorImpl(navigationHolder)

    @Provides
    @ApplicationScope
    fun provideSelectAddressCommunicator(
        router: AssetsRouter,
        navigationHolder: NavigationHolder
    ): SelectAddressCommunicator = SelectAddressCommunicatorImpl(router, navigationHolder)

    @ApplicationScope
    @Provides
    fun provideAccountRouter(navigator: Navigator): AccountRouter = navigator
}
