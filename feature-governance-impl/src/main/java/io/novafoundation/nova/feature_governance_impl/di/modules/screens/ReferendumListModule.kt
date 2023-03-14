package io.novafoundation.nova.feature_governance_impl.di.modules.screens

import dagger.Module
import dagger.Provides
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.feature_account_api.data.repository.OnChainIdentityRepository
import io.novafoundation.nova.feature_governance_api.data.source.GovernanceSourceRegistry
import io.novafoundation.nova.feature_governance_api.domain.referendum.list.ReferendaListInteractor
import io.novafoundation.nova.feature_governance_impl.data.GovernanceSharedState
import io.novafoundation.nova.feature_governance_impl.domain.referendum.common.ReferendaConstructor
import io.novafoundation.nova.feature_governance_impl.domain.referendum.list.RealReferendaListInteractor
import io.novafoundation.nova.feature_governance_impl.domain.referendum.list.sorting.RealReferendaSortingProvider
import io.novafoundation.nova.feature_governance_impl.domain.referendum.list.sorting.ReferendaSortingProvider
import io.novafoundation.nova.runtime.repository.ChainStateRepository

@Module
class ReferendumListModule {

    @Provides
    @FeatureScope
    fun provideReferendaSortingProvider(): ReferendaSortingProvider {
        return RealReferendaSortingProvider()
    }

    @Provides
    @FeatureScope
    fun provideReferendaListInteractor(
        chainStateRepository: ChainStateRepository,
        governanceSourceRegistry: GovernanceSourceRegistry,
        referendaConstructor: ReferendaConstructor,
        referendaSortingProvider: ReferendaSortingProvider,
        identityRepository: OnChainIdentityRepository,
        governanceSharedState: GovernanceSharedState
    ): ReferendaListInteractor = RealReferendaListInteractor(
        chainStateRepository = chainStateRepository,
        governanceSourceRegistry = governanceSourceRegistry,
        referendaConstructor = referendaConstructor,
        referendaSortingProvider = referendaSortingProvider,
        identityRepository = identityRepository,
        governanceSharedState = governanceSharedState
    )
}
