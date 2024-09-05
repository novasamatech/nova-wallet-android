package io.novafoundation.nova.feature_governance_impl.di.modules.screens

import dagger.Module
import dagger.Provides
import io.novafoundation.nova.common.data.network.NetworkApiCreator
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_governance_api.domain.tindergov.TinderGovInteractor
import io.novafoundation.nova.feature_governance_impl.data.GovernanceSharedState
import io.novafoundation.nova.feature_governance_impl.data.offchain.referendum.summary.v2.RealReferendumSummaryDataSource
import io.novafoundation.nova.feature_governance_impl.data.offchain.referendum.summary.v2.ReferendumSummaryApi
import io.novafoundation.nova.feature_governance_impl.data.offchain.referendum.summary.v2.ReferendumSummaryDataSource
import io.novafoundation.nova.feature_governance_impl.domain.referendum.details.RealReferendumDetailsRepository
import io.novafoundation.nova.feature_governance_impl.domain.referendum.details.ReferendumDetailsRepository
import io.novafoundation.nova.feature_governance_impl.domain.referendum.details.call.ReferendumPreImageParser
import io.novafoundation.nova.feature_governance_impl.domain.referendum.list.ReferendaSharedComputation
import io.novafoundation.nova.feature_governance_impl.domain.referendum.list.filtering.ReferendaFilteringProvider
import io.novafoundation.nova.feature_governance_impl.domain.referendum.tindergov.RealTinderGovInteractor

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
    fun provideReferendumDetailsRepository(
        dataSource: ReferendumSummaryDataSource
    ): ReferendumDetailsRepository {
        return RealReferendumDetailsRepository(dataSource)
    }

    @Provides
    @FeatureScope
    fun provideTinderGovInteractor(
        governanceSharedState: GovernanceSharedState,
        referendaSharedComputation: ReferendaSharedComputation,
        accountRepository: AccountRepository,
        referendumDetailsRepository: ReferendumDetailsRepository,
        preImageParser: ReferendumPreImageParser,
        referendaFilteringProvider: ReferendaFilteringProvider
    ): TinderGovInteractor = RealTinderGovInteractor(
        governanceSharedState,
        referendaSharedComputation,
        accountRepository,
        referendumDetailsRepository,
        preImageParser,
        referendaFilteringProvider
    )
}
