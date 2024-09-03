package io.novafoundation.nova.feature_governance_impl.di.modules.screens

import dagger.Module
import dagger.Provides
import io.novafoundation.nova.common.data.memory.ComputationalCache
import io.novafoundation.nova.common.data.network.NetworkApiCreator
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.feature_account_api.data.repository.OnChainIdentityRepository
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_governance_api.data.source.GovernanceSourceRegistry
import io.novafoundation.nova.feature_governance_api.domain.referendum.list.ReferendaListInteractor
import io.novafoundation.nova.feature_governance_api.domain.tindergov.TinderGovInteractor
import io.novafoundation.nova.feature_governance_impl.data.GovernanceSharedState
import io.novafoundation.nova.feature_governance_impl.data.offchain.delegation.v2.stats.DelegationsSubqueryApi
import io.novafoundation.nova.feature_governance_impl.data.offchain.referendum.summary.v2.RealReferendumSummaryDataSource
import io.novafoundation.nova.feature_governance_impl.data.offchain.referendum.summary.v2.ReferendumSummaryApi
import io.novafoundation.nova.feature_governance_impl.data.offchain.referendum.summary.v2.ReferendumSummaryDataSource
import io.novafoundation.nova.feature_governance_impl.data.repository.filters.PreferencesReferendaFiltersRepository
import io.novafoundation.nova.feature_governance_impl.data.repository.filters.ReferendaFiltersRepository
import io.novafoundation.nova.feature_governance_impl.domain.filters.RealReferendaFiltersInteractor
import io.novafoundation.nova.feature_governance_impl.domain.filters.ReferendaFiltersInteractor
import io.novafoundation.nova.feature_governance_impl.domain.referendum.common.ReferendaConstructor
import io.novafoundation.nova.feature_governance_impl.domain.referendum.details.call.ReferendumPreImageParser
import io.novafoundation.nova.feature_governance_impl.domain.referendum.list.RealReferendaListInteractor
import io.novafoundation.nova.feature_governance_impl.domain.referendum.list.ReferendaSharedComputation
import io.novafoundation.nova.feature_governance_impl.domain.referendum.list.repository.RealReferendaCommonRepository
import io.novafoundation.nova.feature_governance_impl.domain.referendum.list.repository.ReferendaCommonRepository
import io.novafoundation.nova.feature_governance_impl.domain.referendum.list.sorting.RealReferendaSortingProvider
import io.novafoundation.nova.feature_governance_impl.domain.referendum.list.sorting.ReferendaSortingProvider
import io.novafoundation.nova.feature_governance_impl.domain.referendum.tindergov.RealTinderGovInteractor
import io.novafoundation.nova.runtime.repository.ChainStateRepository

@Module
class TinderGovModule {

    @Provides
    @FeatureScope
    fun provideReferendumSummaryApi(apiCreator: NetworkApiCreator): ReferendumSummaryApi {
        return apiCreator.create(ReferendumSummaryApi::class.java)
    }

    @Provides
    @FeatureScope
    fun provideSummaryDataSource(
        referendumSummaryApi: ReferendumSummaryApi
    ): ReferendumSummaryDataSource {
        return RealReferendumSummaryDataSource(referendumSummaryApi)
    }

    @Provides
    @FeatureScope
    fun provideTinderGovInteractor(
        governanceSharedState: GovernanceSharedState,
        referendaCommonRepository: ReferendaCommonRepository,
        referendaSharedComputation: ReferendaSharedComputation,
        accountRepository: AccountRepository,
        referendumSummaryDataSource: ReferendumSummaryDataSource,
        preImageParser: ReferendumPreImageParser,
    ): TinderGovInteractor = RealTinderGovInteractor(
        governanceSharedState,
        referendaCommonRepository,
        referendaSharedComputation,
        accountRepository,
        referendumSummaryDataSource,
        preImageParser
    )
}
