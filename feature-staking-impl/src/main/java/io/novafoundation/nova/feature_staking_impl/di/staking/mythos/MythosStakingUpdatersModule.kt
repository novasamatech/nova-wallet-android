package io.novafoundation.nova.feature_staking_impl.di.staking.mythos

import dagger.Module
import dagger.Provides
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.core.storage.StorageCache
import io.novafoundation.nova.feature_account_api.domain.updaters.AccountUpdateScope
import io.novafoundation.nova.feature_staking_impl.data.StakingSharedState
import io.novafoundation.nova.feature_staking_impl.data.mythos.repository.MythosUserStakeRepository
import io.novafoundation.nova.feature_staking_impl.data.mythos.updaters.MythosCollatorRewardPercentageUpdater
import io.novafoundation.nova.feature_staking_impl.data.mythos.updaters.MythosCompoundPercentageUpdater
import io.novafoundation.nova.feature_staking_impl.data.mythos.updaters.MythosExtraRewardUpdater
import io.novafoundation.nova.feature_staking_impl.data.mythos.updaters.MythosMinStakeUpdater
import io.novafoundation.nova.feature_staking_impl.data.mythos.updaters.MythosReleaseQueuesUpdater
import io.novafoundation.nova.feature_staking_impl.data.mythos.updaters.MythosSelectedCandidatesUpdater
import io.novafoundation.nova.feature_staking_impl.data.network.blockhain.updaters.StakingUpdaters
import io.novafoundation.nova.feature_staking_impl.data.network.blockhain.updaters.session.CurrentSlotUpdater
import io.novafoundation.nova.feature_staking_impl.data.network.blockhain.updaters.session.InvulnerablesUpdater
import io.novafoundation.nova.feature_staking_impl.data.network.blockhain.updaters.session.SessionValidatorsUpdater
import io.novafoundation.nova.runtime.di.REMOTE_STORAGE_SOURCE
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.storage.source.StorageDataSource
import javax.inject.Named

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
    fun provideInvulnerablesUpdater(
        sharedState: StakingSharedState,
        chainRegistry: ChainRegistry,
        storageCache: StorageCache,
    ) = InvulnerablesUpdater(
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
    fun provideExtraRewardUpdater(
        sharedState: StakingSharedState,
        chainRegistry: ChainRegistry,
        storageCache: StorageCache,
    ) = MythosExtraRewardUpdater(
        sharedState,
        chainRegistry,
        storageCache
    )

    @Provides
    @FeatureScope
    fun provideCollatorRewardPercentageUpdater(
        sharedState: StakingSharedState,
        chainRegistry: ChainRegistry,
        storageCache: StorageCache,
    ) = MythosCollatorRewardPercentageUpdater(
        sharedState,
        chainRegistry,
        storageCache
    )

    @Provides
    @FeatureScope
    fun provideSelectedCandidatesUpdater(
        scope: AccountUpdateScope,
        stakingSharedState: StakingSharedState,
        storageCache: StorageCache,
        mythosUserStakeRepository: MythosUserStakeRepository,
        @Named(REMOTE_STORAGE_SOURCE)
        remoteStorageDataSource: StorageDataSource,
    ) = MythosSelectedCandidatesUpdater(
        scope = scope,
        stakingSharedState = stakingSharedState,
        storageCache = storageCache,
        mythosUserStakeRepository = mythosUserStakeRepository,
        remoteStorageDataSource = remoteStorageDataSource
    )

    @Provides
    @FeatureScope
    fun provideReleaseQueuesUpdater(
        sharedState: StakingSharedState,
        chainRegistry: ChainRegistry,
        storageCache: StorageCache,
        accountUpdateScope: AccountUpdateScope,
    ) = MythosReleaseQueuesUpdater(
        sharedState,
        chainRegistry,
        storageCache,
        accountUpdateScope
    )

    @Provides
    @FeatureScope
    fun provideCompoundPercentageUpdater(
        sharedState: StakingSharedState,
        chainRegistry: ChainRegistry,
        storageCache: StorageCache,
        accountUpdateScope: AccountUpdateScope,
    ) = MythosCompoundPercentageUpdater(
        sharedState,
        chainRegistry,
        storageCache,
        accountUpdateScope
    )

    @Provides
    @FeatureScope
    @Mythos
    fun provideMythosStakingUpdaters(
        // UserStake in synced in-place in StakingDashboardMythosUpdater by dashboard
        sessionValidatorsUpdater: SessionValidatorsUpdater,
        invulnerablesUpdater: InvulnerablesUpdater,
        minStakeUpdater: MythosMinStakeUpdater,
        // For syncing aura session info
        currentSlotUpdater: CurrentSlotUpdater,
        selectedCandidatesUpdater: MythosSelectedCandidatesUpdater,
        releaseQueuesUpdater: MythosReleaseQueuesUpdater,
        extraRewardUpdater: MythosExtraRewardUpdater,
        collatorRewardPercentageUpdater: MythosCollatorRewardPercentageUpdater,
        compoundPercentageUpdater: MythosCompoundPercentageUpdater
    ): StakingUpdaters.Group {
        return StakingUpdaters.Group(
            sessionValidatorsUpdater,
            invulnerablesUpdater,
            minStakeUpdater,
            currentSlotUpdater,
            selectedCandidatesUpdater,
            releaseQueuesUpdater,
            extraRewardUpdater,
            collatorRewardPercentageUpdater,
            compoundPercentageUpdater
        )
    }
}
