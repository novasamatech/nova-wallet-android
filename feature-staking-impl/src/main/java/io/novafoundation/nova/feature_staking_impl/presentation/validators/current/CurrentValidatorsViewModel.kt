package io.novafoundation.nova.feature_staking_impl.presentation.validators.current

import androidx.lifecycle.viewModelScope
import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.list.toListWithHeaders
import io.novafoundation.nova.common.list.toValueList
import io.novafoundation.nova.common.mixin.api.Validatable
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.combineToPair
import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.common.utils.inBackground
import io.novafoundation.nova.common.utils.toHexAccountId
import io.novafoundation.nova.common.utils.withLoading
import io.novafoundation.nova.common.validation.ValidationExecutor
import io.novafoundation.nova.feature_account_api.presenatation.account.icon.createAccountAddressModel
import io.novafoundation.nova.feature_staking_api.domain.model.NominatedValidator
import io.novafoundation.nova.feature_staking_api.domain.model.relaychain.StakingState
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.domain.StakingInteractor
import io.novafoundation.nova.feature_staking_impl.domain.validations.controller.ChangeStackingValidationPayload
import io.novafoundation.nova.feature_staking_impl.domain.validators.current.CurrentValidatorsInteractor
import io.novafoundation.nova.feature_staking_impl.presentation.StakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.common.SetupStakingProcess
import io.novafoundation.nova.feature_staking_impl.presentation.common.SetupStakingProcess.ReadyToSubmit.SelectionMethod
import io.novafoundation.nova.feature_staking_impl.presentation.common.SetupStakingSharedState
import io.novafoundation.nova.feature_staking_impl.presentation.common.currentStakeTargets.CurrentStakeTargetsViewModel
import io.novafoundation.nova.feature_staking_impl.presentation.common.currentStakeTargets.model.SelectedStakeTargetModel
import io.novafoundation.nova.feature_staking_impl.presentation.common.currentStakeTargets.model.SelectedStakeTargetStatusModel
import io.novafoundation.nova.feature_staking_impl.presentation.mappers.formatValidatorApy
import io.novafoundation.nova.feature_staking_impl.presentation.mappers.mapValidatorToValidatorDetailsWithStakeFlagParcelModel
import io.novafoundation.nova.feature_staking_impl.presentation.validators.change.mapAddEvmTokensValidationFailureToUI
import io.novafoundation.nova.feature_staking_impl.presentation.validators.details.StakeTargetDetailsPayload
import io.novafoundation.nova.feature_staking_impl.presentation.validators.details.relaychain
import io.novafoundation.nova.feature_wallet_api.domain.AssetUseCase
import io.novafoundation.nova.feature_wallet_api.domain.model.Token
import io.novafoundation.nova.feature_wallet_api.presentation.model.mapAmountToAmountModel
import io.novafoundation.nova.runtime.ext.addressOf
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.state.AnySelectedAssetOptionSharedState
import io.novafoundation.nova.runtime.state.chain
import jp.co.soramitsu.fearless_utils.extensions.fromHex
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CurrentValidatorsViewModel(
    private val router: StakingRouter,
    private val resourceManager: ResourceManager,
    private val stakingInteractor: StakingInteractor,
    private val iconGenerator: AddressIconGenerator,
    private val currentValidatorsInteractor: CurrentValidatorsInteractor,
    private val setupStakingSharedState: SetupStakingSharedState,
    private val selectedAssetState: AnySelectedAssetOptionSharedState,
    private val validationExecutor: ValidationExecutor,
    private val assetUseCase: AssetUseCase,
) : CurrentStakeTargetsViewModel(), Validatable by validationExecutor {

    private val stashFlow = stakingInteractor.selectedAccountStakingStateFlow(viewModelScope)
        .filterIsInstance<StakingState.Stash>()
        .inBackground()
        .share()

    private val assetFlow = assetUseCase.currentAssetFlow()
        .shareInBackground()

    private val activeStakeFlow = assetFlow
        .map { it.bondedInPlanks }
        .distinctUntilChanged()

    private val groupedCurrentValidatorsFlow = combineToPair(stashFlow, activeStakeFlow)
        .flatMapLatest { (stash, activeStake) -> currentValidatorsInteractor.nominatedValidatorsFlow(stash, activeStake, viewModelScope) }
        .inBackground()
        .share()

    private val flattenCurrentValidators = groupedCurrentValidatorsFlow
        .map { it.toValueList() }
        .inBackground()
        .share()

    private val tokenFlow = assetFlow
        .map { it.token }

    override val currentStakeTargetsFlow = groupedCurrentValidatorsFlow.combine(tokenFlow) { gropedList, token ->
        val chain = selectedAssetState.chain()

        gropedList.mapKeys { (statusGroup, _) -> mapNominatedValidatorStatusToUiModel(statusGroup) }
            .mapValues { (_, nominatedValidators) -> nominatedValidators.map { mapNominatedValidatorToUiModel(chain, it, token) } }
            .toListWithHeaders()
    }
        .withLoading()
        .shareInBackground()

    override val warningFlow = groupedCurrentValidatorsFlow.map { groupedList ->
        val (_, validators) = groupedList.entries.firstOrNull { (group, _) -> group is NominatedValidator.Status.Group.Active } ?: return@map null

        val shouldShowWarning = validators.any { (it.status as NominatedValidator.Status.Active).willUserBeRewarded.not() }

        if (shouldShowWarning) {
            resourceManager.getString(R.string.staking_your_oversubscribed_message)
        } else {
            null
        }
    }

    override val titleFlow: Flow<String> = flowOf {
        resourceManager.getString(R.string.staking_your_validators)
    }

    override fun backClicked() {
        router.back()
    }

    override fun changeClicked() {
        launch {
            val accountSettings = stashFlow.first()
            val payload = ChangeStackingValidationPayload(accountSettings.controllerAddress)

            validationExecutor.requireValid(
                validationSystem = currentValidatorsInteractor.getValidationSystem(),
                payload = payload,
                validationFailureTransformer = { mapAddEvmTokensValidationFailureToUI(resourceManager, it) }
            ) {
                openStartChangeValidators()
            }
        }
    }

    override fun stakeTargetInfoClicked(address: String) {
        launch {
            val payload = withContext(Dispatchers.Default) {
                val accountId = address.toHexAccountId()
                val allValidators = flattenCurrentValidators.first()

                val nominatedValidator = allValidators.first { it.validator.accountIdHex == accountId }

                val stakeTarget = mapValidatorToValidatorDetailsWithStakeFlagParcelModel(nominatedValidator)
                StakeTargetDetailsPayload.relaychain(stakeTarget, stakingInteractor)
            }

            router.openValidatorDetails(payload)
        }
    }

    private fun openStartChangeValidators() {
        launch {
            val currentState = setupStakingSharedState.get<SetupStakingProcess.Initial>()
            val currentValidators = flattenCurrentValidators.first().map(NominatedValidator::validator)
            val activeStake = activeStakeFlow.first()
            val newState = currentState.next(activeStake, currentValidators, SelectionMethod.CUSTOM)
            setupStakingSharedState.set(newState)
            router.openStartChangeValidators()
        }
    }

    private suspend fun mapNominatedValidatorToUiModel(
        chain: Chain,
        nominatedValidator: NominatedValidator,
        token: Token
    ): SelectedStakeTargetModel {
        val validator = nominatedValidator.validator

        val nominationAmount = (nominatedValidator.status as? NominatedValidator.Status.Active)?.let { activeStatus ->
            mapAmountToAmountModel(activeStatus.nomination, token)
        }

        val validatorAddress = chain.addressOf(validator.accountIdHex.fromHex())

        return SelectedStakeTargetModel(
            addressModel = iconGenerator.createAccountAddressModel(
                chain = chain,
                address = validatorAddress,
                name = validator.identity?.display
            ),
            nominated = nominationAmount,
            isOversubscribed = validator.electedInfo?.isOversubscribed ?: false,
            isSlashed = validator.slashed,
            apy = formatValidatorApy(validator)
        )
    }

    private fun mapNominatedValidatorStatusToUiModel(statusGroup: NominatedValidator.Status.Group) = when (statusGroup) {
        is NominatedValidator.Status.Group.Active -> SelectedStakeTargetStatusModel(
            SelectedStakeTargetStatusModel.TitleConfig(
                text = resourceManager.getString(R.string.staking_your_elected_format, statusGroup.numberOfValidators),
                iconRes = R.drawable.ic_checkmark_circle_16,
                iconTintRes = R.color.text_positive,
                textColorRes = R.color.text_primary,
            ),
            description = resourceManager.getString(R.string.staking_your_allocated_description_v2_2_0)
        )

        is NominatedValidator.Status.Group.Inactive -> SelectedStakeTargetStatusModel(
            SelectedStakeTargetStatusModel.TitleConfig(
                text = resourceManager.getString(R.string.staking_your_not_elected_format, statusGroup.numberOfValidators),
                iconRes = R.drawable.ic_time_16,
                iconTintRes = R.color.text_secondary,
                textColorRes = R.color.text_secondary,
            ),
            description = resourceManager.getString(R.string.staking_your_inactive_description_v2_2_0)
        )

        is NominatedValidator.Status.Group.Elected -> SelectedStakeTargetStatusModel(
            null,
            description = resourceManager.getString(R.string.staking_your_not_allocated_description_v2_2_0)
        )

        is NominatedValidator.Status.Group.WaitingForNextEra -> SelectedStakeTargetStatusModel(
            SelectedStakeTargetStatusModel.TitleConfig(
                text = resourceManager.getString(
                    R.string.staking_custom_header_validators_title,
                    statusGroup.numberOfValidators,
                    statusGroup.maxValidatorsPerNominator
                ),
                iconRes = R.drawable.ic_time_16,
                iconTintRes = R.color.text_secondary,
                textColorRes = R.color.text_secondary,
            ),
            description = resourceManager.getString(R.string.staking_your_validators_changing_title)
        )
    }
}
