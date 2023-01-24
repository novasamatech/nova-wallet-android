package io.novafoundation.nova.feature_governance_impl.di.modules

import dagger.Module
import dagger.Provides
import io.novafoundation.nova.common.data.network.NetworkApiCreator
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.core_db.dao.GovernanceDAppsDao
import io.novafoundation.nova.feature_governance_api.data.source.GovernanceSource
import io.novafoundation.nova.feature_governance_impl.data.offchain.v2.delegation.metadata.DelegateMetadataApi
import io.novafoundation.nova.feature_governance_impl.data.offchain.v2.delegation.stats.DelegationsSubqueryApi
import io.novafoundation.nova.feature_governance_impl.data.offchain.v2.referendum.PolkassemblyV2Api
import io.novafoundation.nova.feature_governance_impl.data.preimage.PreImageSizer
import io.novafoundation.nova.feature_governance_impl.data.repository.v2.Gov2DelegationsRepository
import io.novafoundation.nova.feature_governance_impl.data.repository.v2.Gov2OffChainReferendaInfoRepository
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

@Module
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
    ) = GovV2ConvictionVotingRepository(storageSource, chainRegistry)

    @Provides
    @FeatureScope
    fun providePolkassemblyApi(apiCreator: NetworkApiCreator) = apiCreator.create(PolkassemblyV2Api::class.java)

    @Provides
    @FeatureScope
    fun provideOffChainInfoRepository(polkassemblyV2Api: PolkassemblyV2Api) = Gov2OffChainReferendaInfoRepository(polkassemblyV2Api)

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
        offChainInfoRepository: Gov2OffChainReferendaInfoRepository,
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
