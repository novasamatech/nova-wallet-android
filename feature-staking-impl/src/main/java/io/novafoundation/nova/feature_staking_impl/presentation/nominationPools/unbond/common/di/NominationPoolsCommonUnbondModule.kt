package io.novafoundation.nova.feature_staking_impl.presentation.nominationPools.unbond.common.di

import dagger.Module
import dagger.Provides
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.common.validation.ValidationSystem
import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicService
import io.novafoundation.nova.feature_staking_impl.data.StakingSharedState
import io.novafoundation.nova.feature_staking_impl.data.nominationPools.pool.PoolAccountDerivation
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.bondMore.validations.NominationPoolsBondMoreValidationSystem
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.bondMore.validations.nominationPoolsBondMore
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.common.NominationPoolSharedComputation
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.common.hints.NominationPoolHintsUseCase
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.common.validations.PoolStateValidationFactory
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.unbond.NominationPoolsUnbondInteractor
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.unbond.RealNominationPoolsUnbondInteractor
import io.novafoundation.nova.feature_staking_impl.presentation.common.hints.StakingHintsUseCase
import io.novafoundation.nova.feature_staking_impl.presentation.nominationPools.unbond.hints.NominationPoolsUnbondHintsFactory

@Module
class NominationPoolsCommonUnbondModule {

    @Provides
    @ScreenScope
    fun provideInteractor(
        extrinsicService: ExtrinsicService,
        stakingSharedState: StakingSharedState,
        nominationPoolSharedComputation: NominationPoolSharedComputation,
        poolAccountDerivation: PoolAccountDerivation
    ): NominationPoolsUnbondInteractor {
        return RealNominationPoolsUnbondInteractor(extrinsicService, stakingSharedState, nominationPoolSharedComputation, poolAccountDerivation)
    }

    @Provides
    @ScreenScope
    fun provideValidationSystem(
        poolStateValidationFactory: PoolStateValidationFactory
    ): NominationPoolsBondMoreValidationSystem = ValidationSystem.nominationPoolsBondMore(poolStateValidationFactory)

    @Provides
    @ScreenScope
    fun provideHintsMixinFactory(
        nominationPoolHintsUseCase: NominationPoolHintsUseCase,
        stakingHintsUseCase: StakingHintsUseCase,
    ) = NominationPoolsUnbondHintsFactory(nominationPoolHintsUseCase, stakingHintsUseCase)
}
