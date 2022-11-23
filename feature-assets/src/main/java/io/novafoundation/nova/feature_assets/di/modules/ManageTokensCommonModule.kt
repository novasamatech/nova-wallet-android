package io.novafoundation.nova.feature_assets.di.modules

import dagger.Module
import dagger.Provides
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_assets.domain.tokens.manage.ManageTokenInteractor
import io.novafoundation.nova.feature_assets.domain.tokens.manage.RealManageTokenInteractor
import io.novafoundation.nova.feature_assets.presentation.tokens.manage.model.MultiChainTokenMapper
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.ChainAssetRepository
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.WalletRepository
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry

@Module
class ManageTokensCommonModule {

    @Provides
    @FeatureScope
    fun provideMultiChainTokenUiMapper(
        resourceManager: ResourceManager
    ) = MultiChainTokenMapper(resourceManager)

    @Provides
    @FeatureScope
    fun provideInteractor(
        chainRegistry: ChainRegistry,
        walletRepository: WalletRepository,
        chainAssetRepository: ChainAssetRepository,
    ): ManageTokenInteractor = RealManageTokenInteractor(chainRegistry, walletRepository, chainAssetRepository)
}
