package io.novafoundation.nova.feature_wallet_api.di.common

import dagger.Module
import dagger.Provides
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_wallet_api.domain.AssetUseCase
import io.novafoundation.nova.feature_wallet_api.domain.implementations.AssetUseCaseImpl
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.WalletRepository
import io.novafoundation.nova.runtime.state.SelectedAssetOptionSharedState

@Module(includes = [TokenUseCaseModule::class])
class AssetUseCaseModule {

    @Provides
    @FeatureScope
    fun provideAssetUseCase(
        walletRepository: WalletRepository,
        accountRepository: AccountRepository,
        sharedState: SelectedAssetOptionSharedState<*>,
    ): AssetUseCase = AssetUseCaseImpl(
        walletRepository,
        accountRepository,
        sharedState
    )
}
