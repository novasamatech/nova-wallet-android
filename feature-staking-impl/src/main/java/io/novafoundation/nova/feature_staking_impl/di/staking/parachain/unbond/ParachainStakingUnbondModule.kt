package io.novafoundation.nova.feature_staking_impl.di.staking.parachain.unbond

import dagger.Module
import dagger.Provides
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.common.validation.ValidationSystem
import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicService
import io.novafoundation.nova.feature_staking_impl.data.StakingSharedState
import io.novafoundation.nova.feature_staking_impl.data.parachainStaking.repository.CandidatesRepository
import io.novafoundation.nova.feature_staking_impl.data.parachainStaking.repository.DelegatorStateRepository
import io.novafoundation.nova.feature_staking_impl.data.parachainStaking.repository.ParachainStakingConstantsRepository
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.common.CollatorsUseCase
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.common.DelegatorStateUseCase
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.unbond.ParachainStakingUnbondInteractor
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.unbond.RealParachainStakingUnbondInteractor
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.unbond.validations.flow.NoExistingDelegationRequestsToCollatorValidationFactory
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.unbond.validations.flow.ParachainStakingUnbondValidationSystem
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.unbond.validations.flow.RemainingUnbondValidationFactory
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.unbond.validations.flow.parachainStakingUnbond
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.unbond.validations.preliminary.AnyAvailableCollatorForUnbondValidationFactory
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.unbond.validations.preliminary.ParachainStakingUnbondPreliminaryValidationSystem
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.unbond.validations.preliminary.parachainStakingPreliminaryUnbond
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.common.ParachainStakingHintsUseCase
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.unbond.hints.ParachainStakingUnbondHintsMixinFactory

@Module
class ParachainStakingUnbondModule {

    @Provides
    @FeatureScope
    fun provideRemainingUnbondValidationFactory(
        candidatesRepository: CandidatesRepository,
        stakingConstantsRepository: ParachainStakingConstantsRepository,
        delegatorStateUseCase: DelegatorStateUseCase,
    ) = RemainingUnbondValidationFactory(stakingConstantsRepository, candidatesRepository, delegatorStateUseCase)

    @Provides
    @FeatureScope
    fun provideNoExistingDelegationRequestsToCollatorValidationFactory(
        interactor: ParachainStakingUnbondInteractor,
        delegatorStateUseCase: DelegatorStateUseCase,
    ) = NoExistingDelegationRequestsToCollatorValidationFactory(interactor, delegatorStateUseCase)

    @Provides
    @FeatureScope
    fun provideAnyAvailableCollatorsToUnbondValidationFactory(
        delegatorStateRepository: DelegatorStateRepository,
        delegatorStateUseCase: DelegatorStateUseCase,
    ) = AnyAvailableCollatorForUnbondValidationFactory(delegatorStateRepository, delegatorStateUseCase)

    @Provides
    @FeatureScope
    fun providePreliminaryValidationSystem(
        anyAvailableCollatorForUnbondValidationFactory: AnyAvailableCollatorForUnbondValidationFactory
    ): ParachainStakingUnbondPreliminaryValidationSystem = ValidationSystem.parachainStakingPreliminaryUnbond(
        anyAvailableCollatorForUnbondValidationFactory = anyAvailableCollatorForUnbondValidationFactory
    )

    @Provides
    @FeatureScope
    fun provideValidationSystem(
        remainingUnbondValidationFactory: RemainingUnbondValidationFactory,
        noExistingDelegationRequestsToCollatorValidationFactory: NoExistingDelegationRequestsToCollatorValidationFactory,
    ): ParachainStakingUnbondValidationSystem = ValidationSystem.parachainStakingUnbond(
        remainingUnbondValidationFactory = remainingUnbondValidationFactory,
        noExistingDelegationRequestsToCollatorValidationFactory = noExistingDelegationRequestsToCollatorValidationFactory
    )

    @Provides
    @FeatureScope
    fun provideInteractor(
        extrinsicService: ExtrinsicService,
        delegatorStateUseCase: DelegatorStateUseCase,
        stakingSharedState: StakingSharedState,
        delegatorStateRepository: DelegatorStateRepository,
        collatorsUseCase: CollatorsUseCase,
    ): ParachainStakingUnbondInteractor = RealParachainStakingUnbondInteractor(
        extrinsicService = extrinsicService,
        delegatorStateUseCase = delegatorStateUseCase,
        selectedAssetSharedState = stakingSharedState,
        delegatorStateRepository = delegatorStateRepository,
        collatorsUseCase = collatorsUseCase
    )

    @Provides
    @FeatureScope
    fun provideHintsFactory(
        stakingHintsUseCase: ParachainStakingHintsUseCase
    ) = ParachainStakingUnbondHintsMixinFactory(stakingHintsUseCase)
}
