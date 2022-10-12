package io.novafoundation.nova.feature_governance_impl.di

import dagger.Module
import dagger.Provides
import io.novafoundation.nova.common.data.storage.Preferences
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.common.mixin.MixinFactory
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_account_api.domain.account.identity.IdentityProvider
import io.novafoundation.nova.feature_account_api.domain.account.identity.LocalIdentity
import io.novafoundation.nova.feature_account_api.domain.account.identity.OnChainIdentity
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_governance_api.data.repository.PreImageRepository
import io.novafoundation.nova.feature_governance_api.data.repository.TreasuryRepository
import io.novafoundation.nova.feature_governance_api.data.source.GovernanceSource
import io.novafoundation.nova.feature_governance_api.data.source.GovernanceSourceRegistry
import io.novafoundation.nova.feature_governance_impl.data.GovernanceSharedState
import io.novafoundation.nova.feature_governance_impl.data.repository.RealPreImageRepository
import io.novafoundation.nova.feature_governance_impl.data.repository.RealTreasuryRepository
import io.novafoundation.nova.feature_governance_impl.data.source.RealGovernanceSourceRegistry
import io.novafoundation.nova.feature_governance_impl.di.modules.GovernanceUpdatersModule
import io.novafoundation.nova.feature_governance_impl.di.modules.GovernanceV2
import io.novafoundation.nova.feature_governance_impl.di.modules.GovernanceV2Module
import io.novafoundation.nova.feature_governance_impl.di.modules.screens.ReferendumDetailsModule
import io.novafoundation.nova.feature_governance_impl.di.modules.screens.ReferendumListModule
import io.novafoundation.nova.feature_governance_impl.domain.identity.GovernanceIdentityProviderFactory
import io.novafoundation.nova.feature_governance_impl.domain.referendum.common.RealReferendaConstructor
import io.novafoundation.nova.feature_governance_impl.domain.referendum.common.ReferendaConstructor
import io.novafoundation.nova.feature_wallet_api.domain.AssetUseCase
import io.novafoundation.nova.feature_wallet_api.domain.TokenUseCase
import io.novafoundation.nova.feature_wallet_api.domain.implementations.AssetUseCaseImpl
import io.novafoundation.nova.feature_wallet_api.domain.implementations.SharedStateTokenUseCase
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.TokenRepository
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.WalletRepository
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.assetSelector.AssetSelectorFactory
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.assetSelector.AssetSelectorMixin
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.FeeLoaderMixin
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.create
import io.novafoundation.nova.runtime.di.REMOTE_STORAGE_SOURCE
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.repository.ChainStateRepository
import io.novafoundation.nova.runtime.storage.source.StorageDataSource
import javax.inject.Named

@Module(
    includes = [
        GovernanceV2Module::class,
        GovernanceUpdatersModule::class,
        ReferendumDetailsModule::class,
        ReferendumListModule::class
    ]
)
class GovernanceFeatureModule {

    @Provides
    @FeatureScope
    fun provideAssetSharedState(
        chainRegistry: ChainRegistry,
        preferences: Preferences,
    ) = GovernanceSharedState(chainRegistry, preferences)

    @Provides
    @FeatureScope
    fun provideTokenUseCase(
        tokenRepository: TokenRepository,
        sharedState: GovernanceSharedState,
    ): TokenUseCase = SharedStateTokenUseCase(tokenRepository, sharedState)

    @Provides
    @FeatureScope
    fun provideAssetUseCase(
        walletRepository: WalletRepository,
        accountRepository: AccountRepository,
        assetSharedState: GovernanceSharedState,
        chainRegistry: ChainRegistry
    ): AssetUseCase = AssetUseCaseImpl(
        walletRepository = walletRepository,
        accountRepository = accountRepository,
        sharedState = assetSharedState,
        chainRegistry = chainRegistry
    )

    @Provides
    @FeatureScope
    fun provideAssetSelectorMixinFactory(
        assetUseCase: AssetUseCase,
        assetSharedState: GovernanceSharedState,
        resourceManager: ResourceManager
    ): MixinFactory<AssetSelectorMixin.Presentation> = AssetSelectorFactory(
        assetUseCase = assetUseCase,
        singleAssetSharedState = assetSharedState,
        resourceManager = resourceManager
    )

    @Provides
    @FeatureScope
    fun provideFeeLoaderMixin(
        feeLoaderMixinFactory: FeeLoaderMixin.Factory,
        tokenUseCase: TokenUseCase,
    ): FeeLoaderMixin.Presentation = feeLoaderMixinFactory.create(tokenUseCase)

    @Provides
    @FeatureScope
    fun provideGovernanceSourceRegistry(
        @GovernanceV2 governanceV2Source: GovernanceSource,
        chainRegistry: ChainRegistry
    ): GovernanceSourceRegistry = RealGovernanceSourceRegistry(
        chainRegistry = chainRegistry,
        governanceV2Source = governanceV2Source
    )

    @Provides
    @FeatureScope
    fun providePreImageRepository(
        @Named(REMOTE_STORAGE_SOURCE) storageSource: StorageDataSource
    ): PreImageRepository = RealPreImageRepository(storageSource)

    @Provides
    @FeatureScope
    fun provideTreasuryRepository(
        @Named(REMOTE_STORAGE_SOURCE) storageSource: StorageDataSource
    ): TreasuryRepository = RealTreasuryRepository(storageSource)

    @Provides
    @FeatureScope
    fun provideReferendumConstructor(
        governanceSourceRegistry: GovernanceSourceRegistry,
        chainStateRepository: ChainStateRepository
    ): ReferendaConstructor = RealReferendaConstructor(governanceSourceRegistry, chainStateRepository)

    @Provides
    @FeatureScope
    fun provideGovernanceIdentityProviderFactory(
        @LocalIdentity localProvider: IdentityProvider,
        @OnChainIdentity onChainProvider: IdentityProvider
    ): GovernanceIdentityProviderFactory = GovernanceIdentityProviderFactory(
        localProvider = localProvider,
        onChainProvider = onChainProvider
    )
}
