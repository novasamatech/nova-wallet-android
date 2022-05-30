package io.novafoundation.nova.feature_staking_impl.di.staking.parachain.unbond

import dagger.Module
import dagger.Provides
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicService
import io.novafoundation.nova.feature_staking_impl.data.StakingSharedState
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.common.DelegatorStateUseCase
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.unbond.ParachainStakingUnbondInteractor
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.unbond.RealParachainStakingUnbondInteractor
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.common.ParachainStakingHintsUseCase
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.unbond.hints.ParachainStakingUnbondHintsMixinFactory

@Module
class ParachainStakingUnbondModule {

    @Provides
    @FeatureScope
    fun provideInteractor(
        extrinsicService: ExtrinsicService,
        delegatorStateUseCase: DelegatorStateUseCase,
        stakingSharedState: StakingSharedState,
    ): ParachainStakingUnbondInteractor = RealParachainStakingUnbondInteractor(
        extrinsicService = extrinsicService,
        delegatorStateUseCase = delegatorStateUseCase,
        selectedAssetSharedState = stakingSharedState
    )

    @Provides
    @FeatureScope
    fun provideHintsFactory(
        stakingHintsUseCase: ParachainStakingHintsUseCase
    ) = ParachainStakingUnbondHintsMixinFactory(stakingHintsUseCase)
}
