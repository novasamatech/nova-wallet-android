package io.novafoundation.nova.feature_staking_impl.di.staking.nominationPool

import dagger.Module
import dagger.Provides
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.core.storage.StorageCache
import io.novafoundation.nova.feature_staking_impl.data.StakingSharedState
import io.novafoundation.nova.feature_staking_impl.data.network.blockhain.updaters.CurrentEraUpdater
import io.novafoundation.nova.feature_staking_impl.data.network.blockhain.updaters.ValidatorExposureUpdater
import io.novafoundation.nova.feature_staking_impl.data.network.blockhain.updaters.session.CurrentEpochIndexUpdater
import io.novafoundation.nova.feature_staking_impl.data.network.blockhain.updaters.session.CurrentSessionIndexUpdater
import io.novafoundation.nova.feature_staking_impl.data.network.blockhain.updaters.session.CurrentSlotUpdater
import io.novafoundation.nova.feature_staking_impl.data.network.blockhain.updaters.session.EraStartSessionIndexUpdater
import io.novafoundation.nova.feature_staking_impl.data.network.blockhain.updaters.session.GenesisSlotUpdater
import io.novafoundation.nova.feature_staking_impl.data.nominationPools.network.blockhain.updater.LastPoolIdUpdater
import io.novafoundation.nova.feature_staking_impl.data.nominationPools.network.blockhain.updater.MinJoinBondUpdater
import io.novafoundation.nova.feature_staking_impl.data.nominationPools.network.blockhain.updater.SubPoolsUpdater
import io.novafoundation.nova.feature_staking_impl.data.nominationPools.network.blockhain.updater.scope.PoolScope
import io.novafoundation.nova.feature_staking_impl.di.staking.StakingUpdaters
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.common.NominationPoolMemberUseCase
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry

@Module
class NominationPoolStakingUpdatersModule {

    @Provides
    @FeatureScope
    fun providePoolScope(
        poolMemberUseCase: NominationPoolMemberUseCase,
    ) = PoolScope(poolMemberUseCase)

    @Provides
    @FeatureScope
    fun provideLastPoolIdUpdater(
        storageCache: StorageCache,
        stakingSharedState: StakingSharedState,
        chainRegistry: ChainRegistry,
    ) = LastPoolIdUpdater(
        storageCache = storageCache,
        stakingSharedState = stakingSharedState,
        chainRegistry = chainRegistry
    )

    @Provides
    @FeatureScope
    fun provideMinJoinBondUpdater(
        storageCache: StorageCache,
        stakingSharedState: StakingSharedState,
        chainRegistry: ChainRegistry,
    ) = MinJoinBondUpdater(
        storageCache = storageCache,
        stakingSharedState = stakingSharedState,
        chainRegistry = chainRegistry
    )

    @Provides
    @FeatureScope
    fun provideSubPoolsUpdater(
        poolScope: PoolScope,
        storageCache: StorageCache,
        stakingSharedState: StakingSharedState,
        chainRegistry: ChainRegistry,
    ) = SubPoolsUpdater(
        poolScope = poolScope,
        storageCache = storageCache,
        stakingSharedState = stakingSharedState,
        chainRegistry = chainRegistry
    )

    @Provides
    @NominationPools
    @FeatureScope
    fun provideNominationPoolStakingUpdaters(
        lastPoolIdUpdater: LastPoolIdUpdater,
        minJoinBondUpdater: MinJoinBondUpdater,
        exposureUpdater: ValidatorExposureUpdater,
        subPoolsUpdater: SubPoolsUpdater,
        currentEraUpdater: CurrentEraUpdater,
        currentEpochIndexUpdater: CurrentEpochIndexUpdater,
        currentSlotUpdater: CurrentSlotUpdater,
        genesisSlotUpdater: GenesisSlotUpdater,
        currentSessionIndexUpdater: CurrentSessionIndexUpdater,
        eraStartSessionIndexUpdater: EraStartSessionIndexUpdater,
    ): StakingUpdaters = StakingUpdaters(
        lastPoolIdUpdater,
        minJoinBondUpdater,
        exposureUpdater,
        currentEraUpdater,
        subPoolsUpdater,
        currentEpochIndexUpdater,
        currentSlotUpdater,
        genesisSlotUpdater,
        currentSessionIndexUpdater,
        eraStartSessionIndexUpdater
    )
}
