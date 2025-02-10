package io.novafoundation.nova.feature_staking_impl.presentation.mythos.common.validations

import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.common.mixin.api.CustomDialogDisplayer
import io.novafoundation.nova.common.mixin.api.CustomDialogDisplayer.Payload.DialogAction
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.validation.TransformedFailure
import io.novafoundation.nova.common.validation.ValidationStatus
import io.novafoundation.nova.common.validation.asDefault
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.domain.mythos.start.validations.StartMythosStakingValidationFailure
import io.novafoundation.nova.feature_staking_impl.domain.mythos.unbond.validations.UnbondMythosStakingValidationFailure
import io.novafoundation.nova.feature_staking_impl.presentation.MythosStakingRouter
import io.novafoundation.nova.feature_wallet_api.domain.validation.amountIsTooBig
import io.novafoundation.nova.feature_wallet_api.domain.validation.handleNotEnoughFeeError
import io.novafoundation.nova.feature_wallet_api.domain.validation.zeroAmount
import io.novafoundation.nova.feature_wallet_api.presentation.model.mapAmountToAmountModel
import javax.inject.Inject

interface MythosStakingValidationFailureFormatter {

    fun formatStartStaking(failure: ValidationStatus.NotValid<StartMythosStakingValidationFailure>): TransformedFailure

    fun formatUnbond(failure: ValidationStatus.NotValid<UnbondMythosStakingValidationFailure>): TransformedFailure
}

@FeatureScope
class RealMythosStakingValidationFailureFormatter @Inject constructor(
    private val resourceManager: ResourceManager,
    private val router: MythosStakingRouter
) : MythosStakingValidationFailureFormatter {

    override fun formatStartStaking(failure: ValidationStatus.NotValid<StartMythosStakingValidationFailure>): TransformedFailure {
        return when (val reason = failure.reason) {
            is StartMythosStakingValidationFailure.NotEnoughBalanceToPayFees -> handleNotEnoughFeeError(reason, resourceManager).asDefault()

            StartMythosStakingValidationFailure.NotEnoughStakeableBalance -> resourceManager.amountIsTooBig().asDefault()

            StartMythosStakingValidationFailure.NotPositiveAmount -> resourceManager.zeroAmount().asDefault()

            StartMythosStakingValidationFailure.HasNotClaimedRewards -> hasPendingRewardFailure()

            is StartMythosStakingValidationFailure.TooLowStakeAmount -> {
                val formattedMinStake = mapAmountToAmountModel(reason.minimumStake, reason.asset).token

                val content = resourceManager.getString(R.string.common_amount_low) to
                    resourceManager.getString(R.string.staking_setup_amount_too_low, formattedMinStake)

                content.asDefault()
            }
        }
    }

    override fun formatUnbond(failure: ValidationStatus.NotValid<UnbondMythosStakingValidationFailure>): TransformedFailure {
        return when (val reason = failure.reason) {
            UnbondMythosStakingValidationFailure.HasNotClaimedRewards -> hasPendingRewardFailure()

            is UnbondMythosStakingValidationFailure.NotEnoughBalanceToPayFees -> handleNotEnoughFeeError(reason, resourceManager).asDefault()
        }
    }

    private fun hasPendingRewardFailure() = TransformedFailure.Custom(
        dialogPayload = CustomDialogDisplayer.Payload(
            title = resourceManager.getString(R.string.staking_myth_start_validation_pending_rewards_title),
            message = resourceManager.getString(R.string.staking_myth_start_validation_pending_rewards_description),
            okAction = DialogAction(
                title = resourceManager.getString(R.string.common_claim),
                action = { router.openClaimRewards() }
            )
        )
    )
}
