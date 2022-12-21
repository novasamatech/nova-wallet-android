package io.novafoundation.nova.feature_staking_impl.presentation.validators.change.recommended

import androidx.lifecycle.viewModelScope
import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.mixin.api.Validatable
import io.novafoundation.nova.common.presentation.DescriptiveButtonState
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.inBackground
import io.novafoundation.nova.common.utils.invoke
import io.novafoundation.nova.common.utils.lazyAsync
import io.novafoundation.nova.common.validation.ValidationExecutor
import io.novafoundation.nova.common.validation.progressConsumer
import io.novafoundation.nova.feature_staking_api.domain.model.Validator
import io.novafoundation.nova.feature_staking_api.domain.model.relaychain.StakingState
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.domain.StakingInteractor
import io.novafoundation.nova.feature_staking_impl.domain.recommendations.ValidatorRecommendatorFactory
import io.novafoundation.nova.feature_staking_impl.domain.recommendations.settings.RecommendationSettingsProviderFactory
import io.novafoundation.nova.feature_staking_impl.domain.validations.controller.ChangeStackingValidationPayload
import io.novafoundation.nova.feature_staking_impl.presentation.StakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.common.SetupStakingProcess.ReadyToSubmit
import io.novafoundation.nova.feature_staking_impl.presentation.common.SetupStakingProcess.ReadyToSubmit.SelectionMethod
import io.novafoundation.nova.feature_staking_impl.presentation.common.SetupStakingSharedState
import io.novafoundation.nova.feature_staking_impl.presentation.mappers.mapValidatorToValidatorDetailsParcelModel
import io.novafoundation.nova.feature_staking_impl.presentation.mappers.mapValidatorToValidatorModel
import io.novafoundation.nova.feature_staking_impl.presentation.validators.change.ValidatorModel
import io.novafoundation.nova.feature_staking_impl.presentation.validators.change.mapAddEvmTokensValidationFailureToUI
import io.novafoundation.nova.feature_staking_impl.presentation.validators.change.setRecommendedValidators
import io.novafoundation.nova.feature_staking_impl.presentation.validators.details.StakeTargetDetailsPayload
import io.novafoundation.nova.feature_staking_impl.presentation.validators.details.relaychain
import io.novafoundation.nova.feature_wallet_api.domain.TokenUseCase
import io.novafoundation.nova.feature_wallet_api.domain.model.Token
import io.novafoundation.nova.runtime.state.SingleAssetSharedState
import io.novafoundation.nova.runtime.state.chain
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class RecommendedValidatorsViewModel(
    private val router: StakingRouter,
    private val validatorRecommendatorFactory: ValidatorRecommendatorFactory,
    private val recommendationSettingsProviderFactory: RecommendationSettingsProviderFactory,
    private val addressIconGenerator: AddressIconGenerator,
    private val interactor: StakingInteractor,
    private val resourceManager: ResourceManager,
    private val sharedStateSetup: SetupStakingSharedState,
    private val tokenUseCase: TokenUseCase,
    private val selectedAssetState: SingleAssetSharedState,
    private val validationExecutor: ValidationExecutor
) : BaseViewModel(), Validatable by validationExecutor {

    private val stashFlow = interactor.selectedAccountStakingStateFlow()
        .filterIsInstance<StakingState.Stash>()
        .inBackground()
        .share()

    private val recommendedSettings by lazyAsync {
        recommendationSettingsProviderFactory.create(scope = viewModelScope).defaultSettings()
    }

    private val recommendedValidators = flow {
        val validatorRecommendator = validatorRecommendatorFactory.create(scope = viewModelScope)
        val validators = validatorRecommendator.recommendations(recommendedSettings())

        emit(validators)
    }.inBackground().share()

    private val validationProgress = MutableStateFlow(false)

    val continueButtonState = this.validationProgress.map {
        when {
            it -> DescriptiveButtonState.Loading
            else -> DescriptiveButtonState.Enabled(resourceManager.getString(R.string.common_continue))
        }
    }.share()

    val recommendedValidatorModels = recommendedValidators.map {
        convertToModels(it, tokenUseCase.currentToken())
    }.inBackground().share()

    val selectedTitle = recommendedValidators.map {
        val maxValidators = interactor.maxValidatorsPerNominator()

        resourceManager.getString(R.string.staking_custom_header_validators_title, it.size, maxValidators)
    }.inBackground().share()

    fun backClicked() {
        retractRecommended()

        router.back()
    }

    fun validatorInfoClicked(validatorModel: ValidatorModel) = launch {
        val stakeTarget = mapValidatorToValidatorDetailsParcelModel(validatorModel.stakeTarget)
        val payload = StakeTargetDetailsPayload.relaychain(stakeTarget, interactor)

        router.openValidatorDetails(payload)
    }

    fun nextClicked() {
        viewModelScope.launch {
            val validators = recommendedValidators.first()
            val accountSettings = stashFlow.first()
            val payload = ChangeStackingValidationPayload(accountSettings.controllerAddress)

            validationExecutor.requireValid(
                validationSystem = interactor.getValidationSystem(),
                payload = payload,
                progressConsumer = this@RecommendedValidatorsViewModel.validationProgress.progressConsumer(),
                validationFailureTransformer = { mapAddEvmTokensValidationFailureToUI(resourceManager, it) }
            ) {
                sharedStateSetup.setRecommendedValidators(validators)
                router.openConfirmStaking()
            }
        }
    }

    private suspend fun convertToModels(
        validators: List<Validator>,
        token: Token
    ): List<ValidatorModel> {
        val chain = selectedAssetState.chain()

        return validators.map {
            mapValidatorToValidatorModel(chain, it, addressIconGenerator, token)
        }
    }

    private fun retractRecommended() = sharedStateSetup.mutate {
        if (it is ReadyToSubmit && it.payload.selectionMethod == SelectionMethod.RECOMMENDED) {
            it.previous()
        } else {
            it
        }
    }
}
