package io.novafoundation.nova.feature_staking_impl.di.staking.parachain

import dagger.Module
import dagger.Provides
import io.novafoundation.nova.common.data.network.rpc.BulkRetriever
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.core.storage.StorageCache
import io.novafoundation.nova.core.updater.Updater
import io.novafoundation.nova.feature_account_api.domain.updaters.AccountUpdateScope
import io.novafoundation.nova.feature_staking_impl.data.StakingSharedState
import io.novafoundation.nova.feature_staking_impl.data.common.network.blockhain.updaters.TotalIssuanceUpdater
import io.novafoundation.nova.feature_staking_impl.data.parachainStaking.network.blockhain.updaters.CollatorCommissionUpdater
import io.novafoundation.nova.feature_staking_impl.data.parachainStaking.network.blockhain.updaters.CurrentRoundCollatorsUpdater
import io.novafoundation.nova.feature_staking_impl.data.parachainStaking.network.blockhain.updaters.CurrentRoundUpdater
import io.novafoundation.nova.feature_staking_impl.data.parachainStaking.network.blockhain.updaters.DelegatorStateUpdater
import io.novafoundation.nova.feature_staking_impl.data.parachainStaking.network.blockhain.updaters.InflationConfigUpdater
import io.novafoundation.nova.feature_staking_impl.data.parachainStaking.network.blockhain.updaters.ParachainBondInfoUpdater
import io.novafoundation.nova.feature_staking_impl.data.parachainStaking.network.blockhain.updaters.TotalDelegatedUpdater
import io.novafoundation.nova.feature_staking_impl.data.parachainStaking.repository.CurrentRoundRepository
import io.novafoundation.nova.feature_staking_impl.di.staking.common.CommonStakingUpdatersModule
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry

@Module(includes = [CommonStakingUpdatersModule::class])
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
        bulkRetriever: BulkRetriever,
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
    ) = ParachainBondInfoUpdater(
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
    @Parachain
    @FeatureScope
    fun provideRelaychainStakingUpdaters(
        delegatorStateUpdater: DelegatorStateUpdater,
        currentRoundUpdater: CurrentRoundUpdater,
        currentRoundCollatorsUpdater: CurrentRoundCollatorsUpdater,
        totalDelegatedUpdater: TotalDelegatedUpdater,
        inflationConfigUpdater: InflationConfigUpdater,
        parachainBondInfoUpdater: ParachainBondInfoUpdater,
        totalIssuanceUpdater: TotalIssuanceUpdater,
        collatorCommissionUpdater: CollatorCommissionUpdater,
    ): List<Updater> = listOf(
        delegatorStateUpdater,
        currentRoundUpdater,
        currentRoundCollatorsUpdater,
        totalDelegatedUpdater,
        inflationConfigUpdater,
        parachainBondInfoUpdater,
        totalIssuanceUpdater,
        collatorCommissionUpdater
    )
}
