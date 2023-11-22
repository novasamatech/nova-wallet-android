package io.novafoundation.nova.feature_staking_impl.presentation.validators.change.custom.review

import androidx.lifecycle.viewModelScope
import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.common.utils.inBackground
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.data.StakingSharedState
import io.novafoundation.nova.feature_staking_impl.domain.StakingInteractor
import io.novafoundation.nova.feature_staking_impl.presentation.StakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.common.SetupStakingProcess
import io.novafoundation.nova.feature_staking_impl.presentation.common.SetupStakingSharedState
import io.novafoundation.nova.feature_staking_impl.presentation.mappers.mapValidatorToValidatorDetailsParcelModel
import io.novafoundation.nova.feature_staking_impl.presentation.mappers.mapValidatorToValidatorModel
import io.novafoundation.nova.feature_staking_impl.presentation.validators.change.ValidatorStakeTargetModel
import io.novafoundation.nova.feature_staking_impl.presentation.validators.change.custom.review.flowAction.ReviewValidatorsFlowAction
import io.novafoundation.nova.feature_staking_impl.presentation.validators.change.custom.review.model.ValidatorsSelectionState
import io.novafoundation.nova.feature_staking_impl.presentation.validators.change.setCustomValidators
import io.novafoundation.nova.feature_staking_impl.presentation.validators.details.StakeTargetDetailsPayload
import io.novafoundation.nova.feature_staking_impl.presentation.validators.details.relaychain
import io.novafoundation.nova.feature_wallet_api.domain.TokenUseCase
import io.novafoundation.nova.runtime.state.chain
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class ReviewCustomValidatorsViewModel(
    private val router: StakingRouter,
    private val addressIconGenerator: AddressIconGenerator,
    private val interactor: StakingInteractor,
    private val resourceManager: ResourceManager,
    private val sharedStateSetup: SetupStakingSharedState,
    private val selectedAssetState: StakingSharedState,
    private val reviewValidatorsFlowAction: ReviewValidatorsFlowAction,
    tokenUseCase: TokenUseCase
) : BaseViewModel() {

    private val confirmSetupState = sharedStateSetup.setupStakingProcess
        .filterIsInstance<SetupStakingProcess.ReadyToSubmit>()
        .share()

    private val selectedValidators = confirmSetupState
        .map { it.validators }
        .share()

    private val currentTokenFlow = tokenUseCase.currentTokenFlow()
        .share()

    private val maxValidatorsPerNominatorFlow = flowOf {
        val activeStake = confirmSetupState.first().activeStake
        interactor.maxValidatorsPerNominator(activeStake)
    }.share()

    val selectionStateFlow = combine(
        selectedValidators,
        maxValidatorsPerNominatorFlow
    ) { validators, maxValidatorsPerNominator ->
        val isOverflow = validators.size > maxValidatorsPerNominator

        ValidatorsSelectionState(
            selectedHeaderText = resourceManager.getString(R.string.staking_custom_header_validators_title, validators.size, maxValidatorsPerNominator),
            isOverflow = isOverflow,
            nextButtonText = if (isOverflow) {
                resourceManager.getString(R.string.staking_custom_proceed_button_disabled_title, maxValidatorsPerNominator)
            } else {
                resourceManager.getString(R.string.common_continue)
            }
        )
    }

    val selectedValidatorModels = combine(
        selectedValidators,
        currentTokenFlow
    ) { validators, token ->
        validators.map { validator ->
            val chain = selectedAssetState.chain()

            mapValidatorToValidatorModel(chain, validator, addressIconGenerator, token)
        }
    }
        .inBackground()
        .share()

    val isInEditMode = MutableStateFlow(false)

    fun deleteClicked(validatorModel: ValidatorStakeTargetModel) {
        launch {
            val validators = selectedValidators.first()

            val withoutRemoved = validators - validatorModel.stakeTarget

            sharedStateSetup.setCustomValidators(withoutRemoved)

            if (withoutRemoved.isEmpty()) {
                router.back()
            }
        }
    }

    fun backClicked() {
        router.back()
    }

    fun validatorInfoClicked(validatorModel: ValidatorStakeTargetModel) = launch {
        val stakeTarget = mapValidatorToValidatorDetailsParcelModel(validatorModel.stakeTarget)

        router.openValidatorDetails(StakeTargetDetailsPayload.relaychain(stakeTarget, interactor))
    }

    fun nextClicked() {
        launch {
            val stakingOption = selectedAssetState.selectedOption.first()
            reviewValidatorsFlowAction.execute(viewModelScope, stakingOption)
        }
    }
}
