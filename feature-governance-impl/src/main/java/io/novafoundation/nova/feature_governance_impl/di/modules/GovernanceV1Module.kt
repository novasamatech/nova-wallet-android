package io.novafoundation.nova.feature_governance_impl.di.modules

import dagger.Module
import dagger.Provides
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.feature_governance_api.data.source.GovernanceSource
import io.novafoundation.nova.feature_governance_impl.data.repository.v1.Gov1OffChainReferendaInfoRepository
import io.novafoundation.nova.feature_governance_impl.data.repository.v1.GovV1ConvictionVotingRepository
import io.novafoundation.nova.feature_governance_impl.data.repository.v1.GovV1OnChainReferendaRepository
import io.novafoundation.nova.feature_governance_impl.data.source.StaticGovernanceSource
import io.novafoundation.nova.runtime.di.REMOTE_STORAGE_SOURCE
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.storage.source.StorageDataSource
import javax.inject.Named
import javax.inject.Qualifier

@Qualifier
annotation class GovernanceV1

@Module
class GovernanceV1Module {

    @Provides
    @FeatureScope
    fun provideOnChainReferendaRepository(
        @Named(REMOTE_STORAGE_SOURCE) storageSource: StorageDataSource,
        chainRegistry: ChainRegistry
    ) = GovV1OnChainReferendaRepository(storageSource, chainRegistry)

    @Provides
    @FeatureScope
    fun provideConvictionVotingRepository(
        @Named(REMOTE_STORAGE_SOURCE) storageSource: StorageDataSource,
        chainRegistry: ChainRegistry
    ) = GovV1ConvictionVotingRepository(storageSource, chainRegistry)

    @Provides
    @FeatureScope
    fun provideOffChainInfoRepository() = Gov1OffChainReferendaInfoRepository()

    @Provides
    @FeatureScope
    @GovernanceV1
    fun provideGovernanceSource(
        referendaRepository: GovV1OnChainReferendaRepository,
        convictionVotingRepository: GovV1ConvictionVotingRepository,
        offChainInfoRepository: Gov1OffChainReferendaInfoRepository,
    ): GovernanceSource = StaticGovernanceSource(
        referenda = referendaRepository,
        convictionVoting = convictionVotingRepository,
        offChainInfo = offChainInfoRepository
    )
}
