package io.novafoundation.nova.feature_governance_impl.di.modules.screens

import dagger.Module
import dagger.Provides
import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_account_api.data.repository.OnChainIdentityRepository
import io.novafoundation.nova.feature_governance_api.data.source.GovernanceSourceRegistry
import io.novafoundation.nova.feature_governance_api.domain.delegation.delegate.list.DelegateListInteractor
import io.novafoundation.nova.feature_governance_impl.domain.delegation.delegate.list.RealDelegateListInteractor
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
    fun provideDelegateMappers(
        resourceManager: ResourceManager,
        addressIconGenerator: AddressIconGenerator,
    ): DelegateMappers = RealDelegateMappers(resourceManager, addressIconGenerator)
}
