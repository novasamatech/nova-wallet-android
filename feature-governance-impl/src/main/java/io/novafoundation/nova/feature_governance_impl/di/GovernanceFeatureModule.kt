package io.novafoundation.nova.feature_governance_impl.di

import dagger.Module
import dagger.Provides
import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.data.storage.Preferences
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_account_api.domain.account.identity.IdentityProvider
import io.novafoundation.nova.feature_account_api.domain.account.identity.LocalIdentity
import io.novafoundation.nova.feature_account_api.domain.account.identity.OnChainIdentity
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_governance_api.data.repository.TreasuryRepository
import io.novafoundation.nova.feature_governance_api.data.source.GovernanceSource
import io.novafoundation.nova.feature_governance_api.data.source.GovernanceSourceRegistry
import io.novafoundation.nova.feature_governance_impl.data.GovernanceSharedState
import io.novafoundation.nova.feature_governance_impl.data.preimage.PreImageSizer
import io.novafoundation.nova.feature_governance_impl.data.preimage.RealPreImageSizer
import io.novafoundation.nova.feature_governance_impl.data.repository.RealTreasuryRepository
import io.novafoundation.nova.feature_governance_impl.data.source.RealGovernanceSourceRegistry
import io.novafoundation.nova.feature_governance_impl.di.modules.GovernanceDAppsModule
import io.novafoundation.nova.feature_governance_impl.di.modules.GovernanceUpdatersModule
import io.novafoundation.nova.feature_governance_impl.di.modules.GovernanceV1
import io.novafoundation.nova.feature_governance_impl.di.modules.GovernanceV1Module
import io.novafoundation.nova.feature_governance_impl.di.modules.GovernanceV2
import io.novafoundation.nova.feature_governance_impl.di.modules.GovernanceV2Module
import io.novafoundation.nova.feature_governance_impl.di.modules.screens.DelegateModule
import io.novafoundation.nova.feature_governance_impl.di.modules.screens.ReferendumDetailsModule
import io.novafoundation.nova.feature_governance_impl.di.modules.screens.ReferendumListModule
import io.novafoundation.nova.feature_governance_impl.di.modules.screens.ReferendumUnlockModule
import io.novafoundation.nova.feature_governance_impl.di.modules.screens.ReferendumVoteModule
import io.novafoundation.nova.feature_governance_impl.di.modules.screens.ReferendumVotersModule
import io.novafoundation.nova.feature_governance_impl.domain.identity.GovernanceIdentityProviderFactory
import io.novafoundation.nova.feature_governance_impl.domain.referendum.common.RealReferendaConstructor
import io.novafoundation.nova.feature_governance_impl.domain.referendum.common.ReferendaConstructor
import io.novafoundation.nova.feature_governance_impl.domain.track.category.RealTrackCategorizer
import io.novafoundation.nova.feature_governance_impl.domain.track.category.TrackCategorizer
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.common.RealReferendumFormatter
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.common.ReferendumFormatter
import io.novafoundation.nova.feature_governance_impl.presentation.track.RealTrackFormatter
import io.novafoundation.nova.feature_governance_impl.presentation.track.TrackFormatter
import io.novafoundation.nova.feature_governance_impl.presentation.voters.RealVotersFormatter
import io.novafoundation.nova.feature_governance_impl.presentation.voters.VotersFormatter
import io.novafoundation.nova.feature_wallet_api.domain.AssetUseCase
import io.novafoundation.nova.feature_wallet_api.domain.TokenUseCase
import io.novafoundation.nova.feature_wallet_api.domain.implementations.AssetUseCaseImpl
import io.novafoundation.nova.feature_wallet_api.domain.implementations.SharedStateTokenUseCase
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.TokenRepository
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.WalletRepository
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.assetSelector.AssetSelectorFactory
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
        GovernanceV1Module::class,
        GovernanceUpdatersModule::class,
        ReferendumDetailsModule::class,
        ReferendumListModule::class,
        ReferendumVotersModule::class,
        ReferendumVoteModule::class,
        ReferendumUnlockModule::class,
        DelegateModule::class,
        GovernanceDAppsModule::class
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
    ): AssetUseCase = AssetUseCaseImpl(
        walletRepository = walletRepository,
        accountRepository = accountRepository,
        sharedState = assetSharedState,
    )

    @Provides
    @FeatureScope
    fun provideAssetSelectorMixinFactory(
        assetUseCase: AssetUseCase,
        assetSharedState: GovernanceSharedState,
        resourceManager: ResourceManager
    ) = AssetSelectorFactory(
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
        @GovernanceV1 governanceV1Source: GovernanceSource,
    ): GovernanceSourceRegistry = RealGovernanceSourceRegistry(
        governanceV2Source = governanceV2Source,
        governanceV1Source = governanceV1Source
    )

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

    @Provides
    @FeatureScope
    fun providePreImageSizer(): PreImageSizer = RealPreImageSizer()

    @Provides
    @FeatureScope
    fun provideTrackCategorizer(): TrackCategorizer = RealTrackCategorizer()

    @Provides
    @FeatureScope
    fun provideTrackFormatter(
        trackCategorizer: TrackCategorizer,
        resourceManager: ResourceManager
    ): TrackFormatter = RealTrackFormatter(trackCategorizer, resourceManager)

    @Provides
    @FeatureScope
    fun provideReferendumFormatter(
        resourceManager: ResourceManager,
        trackFormatter: TrackFormatter,
    ): ReferendumFormatter = RealReferendumFormatter(resourceManager, trackFormatter)

    @Provides
    @FeatureScope
    fun provideVotersFormatter(
        resourceManager: ResourceManager,
        addressIconGenerator: AddressIconGenerator,
    ): VotersFormatter = RealVotersFormatter(addressIconGenerator, resourceManager)
}
