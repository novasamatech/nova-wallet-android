package io.novafoundation.nova.feature_governance_impl.di.modules.v1

import dagger.Module
import dagger.Provides
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.core_db.dao.GovernanceDAppsDao
import io.novafoundation.nova.feature_governance_api.data.source.GovernanceSource
import io.novafoundation.nova.feature_governance_impl.data.offchain.referendum.polkassembly.v1.PolkassemblyV1ReferendaDataSource
import io.novafoundation.nova.feature_governance_impl.data.offchain.referendum.subsquare.v1.SubSquareV1ReferendaDataSource
import io.novafoundation.nova.feature_governance_impl.data.repository.MultiSourceOffChainReferendaInfoRepository
import io.novafoundation.nova.feature_governance_impl.data.repository.UnsupportedDelegationsRepository
import io.novafoundation.nova.feature_governance_impl.data.repository.v1.GovV1ConvictionVotingRepository
import io.novafoundation.nova.feature_governance_impl.data.repository.v1.GovV1DAppsRepository
import io.novafoundation.nova.feature_governance_impl.data.repository.v1.GovV1OnChainReferendaRepository
import io.novafoundation.nova.feature_governance_impl.data.repository.v1.GovV1PreImageRepository
import io.novafoundation.nova.feature_governance_impl.data.repository.v2.Gov2PreImageRepository
import io.novafoundation.nova.feature_governance_impl.data.source.StaticGovernanceSource
import io.novafoundation.nova.feature_wallet_api.data.repository.BalanceLocksRepository
import io.novafoundation.nova.runtime.di.REMOTE_STORAGE_SOURCE
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.repository.TotalIssuanceRepository
import io.novafoundation.nova.runtime.storage.source.StorageDataSource
import javax.inject.Named
import javax.inject.Qualifier

@Qualifier
annotation class GovernanceV1

@Module(includes = [PolkassemblyV1Module::class, SubSquareV1Module::class])
class GovernanceV1Module {

    @Provides
    @FeatureScope
    fun provideOnChainReferendaRepository(
        @Named(REMOTE_STORAGE_SOURCE) storageSource: StorageDataSource,
        chainRegistry: ChainRegistry,
        totalIssuanceRepository: TotalIssuanceRepository,
    ) = GovV1OnChainReferendaRepository(storageSource, chainRegistry, totalIssuanceRepository)

    @Provides
    @FeatureScope
    fun provideConvictionVotingRepository(
        @Named(REMOTE_STORAGE_SOURCE) storageSource: StorageDataSource,
        chainRegistry: ChainRegistry,
        balanceLocksRepository: BalanceLocksRepository
    ) = GovV1ConvictionVotingRepository(storageSource, chainRegistry, balanceLocksRepository)

    @Provides
    @GovernanceV1
    @FeatureScope
    fun provideOffChainInfoRepository(
        polkassembly: PolkassemblyV1ReferendaDataSource,
        subSquare: SubSquareV1ReferendaDataSource,
    ) = MultiSourceOffChainReferendaInfoRepository(
        subSquareReferendaDataSource = subSquare,
        polkassemblyReferendaDataSource = polkassembly
    )

    @Provides
    @FeatureScope
    fun providePreImageRepository(
        @Named(REMOTE_STORAGE_SOURCE) storageSource: StorageDataSource,
        v2Delegate: Gov2PreImageRepository,
    ) = GovV1PreImageRepository(storageSource, v2Delegate)

    @Provides
    @FeatureScope
    fun provideDappsRepository(governanceDAppsDao: GovernanceDAppsDao): GovV1DAppsRepository {
        return GovV1DAppsRepository(governanceDAppsDao)
    }

    @Provides
    @FeatureScope
    @GovernanceV1
    fun provideGovernanceSource(
        referendaRepository: GovV1OnChainReferendaRepository,
        convictionVotingRepository: GovV1ConvictionVotingRepository,
        @GovernanceV1 offChainInfoRepository: MultiSourceOffChainReferendaInfoRepository,
        preImageRepository: GovV1PreImageRepository,
        governanceV1DAppsRepository: GovV1DAppsRepository
    ): GovernanceSource = StaticGovernanceSource(
        referenda = referendaRepository,
        convictionVoting = convictionVotingRepository,
        offChainInfo = offChainInfoRepository,
        preImageRepository = preImageRepository,
        dappsRepository = governanceV1DAppsRepository,
        delegationsRepository = UnsupportedDelegationsRepository()
    )
}
