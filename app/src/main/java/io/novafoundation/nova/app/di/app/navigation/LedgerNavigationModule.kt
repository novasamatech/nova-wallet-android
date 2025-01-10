package io.novafoundation.nova.app.di.app.navigation

import dagger.Module
import dagger.Provides
import io.novafoundation.nova.app.root.navigation.navigators.NavigationHoldersRegistry
import io.novafoundation.nova.app.root.navigation.navigators.ledger.LedgerNavigator
import io.novafoundation.nova.app.root.navigation.navigators.ledger.LedgerSignCommunicatorImpl
import io.novafoundation.nova.app.root.navigation.navigators.ledger.SelectLedgerAddressCommunicatorImpl
import io.novafoundation.nova.common.di.scope.ApplicationScope
import io.novafoundation.nova.feature_account_api.presenatation.sign.LedgerSignCommunicator
import io.novafoundation.nova.feature_account_impl.presentation.AccountRouter
import io.novafoundation.nova.feature_ledger_impl.presentation.LedgerRouter
import io.novafoundation.nova.feature_ledger_impl.presentation.account.connect.legacy.SelectLedgerAddressInterScreenCommunicator

@Module
class LedgerNavigationModule {

    @ApplicationScope
    @Provides
    fun provideSelectLedgerAddressCommunicator(navigationHoldersRegistry: NavigationHoldersRegistry): SelectLedgerAddressInterScreenCommunicator {
        return SelectLedgerAddressCommunicatorImpl(navigationHoldersRegistry)
    }

    @Provides
    @ApplicationScope
    fun provideLedgerSignerCommunicator(
        navigationHoldersRegistry: NavigationHoldersRegistry
    ): LedgerSignCommunicator = LedgerSignCommunicatorImpl(navigationHoldersRegistry)

    @ApplicationScope
    @Provides
    fun provideRouter(router: AccountRouter, navigationHoldersRegistry: NavigationHoldersRegistry): LedgerRouter =
        LedgerNavigator(router, navigationHoldersRegistry)
}
