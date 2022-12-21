package io.novafoundation.nova.feature_staking_impl.presentation.validators.change.custom.review

import androidx.lifecycle.viewModelScope
import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.mixin.api.Validatable
import io.novafoundation.nova.common.presentation.DescriptiveButtonState
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.common.utils.inBackground
import io.novafoundation.nova.common.validation.ValidationExecutor
import io.novafoundation.nova.common.validation.progressConsumer
import io.novafoundation.nova.feature_staking_api.domain.model.relaychain.StakingState
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.domain.StakingInteractor
import io.novafoundation.nova.feature_staking_impl.domain.validations.controller.ChangeStackingValidationPayload
import io.novafoundation.nova.feature_staking_impl.presentation.StakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.common.SetupStakingProcess
import io.novafoundation.nova.feature_staking_impl.presentation.common.SetupStakingSharedState
import io.novafoundation.nova.feature_staking_impl.presentation.mappers.mapValidatorToValidatorDetailsParcelModel
import io.novafoundation.nova.feature_staking_impl.presentation.mappers.mapValidatorToValidatorModel
import io.novafoundation.nova.feature_staking_impl.presentation.validators.change.ValidatorModel
import io.novafoundation.nova.feature_staking_impl.presentation.validators.change.custom.review.model.ValidatorsSelectionState
import io.novafoundation.nova.feature_staking_impl.presentation.validators.change.mapAddEvmTokensValidationFailureToUI
import io.novafoundation.nova.feature_staking_impl.presentation.validators.change.setCustomValidators
import io.novafoundation.nova.feature_staking_impl.presentation.validators.details.StakeTargetDetailsPayload
import io.novafoundation.nova.feature_staking_impl.presentation.validators.details.relaychain
import io.novafoundation.nova.feature_wallet_api.domain.TokenUseCase
import io.novafoundation.nova.runtime.state.SingleAssetSharedState
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
    private val selectedAssetState: SingleAssetSharedState,
    private val validationExecutor: ValidationExecutor,
    tokenUseCase: TokenUseCase
) : BaseViewModel(), Validatable by validationExecutor {

    private val confirmSetupState = sharedStateSetup.setupStakingProcess
        .filterIsInstance<SetupStakingProcess.ReadyToSubmit>()
        .share()

    private val stashFlow = interactor.selectedAccountStakingStateFlow()
        .filterIsInstance<StakingState.Stash>()
        .inBackground()
        .share()

    private val selectedValidators = confirmSetupState
        .map { it.payload.validators }
        .share()

    private val currentTokenFlow = tokenUseCase.currentTokenFlow()
        .share()

    private val maxValidatorsPerNominatorFlow = flowOf {
        interactor.maxValidatorsPerNominator()
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

    private val validationProgress = MutableStateFlow(false)

    val continueButtonState = this.validationProgress.map {
        when {
            it -> DescriptiveButtonState.Loading
            else -> DescriptiveButtonState.Enabled(resourceManager.getString(R.string.common_continue))
        }
    }.share()

    val isInEditMode = MutableStateFlow(false)

    fun deleteClicked(validatorModel: ValidatorModel) {
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

    fun validatorInfoClicked(validatorModel: ValidatorModel) = launch {
        val stakeTarget = mapValidatorToValidatorDetailsParcelModel(validatorModel.stakeTarget)

        router.openValidatorDetails(StakeTargetDetailsPayload.relaychain(stakeTarget, interactor))
    }

    fun nextClicked() {
        viewModelScope.launch {
            val accountSettings = stashFlow.first()
            val payload = ChangeStackingValidationPayload(accountSettings.controllerAddress)

            validationExecutor.requireValid(
                validationSystem = interactor.getValidationSystem(),
                payload = payload,
                progressConsumer = validationProgress.progressConsumer(),
                validationFailureTransformer = { mapAddEvmTokensValidationFailureToUI(resourceManager, it) }
            ) {
                router.openConfirmStaking()
            }
        }
    }
}
