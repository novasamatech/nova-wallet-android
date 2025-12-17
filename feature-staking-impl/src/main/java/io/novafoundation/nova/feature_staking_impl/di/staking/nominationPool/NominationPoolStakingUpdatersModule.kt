package io.novafoundation.nova.feature_staking_impl.di.staking.nominationPool

import dagger.Module
import dagger.Provides
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.core.storage.StorageCache
import io.novafoundation.nova.feature_account_api.domain.updaters.AccountUpdateScope
import io.novafoundation.nova.feature_staking_impl.data.StakingSharedState
import io.novafoundation.nova.feature_staking_impl.data.network.blockhain.updaters.ActiveEraUpdater
import io.novafoundation.nova.feature_staking_impl.data.network.blockhain.updaters.CurrentEraUpdater
import io.novafoundation.nova.feature_staking_impl.data.network.blockhain.updaters.ParachainsUpdater
import io.novafoundation.nova.feature_staking_impl.data.network.blockhain.updaters.StakingUpdaters
import io.novafoundation.nova.feature_staking_impl.data.network.blockhain.updaters.ValidatorExposureUpdater
import io.novafoundation.nova.feature_staking_impl.data.network.blockhain.updaters.session.BondedErasUpdaterUpdater
import io.novafoundation.nova.feature_staking_impl.data.network.blockhain.updaters.session.CurrentEpochIndexUpdater
import io.novafoundation.nova.feature_staking_impl.data.network.blockhain.updaters.session.CurrentSessionIndexUpdater
import io.novafoundation.nova.feature_staking_impl.data.network.blockhain.updaters.session.CurrentSlotUpdater
import io.novafoundation.nova.feature_staking_impl.data.network.blockhain.updaters.session.EraStartSessionIndexUpdater
import io.novafoundation.nova.feature_staking_impl.data.network.blockhain.updaters.session.GenesisSlotUpdater
import io.novafoundation.nova.feature_staking_impl.data.nominationPools.network.blockhain.updater.CounterForPoolMembersUpdater
import io.novafoundation.nova.feature_staking_impl.data.nominationPools.network.blockhain.updater.DelegatedStakeUpdater
import io.novafoundation.nova.feature_staking_impl.data.nominationPools.network.blockhain.updater.LastPoolIdUpdater
import io.novafoundation.nova.feature_staking_impl.data.nominationPools.network.blockhain.updater.MaxPoolMembersPerPoolUpdater
import io.novafoundation.nova.feature_staking_impl.data.nominationPools.network.blockhain.updater.MaxPoolMembersUpdater
import io.novafoundation.nova.feature_staking_impl.data.nominationPools.network.blockhain.updater.MinJoinBondUpdater
import io.novafoundation.nova.feature_staking_impl.data.nominationPools.network.blockhain.updater.PoolMetadataUpdater
import io.novafoundation.nova.feature_staking_impl.data.nominationPools.network.blockhain.updater.SubPoolsUpdater
import io.novafoundation.nova.feature_staking_impl.data.nominationPools.network.blockhain.updater.scope.PoolScope
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.common.NominationPoolSharedComputation
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry

@Module
class NominationPoolStakingUpdatersModule {

    @Provides
    @FeatureScope
    fun providePoolScope(
        nominationPoolSharedComputation: NominationPoolSharedComputation,
        stakingSharedState: StakingSharedState
    ) = PoolScope(nominationPoolSharedComputation, stakingSharedState)

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
    fun provideDelegatedStakeUpdater(
        storageCache: StorageCache,
        stakingSharedState: StakingSharedState,
        chainRegistry: ChainRegistry,
        scope: AccountUpdateScope
    ) = DelegatedStakeUpdater(
        storageCache = storageCache,
        stakingSharedState = stakingSharedState,
        chainRegistry = chainRegistry,
        scope = scope
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
    fun provideMaxPoolMembersUpdater(
        storageCache: StorageCache,
        stakingSharedState: StakingSharedState,
        chainRegistry: ChainRegistry,
    ) = MaxPoolMembersUpdater(
        storageCache = storageCache,
        stakingSharedState = stakingSharedState,
        chainRegistry = chainRegistry
    )

    @Provides
    @FeatureScope
    fun provideMaxPoolMembersPerPoolUpdater(
        storageCache: StorageCache,
        stakingSharedState: StakingSharedState,
        chainRegistry: ChainRegistry,
    ) = MaxPoolMembersPerPoolUpdater(
        storageCache = storageCache,
        stakingSharedState = stakingSharedState,
        chainRegistry = chainRegistry
    )

    @Provides
    @FeatureScope
    fun provideCounterForPoolMembersUpdater(
        storageCache: StorageCache,
        stakingSharedState: StakingSharedState,
        chainRegistry: ChainRegistry,
    ) = CounterForPoolMembersUpdater(
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
    @FeatureScope
    fun providePoolMetadataUpdater(
        poolScope: PoolScope,
        storageCache: StorageCache,
        stakingSharedState: StakingSharedState,
        chainRegistry: ChainRegistry,
    ) = PoolMetadataUpdater(
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
        poolMetadataUpdater: PoolMetadataUpdater,
        exposureUpdater: ValidatorExposureUpdater,
        subPoolsUpdater: SubPoolsUpdater,
        maxPoolMembersUpdater: MaxPoolMembersUpdater,
        maxPoolMembersPerPoolUpdater: MaxPoolMembersPerPoolUpdater,
        counterForPoolMembersUpdater: CounterForPoolMembersUpdater,
        activeEraUpdater: ActiveEraUpdater,
        currentEraUpdater: CurrentEraUpdater,
        currentEpochIndexUpdater: CurrentEpochIndexUpdater,
        currentSlotUpdater: CurrentSlotUpdater,
        genesisSlotUpdater: GenesisSlotUpdater,
        currentSessionIndexUpdater: CurrentSessionIndexUpdater,
        eraStartSessionIndexUpdater: EraStartSessionIndexUpdater,
        bondedErasUpdaterUpdater: BondedErasUpdaterUpdater,
        parachainsUpdater: ParachainsUpdater,
        delegatedStakeUpdater: DelegatedStakeUpdater,
    ) = StakingUpdaters.Group(
        lastPoolIdUpdater,
        minJoinBondUpdater,
        poolMetadataUpdater,
        exposureUpdater,
        activeEraUpdater,
        currentEraUpdater,
        subPoolsUpdater,
        maxPoolMembersUpdater,
        maxPoolMembersPerPoolUpdater,
        counterForPoolMembersUpdater,
        currentEpochIndexUpdater,
        currentSlotUpdater,
        genesisSlotUpdater,
        currentSessionIndexUpdater,
        eraStartSessionIndexUpdater,
        parachainsUpdater,
        delegatedStakeUpdater,
        bondedErasUpdaterUpdater
    )
}
