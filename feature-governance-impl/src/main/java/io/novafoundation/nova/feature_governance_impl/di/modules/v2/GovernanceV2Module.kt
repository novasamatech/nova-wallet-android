package io.novafoundation.nova.feature_governance_impl.di.modules.v2

import dagger.Module
import dagger.Provides
import io.novafoundation.nova.common.data.network.NetworkApiCreator
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.core_db.dao.GovernanceDAppsDao
import io.novafoundation.nova.feature_governance_api.data.source.GovernanceSource
import io.novafoundation.nova.feature_governance_impl.data.offchain.referendum.polkassembly.v2.PolkassemblyV2ReferendaDataSource
import io.novafoundation.nova.feature_governance_impl.data.offchain.referendum.subsquare.v2.SubSquareV2ReferendaDataSource
import io.novafoundation.nova.feature_governance_impl.data.offchain.delegation.v2.metadata.DelegateMetadataApi
import io.novafoundation.nova.feature_governance_impl.data.offchain.delegation.v2.stats.DelegationsSubqueryApi
import io.novafoundation.nova.feature_governance_impl.data.preimage.PreImageSizer
import io.novafoundation.nova.feature_governance_impl.data.repository.MultiSourceOffChainReferendaInfoRepository
import io.novafoundation.nova.feature_governance_impl.data.repository.v2.Gov2DelegationsRepository
import io.novafoundation.nova.feature_governance_impl.data.repository.v2.Gov2PreImageRepository
import io.novafoundation.nova.feature_governance_impl.data.repository.v2.GovV2ConvictionVotingRepository
import io.novafoundation.nova.feature_governance_impl.data.repository.v2.GovV2DAppsRepository
import io.novafoundation.nova.feature_governance_impl.data.repository.v2.GovV2OnChainReferendaRepository
import io.novafoundation.nova.feature_governance_impl.data.source.StaticGovernanceSource
import io.novafoundation.nova.runtime.di.REMOTE_STORAGE_SOURCE
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.repository.TotalIssuanceRepository
import io.novafoundation.nova.runtime.storage.source.StorageDataSource
import javax.inject.Named
import javax.inject.Qualifier

@Qualifier
annotation class GovernanceV2

@Module(includes = [PolkassemblyV2Module::class, SubSquareV2Module::class])
class GovernanceV2Module {

    @Provides
    @FeatureScope
    fun provideOnChainReferendaRepository(
        @Named(REMOTE_STORAGE_SOURCE) storageSource: StorageDataSource,
        chainRegistry: ChainRegistry,
        totalIssuanceRepository: TotalIssuanceRepository
    ) = GovV2OnChainReferendaRepository(storageSource, chainRegistry, totalIssuanceRepository)

    @Provides
    @FeatureScope
    fun provideConvictionVotingRepository(
        @Named(REMOTE_STORAGE_SOURCE) storageSource: StorageDataSource,
        chainRegistry: ChainRegistry,
        delegateSubqueryApi: DelegationsSubqueryApi
    ) = GovV2ConvictionVotingRepository(storageSource, chainRegistry, delegateSubqueryApi)

    @Provides
    @GovernanceV2
    @FeatureScope
    fun provideOffChainInfoRepository(
        polkassemblyV2ReferendaDataSource: PolkassemblyV2ReferendaDataSource,
        subSquareV2ReferendaDataSource: SubSquareV2ReferendaDataSource
    ) = MultiSourceOffChainReferendaInfoRepository(
        subSquareReferendaDataSource = subSquareV2ReferendaDataSource,
        polkassemblyReferendaDataSource = polkassemblyV2ReferendaDataSource
    )

    @Provides
    @FeatureScope
    fun providePreImageRepository(
        @Named(REMOTE_STORAGE_SOURCE) storageSource: StorageDataSource,
        preImageSizer: PreImageSizer,
    ) = Gov2PreImageRepository(storageSource, preImageSizer)

    @Provides
    @FeatureScope
    fun provideDappsRepository(governanceDAppsDao: GovernanceDAppsDao): GovV2DAppsRepository {
        return GovV2DAppsRepository(governanceDAppsDao)
    }

    @Provides
    @FeatureScope
    fun provideDelegationStatsApi(apiCreator: NetworkApiCreator): DelegationsSubqueryApi {
        return apiCreator.create(DelegationsSubqueryApi::class.java)
    }

    @Provides
    @FeatureScope
    fun provideDelegateMetadataApi(apiCreator: NetworkApiCreator): DelegateMetadataApi {
        return apiCreator.create(DelegateMetadataApi::class.java, DelegateMetadataApi.BASE_URL)
    }

    @Provides
    @FeatureScope
    fun provideDelegationsRepository(
        delegationStatsApi: DelegationsSubqueryApi,
        delegateMetadataApi: DelegateMetadataApi
    ) = Gov2DelegationsRepository(delegationStatsApi, delegateMetadataApi)

    @Provides
    @FeatureScope
    @GovernanceV2
    fun provideGovernanceSource(
        referendaRepository: GovV2OnChainReferendaRepository,
        convictionVotingRepository: GovV2ConvictionVotingRepository,
        @GovernanceV2 offChainInfoRepository: MultiSourceOffChainReferendaInfoRepository,
        preImageRepository: Gov2PreImageRepository,
        governanceV2DappsRepository: GovV2DAppsRepository,
        delegationsRepository: Gov2DelegationsRepository,
    ): GovernanceSource = StaticGovernanceSource(
        referenda = referendaRepository,
        convictionVoting = convictionVotingRepository,
        offChainInfo = offChainInfoRepository,
        preImageRepository = preImageRepository,
        dappsRepository = governanceV2DappsRepository,
        delegationsRepository = delegationsRepository,
    )
}
