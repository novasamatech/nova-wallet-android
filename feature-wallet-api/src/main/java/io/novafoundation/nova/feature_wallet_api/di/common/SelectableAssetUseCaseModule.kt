package io.novafoundation.nova.feature_wallet_api.di.common

import dagger.Binds
import dagger.Module
import dagger.Provides
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_wallet_api.domain.AssetUseCase
import io.novafoundation.nova.feature_wallet_api.domain.SelectableAssetUseCase
import io.novafoundation.nova.feature_wallet_api.domain.implementations.SelectableAssetUseCaseImpl
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.WalletRepository
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.assetSelector.AssetSelectorFactory
import io.novafoundation.nova.runtime.state.SelectableSingleAssetSharedState
import io.novafoundation.nova.runtime.state.SelectedAssetOptionSharedState

@Module(includes = [SelectableAssetUseCaseModule.BindsModule::class, TokenUseCaseModule::class])
class SelectableAssetUseCaseModule {

    @Provides
    @FeatureScope
    fun provideAssetUseCase(
        walletRepository: WalletRepository,
        accountRepository: AccountRepository,
        sharedState: SelectableSingleAssetSharedState<*>,
    ): SelectableAssetUseCase<*> = SelectableAssetUseCaseImpl(
        walletRepository,
        accountRepository,
        sharedState,
    )

    @Provides
    @FeatureScope
    fun provideAssetSelectorMixinFactory(
        assetUseCase: SelectableAssetUseCase<*>,
        singleAssetSharedState: SelectableSingleAssetSharedState<*>,
        resourceManager: ResourceManager
    ) = AssetSelectorFactory(
        assetUseCase,
        singleAssetSharedState,
        resourceManager
    )

    @Module
    interface BindsModule {

        @Binds
        fun bindAssetUseCase(selectableAssetUseCase: SelectableAssetUseCase<*>): AssetUseCase

        @Binds
        fun bindSelectedAssetState(selectableSingleAssetSharedState: SelectableSingleAssetSharedState<*>): SelectedAssetOptionSharedState<*>
    }
}
