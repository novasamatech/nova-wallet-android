package io.novafoundation.nova.feature_staking_impl.di.staking.unbond

import dagger.Module
import dagger.Provides
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicService
import io.novafoundation.nova.feature_staking_api.domain.api.StakingRepository
import io.novafoundation.nova.feature_staking_impl.data.StakingSharedState
import io.novafoundation.nova.feature_staking_impl.domain.common.StakingSharedComputation
import io.novafoundation.nova.feature_staking_impl.domain.staking.unbond.UnbondInteractor
import io.novafoundation.nova.feature_staking_impl.presentation.common.hints.StakingHintsUseCase
import io.novafoundation.nova.feature_staking_impl.presentation.staking.unbond.hints.UnbondHintsMixinFactory

@Module
class StakingUnbondModule {

    @Provides
    @FeatureScope
    fun provideUnbondInteractor(
        extrinsicService: ExtrinsicService,
        stakingRepository: StakingRepository,
        stakingSharedState: StakingSharedState,
        stakingSharedComputation: StakingSharedComputation
    ) = UnbondInteractor(extrinsicService, stakingRepository, stakingSharedState, stakingSharedComputation)

    @Provides
    @FeatureScope
    fun provideHintsMixinFactory(
        stakingHintsUseCase: StakingHintsUseCase
    ) = UnbondHintsMixinFactory(stakingHintsUseCase)
}
