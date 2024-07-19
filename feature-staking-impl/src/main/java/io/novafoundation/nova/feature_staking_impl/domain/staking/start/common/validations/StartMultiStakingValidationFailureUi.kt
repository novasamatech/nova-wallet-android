package io.novafoundation.nova.feature_staking_impl.domain.staking.start.common.validations

import io.novafoundation.nova.common.base.TitleAndMessage
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.validation.TransformedFailure
import io.novafoundation.nova.common.validation.ValidationFlowActions
import io.novafoundation.nova.common.validation.ValidationStatus
import io.novafoundation.nova.common.validation.asDefault
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.common.validations.handlePoolAvailableBalanceError
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.common.selection.copyWith
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.common.validations.StartMultiStakingValidationFailure.AmountLessThanMinimum
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.common.validations.StartMultiStakingValidationFailure.AvailableBalanceGap
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.common.validations.StartMultiStakingValidationFailure.HasConflictingStakingType
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.common.validations.StartMultiStakingValidationFailure.InactivePool
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.common.validations.StartMultiStakingValidationFailure.MaxNominatorsReached
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.common.validations.StartMultiStakingValidationFailure.NonPositiveAmount
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.common.validations.StartMultiStakingValidationFailure.NotEnoughAvailableToStake
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.common.validations.StartMultiStakingValidationFailure.NotEnoughToPayFees
import io.novafoundation.nova.feature_staking_impl.domain.validations.setup.handleStakingMinimumBondError
import io.novafoundation.nova.feature_staking_impl.presentation.staking.start.common.formatStakingTypeLabel
import io.novafoundation.nova.feature_wallet_api.domain.validation.amountIsTooBig
import io.novafoundation.nova.feature_wallet_api.domain.validation.handleNotEnoughFeeError
import io.novafoundation.nova.feature_wallet_api.domain.validation.zeroAmount
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.formatPlanks
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.mapBalanceIdToUi
import java.math.BigDecimal

fun handleStartMultiStakingValidationFailure(
    status: ValidationStatus.NotValid<StartMultiStakingValidationFailure>,
    resourceManager: ResourceManager,
    flowActions: ValidationFlowActions<StartMultiStakingValidationPayload>,
    updateAmountInUi: (BigDecimal) -> Unit,
): TransformedFailure {
    return when (val reason = status.reason) {
        is AmountLessThanMinimum -> handleStakingMinimumBondError(resourceManager, reason).asDefault()

        is NotEnoughToPayFees -> handleNotEnoughFeeError(reason, resourceManager).asDefault()

        is MaxNominatorsReached -> {
            val stakingTypeLabel = resourceManager.formatStakingTypeLabel(reason.stakingType)

            TransformedFailure.Default(
                resourceManager.getString(R.string.start_staking_max_nominators_reached_title, stakingTypeLabel) to
                    resourceManager.getString(R.string.start_staking_max_nominators_reached_message)
            )
        }

        NotEnoughAvailableToStake -> resourceManager.amountIsTooBig().asDefault()

        NonPositiveAmount -> resourceManager.zeroAmount().asDefault()

        is AvailableBalanceGap -> {
            val lockDisplay = mapBalanceIdToUi(resourceManager, reason.biggestLockId)
            val currentMaxAvailable = reason.currentMaxAvailable.formatPlanks(reason.chainAsset)
            val alternativeMinStake = reason.alternativeMinStake.formatPlanks(reason.chainAsset)

            TransformedFailure.Default(
                resourceManager.getString(R.string.start_staking_cant_stake_amount) to
                    resourceManager.getString(
                        R.string.start_staking_available_balance_gap_message,
                        lockDisplay,
                        currentMaxAvailable,
                        alternativeMinStake,
                        lockDisplay
                    )
            )
        }

        InactivePool -> TransformedFailure.Default(
            resourceManager.getString(R.string.staking_parachain_wont_receive_rewards_title) to
                resourceManager.getString(R.string.start_staking_inactive_pool_message)
        )

        is StartMultiStakingValidationFailure.PoolAvailableBalance -> handlePoolAvailableBalanceError(
            error = reason,
            resourceManager = resourceManager,
            flowActions = flowActions,
            modifyPayload = { oldPayload, _ ->
                val newSelection = oldPayload.recommendableSelection.copyWith(reason.context.maximumToStake)

                oldPayload.copy(recommendableSelection = newSelection)
            },
            updateAmountInUi = updateAmountInUi
        )

        is HasConflictingStakingType -> handleStartStakingConflictingTypesFailure(resourceManager).asDefault()
    }
}

private fun handleStartStakingConflictingTypesFailure(
    resourceManager: ResourceManager
): TitleAndMessage {
    return resourceManager.getString(R.string.setup_staking_conflict_title) to
        resourceManager.getString(R.string.setup_staking_conflict_message)
}
