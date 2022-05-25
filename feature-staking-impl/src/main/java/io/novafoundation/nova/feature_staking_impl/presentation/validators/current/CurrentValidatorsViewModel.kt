package io.novafoundation.nova.feature_staking_impl.presentation.validators.current

import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.list.toListWithHeaders
import io.novafoundation.nova.common.list.toValueList
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.common.utils.inBackground
import io.novafoundation.nova.common.utils.toHexAccountId
import io.novafoundation.nova.common.utils.withLoading
import io.novafoundation.nova.feature_account_api.presenatation.account.icon.createAccountAddressModel
import io.novafoundation.nova.feature_staking_api.domain.model.NominatedValidator
import io.novafoundation.nova.feature_staking_api.domain.model.relaychain.StakingState
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.domain.StakingInteractor
import io.novafoundation.nova.feature_staking_impl.domain.validators.current.CurrentValidatorsInteractor
import io.novafoundation.nova.feature_staking_impl.presentation.StakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.common.SetupStakingProcess
import io.novafoundation.nova.feature_staking_impl.presentation.common.SetupStakingSharedState
import io.novafoundation.nova.feature_staking_impl.presentation.common.currentStakeTargets.CurrentStakeTargetsViewModel
import io.novafoundation.nova.feature_staking_impl.presentation.common.currentStakeTargets.model.SelectedStakeTargetModel
import io.novafoundation.nova.feature_staking_impl.presentation.common.currentStakeTargets.model.SelectedStakeTargetStatusModel
import io.novafoundation.nova.feature_staking_impl.presentation.mappers.formatValidatorApy
import io.novafoundation.nova.feature_staking_impl.presentation.mappers.mapValidatorToValidatorDetailsWithStakeFlagParcelModel
import io.novafoundation.nova.feature_staking_impl.presentation.validators.details.StakeTargetDetailsPayload
import io.novafoundation.nova.feature_staking_impl.presentation.validators.details.relaychain
import io.novafoundation.nova.feature_wallet_api.domain.TokenUseCase
import io.novafoundation.nova.feature_wallet_api.domain.model.Token
import io.novafoundation.nova.feature_wallet_api.presentation.model.mapAmountToAmountModel
import io.novafoundation.nova.runtime.ext.addressOf
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.state.SingleAssetSharedState
import io.novafoundation.nova.runtime.state.chain
import jp.co.soramitsu.fearless_utils.extensions.fromHex
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
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
    private val selectedAssetState: SingleAssetSharedState,
    tokenUseCase: TokenUseCase,
) : CurrentStakeTargetsViewModel() {

    private val groupedCurrentValidatorsFlow = stakingInteractor.selectedAccountStakingStateFlow()
        .filterIsInstance<StakingState.Stash>()
        .flatMapLatest(currentValidatorsInteractor::nominatedValidatorsFlow)
        .inBackground()
        .share()

    private val flattenCurrentValidators = groupedCurrentValidatorsFlow
        .map { it.toValueList() }
        .inBackground()
        .share()

    private val tokenFlow = tokenUseCase.currentTokenFlow()
        .shareInBackground()

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
            val currentState = setupStakingSharedState.get<SetupStakingProcess.Initial>()

            val currentValidators = flattenCurrentValidators.first().map(NominatedValidator::validator)

            val newState = currentState.changeValidatorsFlow()
                .next(currentValidators, SetupStakingProcess.ReadyToSubmit.SelectionMethod.CUSTOM)

            setupStakingSharedState.set(newState)

            router.openStartChangeValidators()
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
                iconTintRes = R.color.green,
                textColorRes = R.color.white,
            ),
            description = resourceManager.getString(R.string.staking_your_allocated_description_v2_2_0)
        )

        is NominatedValidator.Status.Group.Inactive -> SelectedStakeTargetStatusModel(
            SelectedStakeTargetStatusModel.TitleConfig(
                text = resourceManager.getString(R.string.staking_your_not_elected_format, statusGroup.numberOfValidators),
                iconRes = R.drawable.ic_time_16,
                iconTintRes = R.color.white_64,
                textColorRes = R.color.white_64,
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
                iconTintRes = R.color.white_64,
                textColorRes = R.color.white_64,
            ),
            description = resourceManager.getString(R.string.staking_your_validators_changing_title)
        )
    }
}
