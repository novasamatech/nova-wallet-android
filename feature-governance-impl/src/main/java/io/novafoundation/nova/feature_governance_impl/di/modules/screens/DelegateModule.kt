package io.novafoundation.nova.feature_governance_impl.di.modules.screens

import dagger.Module
import dagger.Provides
import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_account_api.data.repository.OnChainIdentityRepository
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_governance_api.data.source.GovernanceSourceRegistry
import io.novafoundation.nova.feature_governance_api.domain.delegation.delegate.details.model.DelegateDetailsInteractor
import io.novafoundation.nova.feature_governance_api.domain.delegation.delegate.list.DelegateListInteractor
import io.novafoundation.nova.feature_governance_api.domain.delegation.delegation.create.chooseTrack.NewDelegationChooseTrackInteractor
import io.novafoundation.nova.feature_governance_impl.data.GovernanceSharedState
import io.novafoundation.nova.feature_governance_impl.domain.delegation.delegate.details.RealDelegateDetailsInteractor
import io.novafoundation.nova.feature_governance_impl.domain.delegation.delegate.list.RealDelegateListInteractor
import io.novafoundation.nova.feature_governance_impl.domain.delegation.delegation.create.chooseTrack.RealNewDelegationChooseTrackInteractor
import io.novafoundation.nova.feature_governance_impl.domain.track.category.TrackCategorizer
import io.novafoundation.nova.feature_governance_impl.presentation.delegation.delegate.common.DelegateMappers
import io.novafoundation.nova.feature_governance_impl.presentation.delegation.delegate.common.RealDelegateMappers
import io.novafoundation.nova.runtime.repository.ChainStateRepository

@Module
class DelegateModule {

    @Provides
    @FeatureScope
    fun provideDelegateListInteractor(
        governanceSourceRegistry: GovernanceSourceRegistry,
        chainStateRepository: ChainStateRepository,
        identityRepository: OnChainIdentityRepository
    ): DelegateListInteractor = RealDelegateListInteractor(
        governanceSourceRegistry = governanceSourceRegistry,
        chainStateRepository = chainStateRepository,
        identityRepository = identityRepository
    )

    @Provides
    @FeatureScope
    fun provideDelegateDetailsInteractor(
        governanceSourceRegistry: GovernanceSourceRegistry,
        chainStateRepository: ChainStateRepository,
        identityRepository: OnChainIdentityRepository
    ): DelegateDetailsInteractor = RealDelegateDetailsInteractor(
        governanceSourceRegistry = governanceSourceRegistry,
        chainStateRepository = chainStateRepository,
        identityRepository = identityRepository
    )

    @Provides
    @FeatureScope
    fun provideNewDelegationChooseTrackInteractor(
        governanceSharedState: GovernanceSharedState,
        governanceSourceRegistry: GovernanceSourceRegistry,
        chainStateRepository: ChainStateRepository,
        accountRepository: AccountRepository,
        trackCategorizer: TrackCategorizer,
    ): NewDelegationChooseTrackInteractor = RealNewDelegationChooseTrackInteractor(
        governanceSharedState = governanceSharedState,
        governanceSourceRegistry = governanceSourceRegistry,
        chainStateRepository = chainStateRepository,
        accountRepository = accountRepository,
        trackCategorizer = trackCategorizer
    )

    @Provides
    @FeatureScope
    fun provideDelegateMappers(
        resourceManager: ResourceManager,
        addressIconGenerator: AddressIconGenerator,
    ): DelegateMappers = RealDelegateMappers(resourceManager, addressIconGenerator)
}
