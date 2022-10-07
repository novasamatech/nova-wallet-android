package io.novafoundation.nova.feature_governance_impl.di.modules

import dagger.Module
import dagger.Provides
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.feature_governance_api.data.source.GovernanceSource
import io.novafoundation.nova.feature_governance_impl.data.repository.v2.Gov2OffChainReferendaInfoRepository
import io.novafoundation.nova.feature_governance_impl.data.repository.v2.GovV2ConvictionVotingRepository
import io.novafoundation.nova.feature_governance_impl.data.repository.v2.GovV2OnChainReferendaRepository
import io.novafoundation.nova.feature_governance_impl.data.source.StaticGovernanceSource
import io.novafoundation.nova.runtime.di.REMOTE_STORAGE_SOURCE
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.storage.source.StorageDataSource
import javax.inject.Named
import javax.inject.Qualifier

@Qualifier
annotation class GovernanceV2

@Module
class V2GovernanceModule {

    @Provides
    @FeatureScope
    fun provideOnChainReferendaRepository(
        @Named(REMOTE_STORAGE_SOURCE) storageSource: StorageDataSource,
        chainRegistry: ChainRegistry
    ) = GovV2OnChainReferendaRepository(storageSource, chainRegistry)

    @Provides
    @FeatureScope
    fun provideConvictionVotingRepository(
        @Named(REMOTE_STORAGE_SOURCE) storageSource: StorageDataSource
    ) = GovV2ConvictionVotingRepository(storageSource)

    @Provides
    @FeatureScope
    fun provideOffChainInfoRepository() = Gov2OffChainReferendaInfoRepository()

    @Provides
    @FeatureScope
    @GovernanceV2
    fun provideGovernanceSource(
        referendaRepository: GovV2OnChainReferendaRepository,
        convictionVotingRepository: GovV2ConvictionVotingRepository,
        offChainInfoRepository: Gov2OffChainReferendaInfoRepository
    ): GovernanceSource = StaticGovernanceSource(referendaRepository, convictionVotingRepository, offChainInfoRepository)
}
