package io.novafoundation.nova.feature_governance_impl.di.modules

import dagger.Module
import dagger.Provides
import io.novafoundation.nova.common.data.network.NetworkApiCreator
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.feature_governance_api.data.source.GovernanceSource
import io.novafoundation.nova.feature_governance_impl.data.offchain.v1.PolkassemblyV1Api
import io.novafoundation.nova.feature_governance_impl.data.repository.v1.Gov1OffChainReferendaInfoRepository
import io.novafoundation.nova.feature_governance_impl.data.repository.v1.GovV1ConvictionVotingRepository
import io.novafoundation.nova.feature_governance_impl.data.repository.v1.GovV1OnChainReferendaRepository
import io.novafoundation.nova.feature_governance_impl.data.repository.v1.GovV1PreImageRepository
import io.novafoundation.nova.feature_governance_impl.data.repository.v2.Gov2PreImageRepository
import io.novafoundation.nova.feature_governance_impl.data.source.StaticGovernanceSource
import io.novafoundation.nova.feature_wallet_api.data.repository.BalanceLocksRepository
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
        chainRegistry: ChainRegistry,
        balanceLocksRepository: BalanceLocksRepository
    ) = GovV1ConvictionVotingRepository(storageSource, chainRegistry, balanceLocksRepository)

    @Provides
    @FeatureScope
    fun providePolkassemblyApi(apiCreator: NetworkApiCreator) = apiCreator.create(PolkassemblyV1Api::class.java)

    @Provides
    @FeatureScope
    fun provideOffChainInfoRepository(polkassemblyApi: PolkassemblyV1Api) = Gov1OffChainReferendaInfoRepository(polkassemblyApi)

    @Provides
    @FeatureScope
    fun providePreImageRepository(
        @Named(REMOTE_STORAGE_SOURCE) storageSource: StorageDataSource,
        v2Delegate: Gov2PreImageRepository,
    ) = GovV1PreImageRepository(storageSource, v2Delegate)

    @Provides
    @FeatureScope
    @GovernanceV1
    fun provideGovernanceSource(
        referendaRepository: GovV1OnChainReferendaRepository,
        convictionVotingRepository: GovV1ConvictionVotingRepository,
        offChainInfoRepository: Gov1OffChainReferendaInfoRepository,
        preImageRepository: GovV1PreImageRepository
    ): GovernanceSource = StaticGovernanceSource(
        referenda = referendaRepository,
        convictionVoting = convictionVotingRepository,
        offChainInfo = offChainInfoRepository,
        preImageRepository = preImageRepository
    )
}
