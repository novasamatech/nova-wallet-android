package io.novafoundation.nova.feature_governance_impl.di.modules.screens

import dagger.Module
import dagger.Provides
import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.data.memory.ComputationalCache
import io.novafoundation.nova.common.data.repository.BannerVisibilityRepository
import io.novafoundation.nova.common.data.storage.Preferences
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.validation.ValidationSystem
import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicService
import io.novafoundation.nova.feature_account_api.data.repository.OnChainIdentityRepository
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_governance_api.data.source.GovernanceSourceRegistry
import io.novafoundation.nova.feature_governance_api.domain.delegation.delegate.delegators.DelegateDelegatorsInteractor
import io.novafoundation.nova.feature_governance_api.domain.delegation.delegate.details.model.DelegateDetailsInteractor
import io.novafoundation.nova.feature_governance_api.domain.delegation.delegate.label.DelegateLabelUseCase
import io.novafoundation.nova.feature_governance_api.domain.delegation.delegate.list.DelegateListInteractor
import io.novafoundation.nova.feature_governance_api.domain.delegation.delegation.common.chooseTrack.ChooseTrackInteractor
import io.novafoundation.nova.feature_governance_api.domain.delegation.delegation.create.chooseAmount.NewDelegationChooseAmountInteractor
import io.novafoundation.nova.feature_governance_impl.data.GovernanceSharedState
import io.novafoundation.nova.feature_governance_impl.data.repository.RealRemoveVotesSuggestionRepository
import io.novafoundation.nova.feature_governance_impl.data.repository.RemoveVotesSuggestionRepository
import io.novafoundation.nova.feature_governance_impl.domain.delegation.delegate.common.repository.DelegateCommonRepository
import io.novafoundation.nova.feature_governance_impl.domain.delegation.delegate.common.repository.RealDelegateCommonRepository
import io.novafoundation.nova.feature_governance_impl.domain.delegation.delegate.delegators.RealDelegateDelegatorsInteractor
import io.novafoundation.nova.feature_governance_impl.domain.delegation.delegate.details.RealDelegateDetailsInteractor
import io.novafoundation.nova.feature_governance_impl.domain.delegation.delegate.label.RealDelegateLabelUseCase
import io.novafoundation.nova.feature_governance_impl.domain.delegation.delegate.list.RealDelegateListInteractor
import io.novafoundation.nova.feature_governance_impl.domain.delegation.delegation.create.chooseAmount.RealNewDelegationChooseAmountInteractor
import io.novafoundation.nova.feature_governance_impl.domain.delegation.delegation.create.chooseAmount.validation.ChooseDelegationAmountValidationSystem
import io.novafoundation.nova.feature_governance_impl.domain.delegation.delegation.create.chooseAmount.validation.chooseDelegationAmount
import io.novafoundation.nova.feature_governance_impl.domain.delegation.delegation.create.chooseTrack.RealChooseTrackInteractor
import io.novafoundation.nova.feature_governance_impl.domain.track.TracksUseCase
import io.novafoundation.nova.feature_governance_impl.domain.track.category.TrackCategorizer
import io.novafoundation.nova.feature_governance_impl.presentation.common.voters.VotersFormatter
import io.novafoundation.nova.feature_governance_impl.presentation.delegation.delegate.common.DelegateMappers
import io.novafoundation.nova.feature_governance_impl.presentation.delegation.delegate.common.RealDelegateMappers
import io.novafoundation.nova.feature_governance_impl.presentation.delegation.delegate.detail.DelegatesSharedComputation
import io.novafoundation.nova.feature_governance_impl.presentation.track.TrackFormatter
import io.novafoundation.nova.feature_wallet_api.data.repository.BalanceLocksRepository
import io.novafoundation.nova.runtime.repository.ChainStateRepository

@Module
class DelegateModule {

    @Provides
    @FeatureScope
    fun provideDelegateCommonRepository(
        governanceSourceRegistry: GovernanceSourceRegistry,
        chainStateRepository: ChainStateRepository,
        accountRepository: AccountRepository
    ): DelegateCommonRepository = RealDelegateCommonRepository(
        governanceSourceRegistry = governanceSourceRegistry,
        chainStateRepository = chainStateRepository,
        accountRepository = accountRepository
    )

