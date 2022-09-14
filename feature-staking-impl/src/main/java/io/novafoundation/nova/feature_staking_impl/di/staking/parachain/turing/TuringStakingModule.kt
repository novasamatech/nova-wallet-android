package io.novafoundation.nova.feature_staking_impl.di.staking.parachain.turing

import dagger.Module
import dagger.Provides
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.common.validation.ValidationSystem
import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicService
import io.novafoundation.nova.feature_staking_api.data.parachainStaking.turing.repository.TuringAutomationTasksRepository
import io.novafoundation.nova.feature_staking_impl.data.StakingSharedState
import io.novafoundation.nova.feature_staking_impl.data.parachainStaking.turing.network.rpc.RealTuringAutomationRpcApi
import io.novafoundation.nova.feature_staking_impl.data.parachainStaking.turing.network.rpc.TuringAutomationRpcApi
import io.novafoundation.nova.feature_staking_impl.data.parachainStaking.turing.repository.RealTuringAutomationTasksRepository
import io.novafoundation.nova.feature_staking_impl.data.parachainStaking.turing.repository.RealTuringStakingRewardsRepository
import io.novafoundation.nova.feature_staking_impl.data.parachainStaking.turing.repository.TuringStakingRewardsRepository
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.yieldBoost.RealYieldBoostInteractor
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.yieldBoost.YieldBoostInteractor
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.yieldBoost.validations.YieldBoostValidationSystem
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.yieldBoost.validations.yieldBoost
import io.novafoundation.nova.runtime.di.LOCAL_STORAGE_SOURCE
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.repository.TimestampRepository
import io.novafoundation.nova.runtime.storage.source.StorageDataSource
import javax.inject.Named

@Module
class TuringStakingModule {

    @Provides
    @FeatureScope
    fun provideTuringAutomationRpcApi(chainRegistry: ChainRegistry): TuringAutomationRpcApi = RealTuringAutomationRpcApi(chainRegistry)

    @Provides
    @FeatureScope
    fun provideTuringRewardsRepository(
        @Named(LOCAL_STORAGE_SOURCE) storageDataSource: StorageDataSource
    ): TuringStakingRewardsRepository = RealTuringStakingRewardsRepository(storageDataSource)

    @Provides
    @FeatureScope
    fun provideTuringAutomationRepository(
        @Named(LOCAL_STORAGE_SOURCE) storageDataSource: StorageDataSource,
        turingAutomationRpcApi: TuringAutomationRpcApi,
    ): TuringAutomationTasksRepository = RealTuringAutomationTasksRepository(storageDataSource, turingAutomationRpcApi)

    @Provides
    @FeatureScope
    fun provideYieldBoostInteractor(
        automationTasksRepository: TuringAutomationTasksRepository,
        extrinsicService: ExtrinsicService,
        stakingSharedState: StakingSharedState,
        timestampRepository: TimestampRepository
    ): YieldBoostInteractor = RealYieldBoostInteractor(automationTasksRepository, extrinsicService, stakingSharedState, timestampRepository)

    @Provides
    @FeatureScope
    fun provideYieldBoostValidationSystem(automationTasksRepository: TuringAutomationTasksRepository): YieldBoostValidationSystem {
        return ValidationSystem.yieldBoost(automationTasksRepository)
    }
}
