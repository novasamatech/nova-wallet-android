package io.novafoundation.nova.feature_staking_impl.presentation.staking.unbond

import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.validation.TransformedFailure
import io.novafoundation.nova.common.validation.ValidationFlowActions
import io.novafoundation.nova.common.validation.ValidationStatus
import io.novafoundation.nova.common.validation.asDefault
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.domain.validations.unbond.UnbondValidationFailure
import io.novafoundation.nova.feature_staking_impl.domain.validations.unbond.UnbondValidationPayload
import io.novafoundation.nova.feature_wallet_api.domain.validation.handleWith
import io.novafoundation.nova.feature_wallet_api.domain.validation.zeroAmount
import io.novafoundation.nova.feature_wallet_api.presentation.validation.handleInsufficientBalanceCommission

fun unbondValidationFailure(
    status: ValidationStatus.NotValid<UnbondValidationFailure>,
    flowActions: ValidationFlowActions<UnbondValidationPayload>,
    resourceManager: ResourceManager
): TransformedFailure {
    return when (val reason = status.reason) {
        is UnbondValidationFailure.BondedWillCrossExistential -> reason.handleWith(resourceManager, flowActions) { old, newAmount ->
            old.copy(amount = newAmount)
        }

        UnbondValidationFailure.CannotPayFees -> TransformedFailure.Default(
            resourceManager.getString(R.string.common_not_enough_funds_title) to
                resourceManager.getString(R.string.common_not_enough_funds_message)
        )

        UnbondValidationFailure.NotEnoughBonded -> TransformedFailure.Default(
            resourceManager.getString(R.string.common_not_enough_funds_title) to
                resourceManager.getString(R.string.staking_unbond_too_big)
        )

        is UnbondValidationFailure.UnbondLimitReached -> TransformedFailure.Default(
            resourceManager.getString(R.string.staking_unbonding_limit_reached_title) to
                resourceManager.getString(R.string.staking_unbonding_limit_reached_message, reason.limit)
        )

        UnbondValidationFailure.ZeroUnbond -> resourceManager.zeroAmount().asDefault()

        is UnbondValidationFailure.NotEnoughBalanceToStayAboveED -> handleInsufficientBalanceCommission(
            reason,
            resourceManager
        ).asDefault()
    }
}
