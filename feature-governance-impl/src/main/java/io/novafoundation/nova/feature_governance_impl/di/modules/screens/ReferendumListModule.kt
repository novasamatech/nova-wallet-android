package io.novafoundation.nova.feature_governance_impl.di.modules.screens

import dagger.Module
import dagger.Provides
import io.novafoundation.nova.common.data.memory.ComputationalCache
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.feature_account_api.data.repository.OnChainIdentityRepository
import io.novafoundation.nova.feature_governance_api.data.source.GovernanceSourceRegistry
import io.novafoundation.nova.feature_governance_api.domain.referendum.list.ReferendaListInteractor
import io.novafoundation.nova.feature_governance_impl.data.GovernanceSharedState
import io.novafoundation.nova.feature_governance_impl.data.repository.filters.PreferencesReferendaFiltersRepository
import io.novafoundation.nova.feature_governance_impl.data.repository.filters.ReferendaFiltersRepository
import io.novafoundation.nova.feature_governance_impl.domain.delegation.delegate.common.RecentVotesTimePointProvider
import io.novafoundation.nova.feature_governance_impl.domain.filters.RealReferendaFiltersInteractor
import io.novafoundation.nova.feature_governance_impl.domain.filters.ReferendaFiltersInteractor
import io.novafoundation.nova.feature_governance_impl.domain.referendum.common.ReferendaConstructor
import io.novafoundation.nova.feature_governance_impl.domain.referendum.list.RealReferendaListInteractor
import io.novafoundation.nova.feature_governance_impl.domain.referendum.list.ReferendaSharedComputation
import io.novafoundation.nova.feature_governance_impl.domain.referendum.list.filtering.RealReferendaFilteringProvider
import io.novafoundation.nova.feature_governance_impl.domain.referendum.list.filtering.ReferendaFilteringProvider
import io.novafoundation.nova.feature_governance_impl.domain.referendum.list.repository.RealReferendaCommonRepository
import io.novafoundation.nova.feature_governance_impl.domain.referendum.list.repository.ReferendaCommonRepository
import io.novafoundation.nova.feature_governance_impl.domain.referendum.list.sorting.RealReferendaSortingProvider
import io.novafoundation.nova.feature_governance_impl.domain.referendum.list.sorting.ReferendaSortingProvider
import io.novafoundation.nova.runtime.repository.ChainStateRepository

@Module
class ReferendumListModule {

    @Provides
    @FeatureScope
    fun provideReferendaFiltersRepository(): ReferendaFiltersRepository {
        return PreferencesReferendaFiltersRepository()
    }

    @Provides
    @FeatureScope
    fun provideReferendaFiltersInteractor(
        referendaFiltersRepository: ReferendaFiltersRepository
    ): ReferendaFiltersInteractor {
        return RealReferendaFiltersInteractor(referendaFiltersRepository)
    }

    @Provides
    @FeatureScope
    fun provideReferendaSortingProvider(): ReferendaSortingProvider {
        return RealReferendaSortingProvider()
    }

    @Provides
    @FeatureScope
    fun provideReferendaFilteringProvider(): ReferendaFilteringProvider {
        return RealReferendaFilteringProvider()
    }

    @Provides
    @FeatureScope
    fun provideReferendaCommonRepository(
        chainStateRepository: ChainStateRepository,
        governanceSourceRegistry: GovernanceSourceRegistry,
        referendaConstructor: ReferendaConstructor,
        referendaSortingProvider: ReferendaSortingProvider,
        identityRepository: OnChainIdentityRepository,
        recentVotesTimePointProvider: RecentVotesTimePointProvider
    ): ReferendaCommonRepository {
        return RealReferendaCommonRepository(
            chainStateRepository = chainStateRepository,
            governanceSourceRegistry = governanceSourceRegistry,
            referendaConstructor = referendaConstructor,
            referendaSortingProvider = referendaSortingProvider,
            identityRepository = identityRepository,
            recentVotesTimePointProvider = recentVotesTimePointProvider
        )
    }

    @Provides
    @FeatureScope
    fun provideReferendaSharedComputation(
        computationalCache: ComputationalCache,
        referendaCommonRepository: ReferendaCommonRepository
    ): ReferendaSharedComputation {
        return ReferendaSharedComputation(computationalCache, referendaCommonRepository)
    }

    @Provides
    @FeatureScope
    fun provideReferendaListInteractor(
        referendaCommonRepository: ReferendaCommonRepository,
        governanceSharedState: GovernanceSharedState,
        referendaSharedComputation: ReferendaSharedComputation,
        governanceSourceRegistry: GovernanceSourceRegistry,
        referendaSortingProvider: ReferendaSortingProvider,
        referendaFilteringProvider: ReferendaFilteringProvider
    ): ReferendaListInteractor = RealReferendaListInteractor(
        referendaCommonRepository = referendaCommonRepository,
        governanceSharedState = governanceSharedState,
        referendaSharedComputation = referendaSharedComputation,
        governanceSourceRegistry = governanceSourceRegistry,
        referendaSortingProvider = referendaSortingProvider,
        referendaFilteringProvider = referendaFilteringProvider
    )
}
