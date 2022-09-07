package io.novafoundation.nova.feature_staking_impl.di.staking.parachain.turing

import dagger.Module
import dagger.Provides
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.feature_staking_impl.data.parachainStaking.repository.turing.RealTuringAutomationTasksRepository
import io.novafoundation.nova.feature_staking_impl.data.parachainStaking.repository.turing.RealTuringStakingRewardsRepository
import io.novafoundation.nova.feature_staking_impl.data.parachainStaking.repository.turing.TuringAutomationTasksRepository
import io.novafoundation.nova.feature_staking_impl.data.parachainStaking.repository.turing.TuringStakingRewardsRepository
import io.novafoundation.nova.runtime.di.LOCAL_STORAGE_SOURCE
import io.novafoundation.nova.runtime.storage.source.StorageDataSource
import javax.inject.Named

@Module
class TuringStakingModule {

    @Provides
    @FeatureScope
    fun provideTuringRewardsRepository(
        @Named(LOCAL_STORAGE_SOURCE) storageDataSource: StorageDataSource
    ): TuringStakingRewardsRepository = RealTuringStakingRewardsRepository(storageDataSource)

    @Provides
    @FeatureScope
    fun provideTuringAutomationRepository(
        @Named(LOCAL_STORAGE_SOURCE) storageDataSource: StorageDataSource
    ): TuringAutomationTasksRepository = RealTuringAutomationTasksRepository(storageDataSource)
}
