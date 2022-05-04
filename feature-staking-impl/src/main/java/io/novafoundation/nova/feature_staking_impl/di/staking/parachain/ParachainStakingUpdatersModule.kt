package io.novafoundation.nova.feature_staking_impl.di.staking.parachain

import dagger.Module
import dagger.Provides
import io.novafoundation.nova.common.data.network.rpc.BulkRetriever
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.core.storage.StorageCache
import io.novafoundation.nova.core.updater.Updater
import io.novafoundation.nova.feature_account_api.domain.updaters.AccountUpdateScope
import io.novafoundation.nova.feature_staking_impl.data.StakingSharedState
import io.novafoundation.nova.feature_staking_impl.data.parachainStaking.network.blockhain.updaters.CurrentRoundCollatorsUpdater
import io.novafoundation.nova.feature_staking_impl.data.parachainStaking.network.blockhain.updaters.CurrentRoundUpdater
import io.novafoundation.nova.feature_staking_impl.data.parachainStaking.network.blockhain.updaters.DelegatorStateUpdater
import io.novafoundation.nova.feature_staking_impl.data.parachainStaking.repository.CurrentRoundRepository
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry

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
    @Parachain
    @FeatureScope
    fun provideRelaychainStakingUpdaters(
        delegatorStateUpdater: DelegatorStateUpdater,
        currentRoundUpdater: CurrentRoundUpdater,
        currentRoundCollatorsUpdater: CurrentRoundCollatorsUpdater
    ): List<Updater> = listOf(
        delegatorStateUpdater,
        currentRoundUpdater,
        currentRoundCollatorsUpdater
    )
}