    @Provides
    @FeatureScope
    fun provideDelegateListInteractor(
        bannerVisibilityRepository: BannerVisibilityRepository,
        delegatesSharedComputation: DelegatesSharedComputation
    ): DelegateListInteractor = RealDelegateListInteractor(
        bannerVisibilityRepository = bannerVisibilityRepository,
        delegatesSharedComputation = delegatesSharedComputation
    )

    @Provides
    @FeatureScope
    fun provideDelegateDetailsInteractor(
        governanceSourceRegistry: GovernanceSourceRegistry,
        chainStateRepository: ChainStateRepository,
        identityRepository: OnChainIdentityRepository,
        governanceSharedState: GovernanceSharedState,
        accountRepository: AccountRepository,
        tracksUseCase: TracksUseCase,
    ): DelegateDetailsInteractor = RealDelegateDetailsInteractor(
        governanceSourceRegistry = governanceSourceRegistry,
        chainStateRepository = chainStateRepository,
        identityRepository = identityRepository,
        governanceSharedState = governanceSharedState,
        accountRepository = accountRepository,
        tracksUseCase = tracksUseCase
    )

    @Provides
    @FeatureScope
    fun provideNewDelegationChooseTrackInteractor(
        governanceSharedState: GovernanceSharedState,
        governanceSourceRegistry: GovernanceSourceRegistry,
        chainStateRepository: ChainStateRepository,
        accountRepository: AccountRepository,
        trackCategorizer: TrackCategorizer,
        removeVotesSuggestionRepository: RemoveVotesSuggestionRepository,
    ): ChooseTrackInteractor = RealChooseTrackInteractor(
        governanceSharedState = governanceSharedState,
        governanceSourceRegistry = governanceSourceRegistry,
        chainStateRepository = chainStateRepository,
        accountRepository = accountRepository,
        trackCategorizer = trackCategorizer,
        removeVotesSuggestionRepository = removeVotesSuggestionRepository
    )

    @Provides
    @FeatureScope
    fun provideDelegateDelegatorsInteractor(
        governanceSharedState: GovernanceSharedState,
        governanceSourceRegistry: GovernanceSourceRegistry,
        identityRepository: OnChainIdentityRepository,
    ): DelegateDelegatorsInteractor = RealDelegateDelegatorsInteractor(
        governanceSharedState = governanceSharedState,
        governanceSourceRegistry = governanceSourceRegistry,
        identityRepository = identityRepository
    )

    @Provides
    @FeatureScope
    fun provideDelegateMappers(
        resourceManager: ResourceManager,
        addressIconGenerator: AddressIconGenerator,
        trackFormatter: TrackFormatter,
        votersFormatter: VotersFormatter
    ): DelegateMappers = RealDelegateMappers(resourceManager, addressIconGenerator, trackFormatter, votersFormatter)

    @Provides
    @FeatureScope
    fun provideNewDelegationChooseAmountInteractor(
        governanceSourceRegistry: GovernanceSourceRegistry,
        chainStateRepository: ChainStateRepository,
        selectedChainState: GovernanceSharedState,
        extrinsicService: ExtrinsicService,
        locksRepository: BalanceLocksRepository,
        computationalCache: ComputationalCache,
        accountRepository: AccountRepository,
    ): NewDelegationChooseAmountInteractor {
        return RealNewDelegationChooseAmountInteractor(
            governanceSourceRegistry = governanceSourceRegistry,
            chainStateRepository = chainStateRepository,
            selectedChainState = selectedChainState,
            extrinsicService = extrinsicService,
            locksRepository = locksRepository,
            computationalCache = computationalCache,
            accountRepository = accountRepository
        )
    }

    @Provides
    @FeatureScope
    fun provideNewDelegationChooseAmountValidationSystem(
        governanceSharedState: GovernanceSharedState,
        accountRepository: AccountRepository
    ): ChooseDelegationAmountValidationSystem {
        return ValidationSystem.chooseDelegationAmount(governanceSharedState, accountRepository)
    }

    @Provides
    @FeatureScope
    fun provideDelegateLabelUseCase(
        governanceSharedState: GovernanceSharedState,
        governanceSourceRegistry: GovernanceSourceRegistry,
        identityRepository: OnChainIdentityRepository,
    ): DelegateLabelUseCase = RealDelegateLabelUseCase(governanceSharedState, governanceSourceRegistry, identityRepository)

    @Provides
    @FeatureScope
    fun provideRemoveVotesSuggestionRepository(preferences: Preferences): RemoveVotesSuggestionRepository = RealRemoveVotesSuggestionRepository(preferences)
}
