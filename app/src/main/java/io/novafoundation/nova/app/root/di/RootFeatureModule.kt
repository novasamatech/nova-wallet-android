package io.novafoundation.nova.app.root.di

import dagger.Module
import dagger.Provides
import io.novafoundation.nova.app.root.di.busHandler.RequestBusHandlerModule
import io.novafoundation.nova.app.root.domain.RootInteractor
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.feature_account_api.data.proxy.ProxySyncService
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_assets.data.network.BalancesUpdateSystem
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.WalletRepository

@Module(includes = [RequestBusHandlerModule::class])
class RootFeatureModule {

    @Provides
    @FeatureScope
    fun provideRootInteractor(
        walletRepository: WalletRepository,
        accountRepository: AccountRepository,
        balancesUpdateSystem: BalancesUpdateSystem,
        proxySyncService: ProxySyncService
    ): RootInteractor {
        return RootInteractor(
            updateSystem = balancesUpdateSystem,
            walletRepository = walletRepository,
            accountRepository = accountRepository,
            proxySyncService = proxySyncService
        )
    }
}
