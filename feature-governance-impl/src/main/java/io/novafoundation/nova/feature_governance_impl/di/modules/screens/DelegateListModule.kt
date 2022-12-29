package io.novafoundation.nova.feature_governance_impl.di.modules.screens

import dagger.Module
import dagger.Provides
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.feature_account_api.data.repository.OnChainIdentityRepository
import io.novafoundation.nova.feature_governance_api.data.source.GovernanceSourceRegistry
import io.novafoundation.nova.feature_governance_api.domain.delegation.delegate.list.DelegateListInteractor
import io.novafoundation.nova.feature_governance_impl.data.GovernanceSharedState
import io.novafoundation.nova.feature_governance_impl.domain.delegation.delegate.list.RealDelegateListInteractor
import io.novafoundation.nova.runtime.repository.ChainStateRepository

@Module
class DelegateListModule {

    @Provides
    @FeatureScope
    fun provideDelegateListInteractor(
        governanceSharedState: GovernanceSharedState,
        governanceSourceRegistry: GovernanceSourceRegistry,
        chainStateRepository: ChainStateRepository,
        identityRepository: OnChainIdentityRepository
    ): DelegateListInteractor = RealDelegateListInteractor(
        governanceSharedState = governanceSharedState,
        governanceSourceRegistry = governanceSourceRegistry,
        chainStateRepository = chainStateRepository,
        identityRepository = identityRepository
    )
}
