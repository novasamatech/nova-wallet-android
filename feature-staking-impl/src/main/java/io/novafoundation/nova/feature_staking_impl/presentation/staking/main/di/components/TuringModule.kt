package io.novafoundation.nova.feature_staking_impl.presentation.staking.main.di.components

import dagger.Module
import dagger.Provides
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.validation.ValidationExecutor
import io.novafoundation.nova.feature_staking_api.data.parachainStaking.turing.repository.TuringAutomationTasksRepository
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.common.DelegatorStateUseCase
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.unbond.validations.preliminary.ParachainStakingUnbondPreliminaryValidationSystem
import io.novafoundation.nova.feature_staking_impl.presentation.ParachainStakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.stakeActions.parachain.turing.TuringStakeActionsComponentFactory

@Module
class TuringModule {

    @Provides
    @ScreenScope
    fun provideStakeActionsFactory(
        delegatorStateUseCase: DelegatorStateUseCase,
        resourceManager: ResourceManager,
        router: ParachainStakingRouter,
        validationExecutor: ValidationExecutor,
        unbondPreliminaryValidationSystem: ParachainStakingUnbondPreliminaryValidationSystem,
        turingAutomationTasksRepository: TuringAutomationTasksRepository
    ) = TuringStakeActionsComponentFactory(
        delegatorStateUseCase = delegatorStateUseCase,
        resourceManager = resourceManager,
        router = router,
        validationExecutor = validationExecutor,
        unbondPreliminaryValidationSystem = unbondPreliminaryValidationSystem,
        turingAutomationTasksRepository = turingAutomationTasksRepository
    )
}
