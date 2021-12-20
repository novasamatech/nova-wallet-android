package io.novafoundation.nova.feature_staking_impl.presentation.staking.main.di

import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.validation.ValidationExecutor
import io.novafoundation.nova.feature_staking_api.domain.model.StakingState
import io.novafoundation.nova.feature_staking_impl.domain.StakingInteractor
import io.novafoundation.nova.feature_staking_impl.domain.main.ManageStakeAction
import io.novafoundation.nova.feature_staking_impl.domain.rewards.RewardCalculatorFactory
import io.novafoundation.nova.feature_staking_impl.domain.validations.main.StakeActionsValidationSystem
import io.novafoundation.nova.feature_staking_impl.domain.validations.welcome.WelcomeStakingValidationSystem
import io.novafoundation.nova.feature_staking_impl.presentation.StakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.common.SetupStakingSharedState
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.NominatorViewState
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.StashNoneViewState
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.ValidatorViewState
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.WelcomeViewState
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow

class StakingViewStateFactory(
    private val stakingInteractor: StakingInteractor,
    private val setupStakingSharedState: SetupStakingSharedState,
    private val resourceManager: ResourceManager,
    private val router: StakingRouter,
    private val rewardCalculatorFactory: RewardCalculatorFactory,
    private val welcomeStakingValidationSystem: WelcomeStakingValidationSystem,
    private val stakeActionsValidations: Map<ManageStakeAction, StakeActionsValidationSystem>,
    private val validationExecutor: ValidationExecutor
) {

    fun createValidatorViewState(
        stakingState: StakingState.Stash.Validator,
        currentAssetFlow: Flow<Asset>,
        scope: CoroutineScope,
        errorDisplayer: (Throwable) -> Unit
    ) = ValidatorViewState(
        validatorState = stakingState,
        stakingInteractor = stakingInteractor,
        currentAssetFlow = currentAssetFlow,
        scope = scope,
        router = router,
        errorDisplayer = errorDisplayer,
        resourceManager = resourceManager,
        stakeActionsValidations = stakeActionsValidations,
        validationExecutor = validationExecutor
    )

    fun createStashNoneState(
        currentAssetFlow: Flow<Asset>,
        accountStakingState: StakingState.Stash.None,
        scope: CoroutineScope,
        errorDisplayer: (Throwable) -> Unit
    ) = StashNoneViewState(
        stashState = accountStakingState,
        currentAssetFlow = currentAssetFlow,
        stakingInteractor = stakingInteractor,
        resourceManager = resourceManager,
        scope = scope,
        router = router,
        errorDisplayer = errorDisplayer,
        stakeActionsValidations = stakeActionsValidations,
        validationExecutor = validationExecutor
    )

    fun createWelcomeViewState(
        scope: CoroutineScope,
        errorDisplayer: (String) -> Unit,
        currentAssetFlow: Flow<Asset>,
    ) = WelcomeViewState(
        setupStakingSharedState = setupStakingSharedState,
        rewardCalculatorFactory = rewardCalculatorFactory,
        resourceManager = resourceManager,
        router = router,
        scope = scope,
        errorDisplayer = errorDisplayer,
        validationSystem = welcomeStakingValidationSystem,
        validationExecutor = validationExecutor,
        currentAssetFlow = currentAssetFlow,
    )

    fun createNominatorViewState(
        stakingState: StakingState.Stash.Nominator,
        currentAssetFlow: Flow<Asset>,
        scope: CoroutineScope,
        errorDisplayer: (Throwable) -> Unit
    ) = NominatorViewState(
        nominatorState = stakingState,
        stakingInteractor = stakingInteractor,
        currentAssetFlow = currentAssetFlow,
        scope = scope,
        router = router,
        errorDisplayer = errorDisplayer,
        resourceManager = resourceManager,
        stakeActionsValidations = stakeActionsValidations,
        validationExecutor = validationExecutor
    )
}
