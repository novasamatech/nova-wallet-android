package io.novafoundation.nova.feature_staking_impl.di.staking.parachain.turing

import dagger.Module
import dagger.Provides
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.core.storage.StorageCache
import io.novafoundation.nova.feature_account_api.domain.updaters.AccountUpdateScope
import io.novafoundation.nova.feature_staking_impl.data.StakingSharedState
import io.novafoundation.nova.feature_staking_impl.data.parachainStaking.network.blockhain.updaters.turing.TuringAdditionalIssuanceUpdater
import io.novafoundation.nova.feature_staking_impl.data.parachainStaking.network.blockhain.updaters.turing.TuringAutomationTasksUpdater
import io.novafoundation.nova.feature_staking_impl.di.staking.StakingUpdaters
import io.novafoundation.nova.runtime.di.REMOTE_STORAGE_SOURCE
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.storage.source.StorageDataSource
import javax.inject.Named

@Module
class TuringStakingUpdatersModule {

    @Provides
    @FeatureScope
    fun provideAdditionalIssuanceUpdater(
        storageCache: StorageCache,
        stakingSharedState: StakingSharedState,
        chainRegistry: ChainRegistry,
    ) = TuringAdditionalIssuanceUpdater(
        storageCache = storageCache,
        stakingSharedState = stakingSharedState,
        chainRegistry = chainRegistry
    )

    @Provides
    @FeatureScope
    fun provideAutomationTaskUpdater(
        storageCache: StorageCache,
        stakingSharedState: StakingSharedState,
        chainRegistry: ChainRegistry,
        @Named(REMOTE_STORAGE_SOURCE) remoteStorageDataSource: StorageDataSource,
        accountUpdateScope: AccountUpdateScope,
    ) = TuringAutomationTasksUpdater(
        storageCache = storageCache,
        stakingSharedState = stakingSharedState,
        remoteStorageSource = remoteStorageDataSource,
        chainRegistry = chainRegistry,
        scope = accountUpdateScope
    )

    @Provides
    @FeatureScope
    @Turing
    fun provideTuringExtraUpdaters(
        turingAdditionalIssuanceUpdater: TuringAdditionalIssuanceUpdater,
        turingAutomationTasksUpdater: TuringAutomationTasksUpdater,
    ): StakingUpdaters = StakingUpdaters(turingAdditionalIssuanceUpdater, turingAutomationTasksUpdater)
}
