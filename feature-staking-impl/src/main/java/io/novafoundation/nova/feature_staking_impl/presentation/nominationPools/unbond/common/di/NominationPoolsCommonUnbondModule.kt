package io.novafoundation.nova.feature_staking_impl.presentation.nominationPools.unbond.common.di

import dagger.Module
import dagger.Provides
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.common.validation.ValidationSystem
import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicService
import io.novafoundation.nova.feature_staking_api.domain.api.StakingRepository
import io.novafoundation.nova.feature_staking_impl.data.StakingSharedState
import io.novafoundation.nova.feature_staking_api.data.nominationPools.pool.PoolAccountDerivation
import io.novafoundation.nova.feature_staking_impl.data.nominationPools.repository.NominationPoolGlobalsRepository
import io.novafoundation.nova.feature_staking_impl.data.repository.StakingConstantsRepository
import io.novafoundation.nova.feature_staking_impl.domain.common.StakingSharedComputation
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.common.NominationPoolMemberUseCase
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.common.NominationPoolSharedComputation
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.common.hints.NominationPoolHintsUseCase
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.unbond.NominationPoolsUnbondInteractor
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.unbond.RealNominationPoolsUnbondInteractor
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.unbond.validations.NominationPoolsUnbondValidationFactory
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.unbond.validations.NominationPoolsUnbondValidationSystem
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.unbond.validations.nominationPoolsUnbond
import io.novafoundation.nova.feature_staking_impl.presentation.common.hints.StakingHintsUseCase
import io.novafoundation.nova.feature_staking_impl.presentation.nominationPools.unbond.hints.NominationPoolsUnbondHintsFactory
import io.novafoundation.nova.feature_wallet_api.domain.validation.EnoughTotalToStayAboveEDValidationFactory

@Module
class NominationPoolsCommonUnbondModule {

    @Provides
    @ScreenScope
    fun provideUnbondValidationFactory(
        stakingConstantsRepository: StakingConstantsRepository,
        stakingRepository: StakingRepository,
        stakingSharedComputation: StakingSharedComputation,
        nominationPoolSharedComputation: NominationPoolSharedComputation,
        stakingSharedState: StakingSharedState,
        nominationPoolGlobalsRepository: NominationPoolGlobalsRepository,
    ) = NominationPoolsUnbondValidationFactory(
        stakingConstantsRepository = stakingConstantsRepository,
        stakingRepository = stakingRepository,
        stakingSharedComputation = stakingSharedComputation,
        nominationPoolSharedComputation = nominationPoolSharedComputation,
        stakingSharedState = stakingSharedState,
        nominationPoolGlobalsRepository = nominationPoolGlobalsRepository
    )

    @Provides
    @ScreenScope
    fun provideInteractor(
        extrinsicService: ExtrinsicService,
        stakingSharedState: StakingSharedState,
        nominationPoolSharedComputation: NominationPoolSharedComputation,
        poolAccountDerivation: PoolAccountDerivation,
        poolMemberUseCase: NominationPoolMemberUseCase,
    ): NominationPoolsUnbondInteractor {
        return RealNominationPoolsUnbondInteractor(
            extrinsicService = extrinsicService,
            stakingSharedState = stakingSharedState,
            nominationPoolSharedComputation = nominationPoolSharedComputation,
            poolAccountDerivation = poolAccountDerivation,
            poolMemberUseCase = poolMemberUseCase
        )
    }

    @Provides
    @ScreenScope
    fun provideValidationSystem(
        validationFactory: NominationPoolsUnbondValidationFactory,
        enoughTotalToStayAboveEDValidationFactory: EnoughTotalToStayAboveEDValidationFactory
    ): NominationPoolsUnbondValidationSystem = ValidationSystem.nominationPoolsUnbond(validationFactory, enoughTotalToStayAboveEDValidationFactory)

    @Provides
    @ScreenScope
    fun provideHintsMixinFactory(
        nominationPoolHintsUseCase: NominationPoolHintsUseCase,
        stakingHintsUseCase: StakingHintsUseCase,
    ) = NominationPoolsUnbondHintsFactory(nominationPoolHintsUseCase, stakingHintsUseCase)
}
