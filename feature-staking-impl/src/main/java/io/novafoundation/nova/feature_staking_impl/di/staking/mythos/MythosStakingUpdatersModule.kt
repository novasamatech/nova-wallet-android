package io.novafoundation.nova.feature_staking_impl.di.staking.mythos

import dagger.Module
import dagger.Provides
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.core.storage.StorageCache
import io.novafoundation.nova.feature_staking_impl.data.StakingSharedState
import io.novafoundation.nova.feature_staking_impl.data.mythos.updaters.MythosMinStakeUpdater
import io.novafoundation.nova.feature_staking_impl.data.network.blockhain.updaters.StakingUpdaters
import io.novafoundation.nova.feature_staking_impl.data.network.blockhain.updaters.session.CurrentSlotUpdater
import io.novafoundation.nova.feature_staking_impl.data.network.blockhain.updaters.session.SessionValidatorsUpdater
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry

@Module
class MythosStakingUpdatersModule {

    @Provides
    @FeatureScope
    fun provideSessionValidatorsUpdater(
        sharedState: StakingSharedState,
        chainRegistry: ChainRegistry,
        storageCache: StorageCache,
    ) = SessionValidatorsUpdater(
        sharedState,
        chainRegistry,
        storageCache
    )

    @Provides
    @FeatureScope
    fun provideMinStakeUpdater(
        sharedState: StakingSharedState,
        chainRegistry: ChainRegistry,
        storageCache: StorageCache,
    ) = MythosMinStakeUpdater(
        sharedState,
        chainRegistry,
        storageCache
    )

    @Provides
    @FeatureScope
    @Mythos
    fun provideMythosStakingUpdaters(
        // UserStake in synced in-place in StakingDashboardMythosUpdater by dashboard
        sessionValidatorsUpdater: SessionValidatorsUpdater,
        minStakeUpdater: MythosMinStakeUpdater,
        // For syncing aura session info
        currentSlotUpdater: CurrentSlotUpdater
    ): StakingUpdaters.Group {
        return StakingUpdaters.Group(
            sessionValidatorsUpdater,
            minStakeUpdater,
            currentSlotUpdater
        )
    }
}
