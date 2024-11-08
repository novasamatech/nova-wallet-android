package io.novafoundation.nova.feature_staking_impl.di.staking.parachain

import dagger.Module
import dagger.Provides
import io.novafoundation.nova.common.data.network.rpc.BulkRetriever
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.core.storage.StorageCache
import io.novafoundation.nova.feature_account_api.domain.updaters.AccountUpdateScope
import io.novafoundation.nova.feature_staking_impl.data.StakingSharedState
import io.novafoundation.nova.feature_staking_impl.data.network.blockhain.updaters.StakingUpdaters
import io.novafoundation.nova.feature_staking_impl.data.parachainStaking.network.blockhain.updaters.CollatorCommissionUpdater
import io.novafoundation.nova.feature_staking_impl.data.parachainStaking.network.blockhain.updaters.CurrentRoundCollatorsUpdater
import io.novafoundation.nova.feature_staking_impl.data.parachainStaking.network.blockhain.updaters.CurrentRoundUpdater
import io.novafoundation.nova.feature_staking_impl.data.parachainStaking.network.blockhain.updaters.DelegatorStateUpdater
import io.novafoundation.nova.feature_staking_impl.data.parachainStaking.network.blockhain.updaters.InflationConfigUpdater
import io.novafoundation.nova.feature_staking_impl.data.parachainStaking.network.blockhain.updaters.InflationDistributionConfigUpdater
import io.novafoundation.nova.feature_staking_impl.data.parachainStaking.network.blockhain.updaters.ScheduledDelegationRequestsUpdater
import io.novafoundation.nova.feature_staking_impl.data.parachainStaking.network.blockhain.updaters.TotalDelegatedUpdater
import io.novafoundation.nova.feature_staking_impl.data.parachainStaking.repository.CurrentRoundRepository
import io.novafoundation.nova.feature_staking_impl.di.staking.DefaultBulkRetriever
import io.novafoundation.nova.runtime.di.REMOTE_STORAGE_SOURCE
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.storage.source.StorageDataSource
import javax.inject.Named

@Module
class ParachainStakingUpdatersModule {

    @Provides
    @FeatureScope
    fun provideDelegatorStateUpdater(
        scope: AccountUpdateScope,
        storageCache: StorageCache,
        stakingSharedState: StakingSharedState,
        chainRegistry: ChainRegistry,
    ) = DelegatorStateUpdater(
        scope = scope,
        storageCache = storageCache,
        stakingSharedState = stakingSharedState,
        chainRegistry = chainRegistry
    )

    @Provides
    @FeatureScope
    fun provideCurrentRoundUpdater(
        storageCache: StorageCache,
        stakingSharedState: StakingSharedState,
        chainRegistry: ChainRegistry,
    ) = CurrentRoundUpdater(
        storageCache = storageCache,
        stakingSharedState = stakingSharedState,
        chainRegistry = chainRegistry
    )

    @Provides
    @FeatureScope
    fun provideCurrentRoundCollatorsUpdater(
        storageCache: StorageCache,
        stakingSharedState: StakingSharedState,
        chainRegistry: ChainRegistry,
        @DefaultBulkRetriever bulkRetriever: BulkRetriever,
        currentRoundRepository: CurrentRoundRepository,
    ) = CurrentRoundCollatorsUpdater(
        storageCache = storageCache,
        stakingSharedState = stakingSharedState,
        chainRegistry = chainRegistry,
        bulkRetriever = bulkRetriever,
        currentRoundRepository = currentRoundRepository
    )

    @Provides
    @FeatureScope
    fun provideTotalDelegatedUpdater(
        storageCache: StorageCache,
        stakingSharedState: StakingSharedState,
        chainRegistry: ChainRegistry,
    ) = TotalDelegatedUpdater(
        storageCache = storageCache,
        stakingSharedState = stakingSharedState,
        chainRegistry = chainRegistry
    )

    @Provides
    @FeatureScope
    fun provideInflationConfigUpdater(
        storageCache: StorageCache,
        stakingSharedState: StakingSharedState,
        chainRegistry: ChainRegistry,
    ) = InflationConfigUpdater(
        storageCache = storageCache,
        stakingSharedState = stakingSharedState,
        chainRegistry = chainRegistry
    )

    @Provides
    @FeatureScope
    fun provideParachainBondInfoUpdater(
        storageCache: StorageCache,
        stakingSharedState: StakingSharedState,
        chainRegistry: ChainRegistry,
    ) = InflationDistributionConfigUpdater(
        storageCache = storageCache,
        stakingSharedState = stakingSharedState,
        chainRegistry = chainRegistry
    )

    @Provides
    @FeatureScope
    fun provideCollatorCommissionUpdater(
        storageCache: StorageCache,
        stakingSharedState: StakingSharedState,
        chainRegistry: ChainRegistry,
    ) = CollatorCommissionUpdater(
        storageCache = storageCache,
        stakingSharedState = stakingSharedState,
        chainRegistry = chainRegistry
    )

    @Provides
    @FeatureScope
    fun provideScheduledDelegationRequestsUpdater(
        scope: AccountUpdateScope,
        storageCache: StorageCache,
        stakingSharedState: StakingSharedState,
        chainRegistry: ChainRegistry,
        @Named(REMOTE_STORAGE_SOURCE) storageDataSource: StorageDataSource,
    ) = ScheduledDelegationRequestsUpdater(
        scope = scope,
        storageCache = storageCache,
        stakingSharedState = stakingSharedState,
        chainRegistry = chainRegistry,
        remoteStorageDataSource = storageDataSource
    )

    @Provides
    @Parachain
    @FeatureScope
    fun provideRelaychainStakingUpdaters(
        delegatorStateUpdater: DelegatorStateUpdater,
        currentRoundUpdater: CurrentRoundUpdater,
        currentRoundCollatorsUpdater: CurrentRoundCollatorsUpdater,
        totalDelegatedUpdater: TotalDelegatedUpdater,
        inflationConfigUpdater: InflationConfigUpdater,
        inflationDistributionConfigUpdater: InflationDistributionConfigUpdater,
        collatorCommissionUpdater: CollatorCommissionUpdater,
        scheduledDelegationRequestsUpdater: ScheduledDelegationRequestsUpdater,
    ) = StakingUpdaters.Group(
        delegatorStateUpdater,
        currentRoundUpdater,
        currentRoundCollatorsUpdater,
        totalDelegatedUpdater,
        inflationConfigUpdater,
        inflationDistributionConfigUpdater,
        collatorCommissionUpdater,
        scheduledDelegationRequestsUpdater
    )
}
