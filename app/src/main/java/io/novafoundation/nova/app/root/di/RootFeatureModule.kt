package io.novafoundation.nova.app.root.di

import dagger.Module
import dagger.Provides
import io.novafoundation.nova.app.root.di.busHandler.RequestBusHandlerModule
import io.novafoundation.nova.app.root.di.deeplink.DeepLinksModule
import io.novafoundation.nova.app.root.domain.MainInteractor
import io.novafoundation.nova.app.root.domain.RootInteractor
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.feature_account_api.data.externalAccounts.ExternalAccountsSyncService
import io.novafoundation.nova.feature_account_api.data.multisig.MultisigPendingOperationsService
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_ahm_api.data.repository.ChainMigrationRepository
import io.novafoundation.nova.feature_ahm_api.data.repository.MigrationInfoRepository
import io.novafoundation.nova.feature_assets.data.network.BalancesUpdateSystem
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.WalletRepository

@Module(
    includes = [
        RequestBusHandlerModule::class,
        ExternalServiceInitializersModule::class,
        DeepLinksModule::class
    ]
)
class RootFeatureModule {

    @Provides
    @FeatureScope
    fun provideRootInteractor(
        walletRepository: WalletRepository,
        accountRepository: AccountRepository,
        balancesUpdateSystem: BalancesUpdateSystem,
        multisigPendingOperationsService: MultisigPendingOperationsService,
        externalAccountsSyncService: ExternalAccountsSyncService,
        chainMigrationRepository: ChainMigrationRepository,
        migrationInfoRepository: MigrationInfoRepository
    ): RootInteractor {
        return RootInteractor(
            updateSystem = balancesUpdateSystem,
            walletRepository = walletRepository,
            accountRepository = accountRepository,
            multisigPendingOperationsService = multisigPendingOperationsService,
            externalAccountsSyncService = externalAccountsSyncService,
            chainMigrationRepository = chainMigrationRepository,
            migrationInfoRepository = migrationInfoRepository
        )
    }

    @Provides
    @FeatureScope
    fun provideMainInteractor(
        chainMigrationRepository: ChainMigrationRepository,
        migrationInfoRepository: MigrationInfoRepository
    ): MainInteractor {
        return MainInteractor(
            chainMigrationRepository = chainMigrationRepository,
            migrationInfoRepository = migrationInfoRepository
        )
    }
}
