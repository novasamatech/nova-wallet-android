package io.novafoundation.nova.app.root.di

import dagger.Module
import dagger.Provides
import io.novafoundation.nova.app.root.domain.RootInteractor
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.core.updater.UpdateSystem
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_wallet_api.di.Wallet
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.WalletRepository

@Module
class RootFeatureModule {

    @Provides
    @FeatureScope
    fun provideRootInteractor(
        walletRepository: WalletRepository,
        @Wallet walletUpdateSystem: UpdateSystem,
        accountRepository: AccountRepository
    ): RootInteractor {
        return RootInteractor(
            walletUpdateSystem,
            walletRepository,
            accountRepository
        )
    }
}
