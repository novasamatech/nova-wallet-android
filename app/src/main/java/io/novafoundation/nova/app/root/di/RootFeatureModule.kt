package io.novafoundation.nova.app.root.di

import dagger.Module
import dagger.Provides
import io.novafoundation.nova.app.root.di.busHandler.RequestBusHandlerModule
import io.novafoundation.nova.app.root.domain.RootInteractor
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.common.view.bottomSheet.action.ActionBottomSheetLauncher
import io.novafoundation.nova.common.view.bottomSheet.action.ActionBottomSheetLauncherFactory
import io.novafoundation.nova.feature_account_api.data.proxy.ProxySyncService
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_assets.data.network.BalancesUpdateSystem
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.WalletRepository
import javax.inject.Qualifier

@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class RootActionBottomSheetLauncher

@Module(includes = [RequestBusHandlerModule::class, ExternalServiceInitializersModule::class])
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

    @Provides
    @FeatureScope
    @RootActionBottomSheetLauncher
    fun provideRootActionBottomSheetLauncher(
        actionBottomSheetLauncherFactory: ActionBottomSheetLauncherFactory
    ): ActionBottomSheetLauncher {
        return actionBottomSheetLauncherFactory.create()
    }
}
