package io.novafoundation.nova.feature_crowdloan_impl.presentation.contribute

import io.novafoundation.nova.common.mixin.api.Action
import io.novafoundation.nova.common.mixin.api.CustomDialogDisplayer
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.validation.TransformedFailure
import io.novafoundation.nova.common.validation.ValidationFlowActions
import io.novafoundation.nova.feature_crowdloan_impl.R
import io.novafoundation.nova.feature_crowdloan_impl.domain.contribute.validations.ContributeValidationFailure
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.formatTokenAmount

fun contributeValidationFailure(
    reason: ContributeValidationFailure,
    validationFlowActions: ValidationFlowActions,
    resourceManager: ResourceManager,
    onOpenCustomContribute: Action?,
): TransformedFailure {
    return when (reason) {
        ContributeValidationFailure.CannotPayFees -> {
            TransformedFailure.Default(
                resourceManager.getString(R.string.common_not_enough_funds_title) to
                    resourceManager.getString(R.string.common_not_enough_funds_message)
            )
        }

        ContributeValidationFailure.ExistentialDepositCrossed -> {
            TransformedFailure.Default(
                resourceManager.getString(R.string.common_existential_warning_title) to
                    resourceManager.getString(R.string.common_existential_warning_message_v2_2_0)
            )
        }

        ContributeValidationFailure.CrowdloanEnded -> {
            TransformedFailure.Default(
                resourceManager.getString(R.string.crowdloan_ended_title) to
                    resourceManager.getString(R.string.crowdloan_ended_message)
            )
        }

        ContributeValidationFailure.CapExceeded.FromRaised -> {
            TransformedFailure.Default(
                resourceManager.getString(R.string.crowdloan_cap_reached_title) to
                    resourceManager.getString(R.string.crowdloan_cap_reached_raised_message)
            )
        }

        is ContributeValidationFailure.CapExceeded.FromAmount -> {
            val formattedAmount = with(reason) {
                maxAllowedContribution.formatTokenAmount(chainAsset)
            }

            TransformedFailure.Default(
                resourceManager.getString(R.string.crowdloan_cap_reached_title) to
                    resourceManager.getString(R.string.crowdloan_cap_reached_amount_message, formattedAmount)
            )
        }

        is ContributeValidationFailure.LessThanMinContribution -> {
            val formattedAmount = with(reason) {
                minContribution.formatTokenAmount(chainAsset)
            }

            TransformedFailure.Default(
                resourceManager.getString(R.string.crowdloan_too_small_contribution_title) to
                    resourceManager.getString(R.string.crowdloan_too_small_contribution_message, formattedAmount)
            )
        }

        ContributeValidationFailure.PrivateCrowdloanNotSupported -> {
            TransformedFailure.Default(
                resourceManager.getString(R.string.crodloan_private_crowdloan_title) to
                    resourceManager.getString(R.string.crodloan_private_crowdloan_message)
            )
        }
        ContributeValidationFailure.BonusNotApplied -> {
            TransformedFailure.Custom(
                CustomDialogDisplayer.Payload(
                    title = resourceManager.getString(R.string.crowdloan_missing_bonus_title),
                    message = resourceManager.getString(R.string.crowdloan_missing_bonus_message),
                    okAction = CustomDialogDisplayer.Payload.DialogAction(
                        title = resourceManager.getString(R.string.crowdloan_missing_bonus_action),
                        action = { onOpenCustomContribute?.invoke() }
                    ),
                    cancelAction = CustomDialogDisplayer.Payload.DialogAction(
                        title = resourceManager.getString(R.string.common_skip),
                        action = { validationFlowActions.resumeFlow() }
                    ),
                    customStyle = R.style.AccentNegativeAlertDialogTheme
                )
            )
        }
    }
}
