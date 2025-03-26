package io.novafoundation.nova.feature_governance_impl.di.modules.screens

import dagger.Module
import dagger.Provides
import io.novafoundation.nova.common.data.memory.ComputationalCache
import io.novafoundation.nova.common.data.network.NetworkApiCreator
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.core_db.dao.TinderGovDao
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_governance_api.data.source.GovernanceSourceRegistry
import io.novafoundation.nova.feature_governance_impl.domain.summary.ReferendaSummaryInteractor
import io.novafoundation.nova.feature_governance_impl.domain.referendum.tindergov.TinderGovBasketInteractor
import io.novafoundation.nova.feature_governance_impl.domain.referendum.tindergov.TinderGovInteractor
import io.novafoundation.nova.feature_governance_impl.data.GovernanceSharedState
import io.novafoundation.nova.feature_governance_impl.data.offchain.referendum.summary.v2.RealReferendumSummaryDataSource
import io.novafoundation.nova.feature_governance_impl.data.offchain.referendum.summary.v2.ReferendumSummaryApi
import io.novafoundation.nova.feature_governance_impl.data.offchain.referendum.summary.v2.ReferendumSummaryDataSource
import io.novafoundation.nova.feature_governance_impl.data.repository.tindergov.RealTinderGovBasketRepository
import io.novafoundation.nova.feature_governance_impl.data.repository.tindergov.RealTinderGovVotingPowerRepository
import io.novafoundation.nova.feature_governance_impl.data.repository.tindergov.TinderGovBasketRepository
import io.novafoundation.nova.feature_governance_impl.data.repository.tindergov.TinderGovVotingPowerRepository
import io.novafoundation.nova.feature_governance_impl.domain.referendum.details.RealReferendumDetailsRepository
import io.novafoundation.nova.feature_governance_impl.domain.referendum.details.ReferendumDetailsRepository
import io.novafoundation.nova.feature_governance_impl.domain.referendum.details.call.ReferendumPreImageParser
import io.novafoundation.nova.feature_governance_impl.domain.referendum.list.ReferendaSharedComputation
import io.novafoundation.nova.feature_governance_impl.domain.referendum.list.filtering.ReferendaFilteringProvider
import io.novafoundation.nova.feature_governance_impl.domain.referendum.tindergov.RealTinderGovBasketInteractor
import io.novafoundation.nova.feature_governance_impl.domain.referendum.tindergov.RealTinderGovInteractor
import io.novafoundation.nova.feature_governance_impl.domain.summary.RealReferendaSummaryInteractor
import io.novafoundation.nova.feature_governance_impl.domain.summary.ReferendaSummarySharedComputation
import io.novafoundation.nova.feature_wallet_api.domain.AssetUseCase

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
    fun provideTinderGovBasketRepository(dao: TinderGovDao): TinderGovBasketRepository {
        return RealTinderGovBasketRepository(dao)
    }

    @Provides
    @FeatureScope
    fun provideTinderGovVotingPowerRepository(
        tinderGovDao: TinderGovDao
    ): TinderGovVotingPowerRepository {
        return RealTinderGovVotingPowerRepository(tinderGovDao)
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
        preImageParser: ReferendumPreImageParser,
        tinderGovVotingPowerRepository: TinderGovVotingPowerRepository,
        referendaFilteringProvider: ReferendaFilteringProvider,
        assetUseCase: AssetUseCase,
        governanceSourceRegistry: GovernanceSourceRegistry,
    ): TinderGovInteractor = RealTinderGovInteractor(
        governanceSharedState,
        referendaSharedComputation,
        accountRepository,
        preImageParser,
        tinderGovVotingPowerRepository,
        referendaFilteringProvider,
        governanceSourceRegistry,
        assetUseCase,
    )

    @Provides
    @FeatureScope
    fun provideReferendaSummarySharedComputation(
        computationalCache: ComputationalCache,
        referendumDetailsRepository: ReferendumDetailsRepository,
        accountRepository: AccountRepository
    ) = ReferendaSummarySharedComputation(
        computationalCache,
        referendumDetailsRepository,
        accountRepository
    )

    @Provides
    @FeatureScope
    fun provideReferendaSummaryInteractor(
        governanceSharedState: GovernanceSharedState,
        referendaSummarySharedComputation: ReferendaSummarySharedComputation
    ): ReferendaSummaryInteractor = RealReferendaSummaryInteractor(
        governanceSharedState,
        referendaSummarySharedComputation
    )

    @Provides
    @FeatureScope
    fun provideTinderGovBasketInteractor(
        governanceSharedState: GovernanceSharedState,
        accountRepository: AccountRepository,
        tinderGovBasketRepository: TinderGovBasketRepository,
        tinderGovVotingPowerRepository: TinderGovVotingPowerRepository,
        assetUseCase: AssetUseCase,
        tinderGovInteractor: TinderGovInteractor,
        governanceSourceRegistry: GovernanceSourceRegistry,
    ): TinderGovBasketInteractor = RealTinderGovBasketInteractor(
        governanceSharedState = governanceSharedState,
        accountRepository = accountRepository,
        tinderGovBasketRepository = tinderGovBasketRepository,
        tinderGovVotingPowerRepository = tinderGovVotingPowerRepository,
        assetUseCase = assetUseCase,
        tinderGovInteractor = tinderGovInteractor,
        governanceSourceRegistry = governanceSourceRegistry
    )
}
